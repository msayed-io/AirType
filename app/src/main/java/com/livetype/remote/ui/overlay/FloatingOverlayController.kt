package com.livetype.remote.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.livetype.remote.R
import com.livetype.remote.core.pairing.PairingStore
import com.livetype.remote.core.session.AirTypeSession
import com.livetype.remote.core.session.SessionSnapshot
import com.livetype.remote.core.network.ConnState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** True when Android lets this app draw over other apps (checked at use time, every time). */
object OverlayPermission {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)
}

/**
 * The floating bubble and its writing capsule.
 *
 * Both are ordinary windows drawn by the foreground service (never by an activity), so they
 * survive while the user works in Chrome or anywhere else. Every keystroke goes through
 * [AirTypeSession.onTextChanged] — the same single stream the phone screen uses — so a
 * character is streamed exactly once no matter which field the user typed it in.
 */
class FloatingOverlayController(rawContext: Context) {

    /**
     * Overlay views are built outside any activity, so they must be given a theme
     * explicitly — otherwise text selection handles and the app font fall back to defaults.
     */
    private val context: Context =
        ContextThemeWrapper(rawContext.applicationContext, R.style.Theme_LiveTypeRemote)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val store = PairingStore(context.applicationContext)
    private val ime = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    private val bubble: FrameLayout by lazy { buildBubble() }
    private val capsule: LinearLayout by lazy { buildCapsule() }
    private val input: EditText by lazy { capsule.findViewById<EditText>(R.id.airtype_capsule_input) }
    private val statusDot: View by lazy { capsule.findViewById(R.id.airtype_capsule_dot) }

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var capsuleParams: WindowManager.LayoutParams? = null
    private var bubbleAttached = false
    private var capsuleAttached = false
    private var suppressWatcher = false
    private var lastSnapshot: SessionSnapshot = SessionSnapshot()

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun usableBounds(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            width = metrics.widthPixels
            height = metrics.heightPixels
        }
        return width to height
    }

    // ---- public surface --------------------------------------------------------

    fun isBubbleShown(): Boolean = bubbleAttached

    fun isCapsuleShown(): Boolean = capsuleAttached

    /** Attaches the bubble, restoring size, colour and the last valid position. */
    fun showBubble(snapshot: SessionSnapshot) {
        lastSnapshot = snapshot
        if (bubbleAttached) {
            applyAppearance()
            return
        }
        val (screenW, screenH) = usableBounds()
        val size = dp(store.bubbleSizeDp)
        val margin = dp(MARGIN_DP)
        val savedX = store.bubbleX
        val savedY = store.bubbleY
        val maxX = max(0, screenW - size)
        val maxY = max(0, screenH - size)
        val x = savedX.coerceIn(0, maxX).takeIf { savedX >= 0 } ?: (maxX - margin).coerceAtLeast(0)
        val y = if (savedY < 0) (screenH / 2).coerceIn(0, maxY) else savedY.coerceIn(0, maxY)

        val params = WindowManager.LayoutParams(
            size,
            size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            title = "AirType bubble"
        }
        bubbleParams = params
        applyAppearance()
        attachBubbleTouch(params)
        runCatching { windowManager.addView(bubble, params) }
            .onSuccess { bubbleAttached = true }
            .onFailure { bubbleAttached = false }
    }

    fun hideBubble() {
        hideCapsule()
        if (!bubbleAttached) return
        runCatching { windowManager.removeView(bubble) }
        bubbleAttached = false
        bubbleParams = null
    }

    /** Tap on the bubble: show the capsule (and the keyboard), or hide it again. */
    @SuppressLint("ClickableViewAccessibility")
    fun toggleCapsule() {
        if (capsuleAttached) hideCapsule() else showCapsule()
    }

    fun showCapsule() {
        if (!bubbleAttached) showBubble(lastSnapshot)
        val (screenW, screenH) = usableBounds()
        val margin = dp(MARGIN_DP)
        val width = max(dp(240), screenW - margin * 2)
        val bubbleP = bubbleParams ?: return
        val bubbleSize = bubbleP.width.coerceAtLeast(dp(44))

        if (!capsuleAttached) {
            val params = WindowManager.LayoutParams(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                // Focusable (no FLAG_NOT_FOCUSABLE) so the keyboard can type into it, and
                // NOT_TOUCH_MODAL so taps outside still reach the app underneath.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                title = "AirType capsule"
            }
            capsuleParams = params
            runCatching { windowManager.addView(capsule, params) }
                .onSuccess { capsuleAttached = true }
                .onFailure { capsuleAttached = false; return }

            // Keep the capsule above the keyboard zone: the keyboard takes roughly the
            // lower 45% of the screen, so the capsule never starts below that line.
            val capsuleHeight = dp(CAPSULE_MAX_HEIGHT_DP)
            val maxBottom = (screenH * CAPSULE_Y_LIMIT).toInt()
            var y = bubbleP.y + bubbleSize + dp(10)
            if (y + capsuleHeight > maxBottom) y = bubbleP.y - capsuleHeight - dp(10)
            y = y.coerceIn(margin, max(maxBottom - capsuleHeight, margin))
            params.x = margin
            params.y = y
            runCatching { windowManager.updateViewLayout(capsule, params) }
        }

        syncFromSession()
        bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(EXIT_MS).start()
        capsule.alpha = 0f
        capsule.animate().alpha(1f).setDuration(ENTER_MS).start()
        input.post {
            // Requesting focus before the window is laid out is silently dropped, which
            // would leave the capsule visible but untypeable.
            input.requestFocus()
            ime?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun hideCapsule() {
        if (!capsuleAttached) return
        ime?.hideSoftInputFromWindow(input.windowToken, 0)
        capsule.animate().alpha(0f).setDuration(EXIT_MS).withEndAction {
            if (capsuleAttached) {
                runCatching { windowManager.removeView(capsule) }
                capsuleAttached = false
                capsuleParams = null
            }
        }.start()
    }

    /** Mirrors session state into the capsule (dot colour, remote text) without echoing. */
    fun updateFromSnapshot(snapshot: SessionSnapshot) {
        lastSnapshot = snapshot
        if (!snapshot.hasLiveSession) {
            hideBubble()
            return
        }
        if (capsuleAttached) syncFromSession()
    }

    fun applyAppearance() {
        val size = dp(store.bubbleSizeDp)
        val color = store.bubbleColor
        (bubble.background as? GradientDrawable)?.let { shape ->
            shape.setColor(color)
        }
        bubbleParams?.let { params ->
            if (params.width != size || params.height != size) {
                params.width = size
                params.height = size
                if (bubbleAttached) {
                    val (screenW, screenH) = usableBounds()
                    params.x = params.x.coerceIn(0, max(0, screenW - size))
                    params.y = params.y.coerceIn(0, max(0, screenH - size))
                    runCatching { windowManager.updateViewLayout(bubble, params) }
                }
            }
        }
        bubble.findViewById<ImageView>(R.id.airtype_bubble_icon)?.let { icon ->
            val pad = (size * 0.26f).roundToInt()
            icon.setPadding(pad, pad, pad, pad)
        }
    }

    fun destroy() {
        hideCapsule()
        hideBubble()
    }

    // ---- view construction -----------------------------------------------------

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun buildBubble(): FrameLayout {
        val root = FrameLayout(context)
        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(store.bubbleColor)
        }
        root.background = background
        root.contentDescription = "AirType"
        val icon = ImageView(context)
        icon.id = R.id.airtype_bubble_icon
        icon.setImageResource(R.drawable.ic_floating_keyboard)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        root.addView(
            icon,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return root
    }

    private fun buildCapsule(): LinearLayout {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#E1E3E8"))
            }
            elevation = dp(6).toFloat()
        }

        val dot = View(context)
        dot.id = R.id.airtype_capsule_dot
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#10B981"))
        }
        root.addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)).apply {
            marginEnd = dp(10)
        })

        val field = EditText(context).apply {
            id = R.id.airtype_capsule_input
            hint = com.livetype.remote.ui.Strings.forLang(store.language)["floating_capsule_hint"]
            setTextColor(Color.parseColor("#181A1F"))
            setHintTextColor(Color.parseColor("#9DA3AF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = null
            isSingleLine = false
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
            runCatching {
                ResourcesCompat.getFont(context, R.font.readex_pro)?.let { typeface = it }
                typeface = Typeface.create(typeface, Typeface.NORMAL)
            }
            setPadding(dp(2), dp(6), dp(6), dp(6))
        }
        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressWatcher) return
                // Same single entry point as the phone screen: one diff, one message.
                AirTypeSession.onTextChanged(s?.toString().orEmpty())
            }
        })
        root.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val close = ImageView(context).apply {
            setImageResource(R.drawable.ic_floating_close)
            contentDescription = "close"
            setColorFilter(Color.parseColor("#7A808C"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { hideCapsule() }
        }
        root.addView(close, LinearLayout.LayoutParams(dp(36), dp(36)))
        return root
    }

    private fun syncFromSession() {
        val snapshot = AirTypeSession.snapshot.value
        if (!capsuleAttached) return
        val sessionText = snapshot.text
        if (input.text.toString() != sessionText) {
            suppressWatcher = true
            input.setText(sessionText)
            input.setSelection(sessionText.length.coerceAtMost(input.text.length))
            suppressWatcher = false
        }
        val dotColor = when (snapshot.conn) {
            ConnState.PAIRED -> Color.parseColor("#10B981")
            ConnState.CONNECTING, ConnState.RECONNECTING -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#EF4444")
        }
        (statusDot.background as? GradientDrawable)?.setColor(dotColor)
    }

    // ---- touch handling --------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun attachBubbleTouch(params: WindowManager.LayoutParams) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        bubble.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        val (screenW, screenH) = usableBounds()
                        params.x = (startX + dx.roundToInt()).coerceIn(0, max(0, screenW - params.width))
                        params.y = (startY + dy.roundToInt()).coerceIn(0, max(0, screenH - params.height))
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        // Persist the resting place so the bubble comes back where it was.
                        store.bubbleX = params.x
                        store.bubbleY = params.y
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        toggleCapsule()
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private companion object {
        const val MARGIN_DP = 14
        const val CAPSULE_MAX_HEIGHT_DP = 64
        const val ENTER_MS = 160L
        const val EXIT_MS = 120L

        /** Capsule stays above this fraction of the screen so the keyboard never hides it. */
        const val CAPSULE_Y_LIMIT = 0.55f
    }
}
