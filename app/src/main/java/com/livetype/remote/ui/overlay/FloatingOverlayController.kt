package com.livetype.remote.ui.overlay

import android.animation.Animator
import android.animation.ValueAnimator
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
import android.view.animation.LinearInterpolator
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
    private val actionButton: FrameLayout by lazy { capsule.findViewById(R.id.airtype_capsule_action) }
    private val closeIcon: ImageView by lazy { capsule.findViewById(R.id.airtype_capsule_close) }
    private val clearIcon: ImageView by lazy { capsule.findViewById(R.id.airtype_capsule_clear) }
    private val strings by lazy { com.livetype.remote.ui.Strings.forLang(store.language) }

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var capsuleParams: WindowManager.LayoutParams? = null
    private var bubbleAttached = false
    private var capsuleAttached = false
    private var suppressWatcher = false
    private var lastSnapshot: SessionSnapshot = SessionSnapshot()
    private var capsuleAnimator: ValueAnimator? = null
    private var capsuleProgress = 0f
    /** The one place that decides whether an open or close may start right now. */
    private var capsulePhase = CapsulePhase.HIDDEN

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
        // Leaving floating mode removes both windows at once: no fade-out window may
        // outlive the bubble it belongs to.
        capsuleAnimator?.cancel()
        capsuleAnimator = null
        removeCapsuleWindow.run()
        if (!bubbleAttached) return
        runCatching { windowManager.removeView(bubble) }
        bubbleAttached = false
        bubbleParams = null
    }

    /**
     * Tap on the bubble: show the capsule (and the keyboard), or hide it again.
     *
     * The decision goes through [CapsuleMotion.command], so a tap during a close reverses
     * the single running motion instead of starting a second one.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun toggleCapsule() {
        when (CapsuleMotion.command(capsulePhase, CapsuleRequest.TOGGLE)) {
            CapsuleCommand.CLOSE -> hideCapsule()
            CapsuleCommand.OPEN -> showCapsule()
            CapsuleCommand.NONE -> Unit
        }
    }

    fun showCapsule() {
        if (CapsuleMotion.command(capsulePhase, CapsuleRequest.SHOW) == CapsuleCommand.NONE) {
            // Already open (or opening): never attach or animate a second time.
            focusCapsuleInput()
            return
        }
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
                .onSuccess {
                    capsuleAttached = true
                    // Start from nothing so the first frame cannot flash at full opacity.
                    applyCapsuleProgress(0f)
                }
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
        // The capsule grows out of the edge of the bubble it belongs to, so the pair reads
        // as one floating object rather than as a second, unrelated window.
        anchorCapsule(pivotBubble = bubbleP, capsuleWidthPx = width)
        capsulePhase = CapsulePhase.SHOWING
        animateCapsuleTo(1f) {
            if (capsulePhase == CapsulePhase.SHOWING) capsulePhase = CapsulePhase.SHOWN
        }
        focusCapsuleInput()
    }

    fun hideCapsule() {
        // A second hide while the exit motion runs must not restart it, and a hide that
        // arrives when nothing is shown must not touch the window at all.
        if (CapsuleMotion.command(capsulePhase, CapsuleRequest.HIDE) != CapsuleCommand.CLOSE) return
        capsulePhase = CapsulePhase.HIDING
        ime?.hideSoftInputFromWindow(input.windowToken, 0)
        // The window is removed only once the motion has actually finished, so it never
        // vanishes mid-flight and never leaves an invisible layer behind.
        animateCapsuleTo(0f) {
            if (capsulePhase == CapsulePhase.HIDING) removeCapsuleWindow.run()
        }
    }

    /** Requests focus once the window is laid out; silently dropped before that. */
    private fun focusCapsuleInput() {
        input.post {
            if (capsulePhase != CapsulePhase.SHOWING && capsulePhase != CapsulePhase.SHOWN) return@post
            input.requestFocus()
            ime?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /** Removes the capsule window. Idempotent: safe from the motion end and from destroy(). */
    private val removeCapsuleWindow = Runnable {
        capsuleAnimator?.cancel()
        capsuleAnimator = null
        capsuleProgress = 0f
        if (capsuleAttached) {
            runCatching { windowManager.removeView(capsule) }
            capsuleAttached = false
        }
        capsuleParams = null
        capsulePhase = CapsulePhase.HIDDEN
    }

    // ---- capsule motion and the smart trailing button ---------------------------

    /**
     * Puts the scale pivot on the corner of the capsule that faces the bubble, so the
     * capsule appears to unfold from the bubble instead of from nowhere.
     */
    private fun anchorCapsule(pivotBubble: WindowManager.LayoutParams, capsuleWidthPx: Int) {
        val capsuleX = capsuleParams?.x ?: dp(MARGIN_DP)
        val capsuleY = capsuleParams?.y ?: pivotBubble.y
        val bubbleCentreX = pivotBubble.x + pivotBubble.width / 2f
        val bubbleCentreY = pivotBubble.y + pivotBubble.height / 2f
        val capsuleCentreX = capsuleX + capsuleWidthPx / 2f
        val capsuleCentreY = capsuleY + dp(CAPSULE_MAX_HEIGHT_DP) / 2f
        capsule.pivotX = if (bubbleCentreX >= capsuleCentreX) capsuleWidthPx.toFloat() else 0f
        capsule.pivotY =
            if (bubbleCentreY >= capsuleCentreY) dp(CAPSULE_MAX_HEIGHT_DP).toFloat() else 0f
    }

    /**
     * Grows or shrinks the capsule along a critically damped spring curve.
     *
     * Reversals reuse the current progress, so tapping the bubble twice quickly produces
     * one continuous motion instead of two overlapping ones.
     */
    private fun animateCapsuleTo(target: Float, onFinished: (() -> Unit)? = null) {
        capsuleAnimator?.cancel()
        val from = capsuleProgress
        if (abs(target - from) < 0.001f && onFinished == null) {
            applyCapsuleProgress(target)
            return
        }
        if (!animationsEnabled()) {
            applyCapsuleProgress(target)
            onFinished?.invoke()
            return
        }
        val opening = target > from
        val response = CapsuleMotion.responseMillis(opening)
        val duration = (response * abs(target - from)).toInt()
            .coerceAtLeast(CapsuleMotion.MIN_STEP_MS)
        var cancelled = false
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val linear = anim.animatedValue as Float
                val elapsed = (linear * response).toInt()
                val eased = CapsuleMotion.springFraction(response, elapsed)
                applyCapsuleProgress(from + (target - from) * eased)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    capsuleAnimator = null
                    applyCapsuleProgress(target)
                    onFinished?.invoke()
                }

                override fun onAnimationRepeat(animation: Animator) = Unit
            })
        }
        capsuleAnimator = animator
        animator.start()
    }

    private fun applyCapsuleProgress(progress: Float) {
        capsuleProgress = progress.coerceIn(0f, 1f)
        capsule.alpha = CapsuleMotion.alphaFor(capsuleProgress)
        val scale = CapsuleMotion.scaleFor(capsuleProgress)
        capsule.scaleX = scale
        capsule.scaleY = scale
        capsule.translationY = dp(CapsuleMotion.TRAVEL_DP) * (1f - capsuleProgress)
    }

    /**
     * Honours the system animation scale: with animations turned off (accessibility
     * "remove animations" / developer setting) the transitions become instantaneous while
     * every interaction still works.
     */
    private fun animationsEnabled(): Boolean = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }.getOrDefault(true)

    /**
     * The single trailing button: it closes the capsule while the field is empty and
     * clears the field while there is text. Both glyphs live in the same fixed 36dp slot,
     * so swapping them never moves the button, the field or the capsule edge.
     */
    private fun updateCapsuleAction(animate: Boolean = true) {
        val clearing = CapsuleMotion.actionFor(input.text) == CapsuleAction.CLEAR
        val closeAlpha = if (clearing) 0f else 1f
        val clearAlpha = if (clearing) 1f else 0f
        actionButton.contentDescription = strings[
            if (clearing) "floating_capsule_clear" else "floating_capsule_close"
        ]
        if (!animate) {
            closeIcon.animate().cancel()
            clearIcon.animate().cancel()
            closeIcon.alpha = closeAlpha
            clearIcon.alpha = clearAlpha
            return
        }
        if (closeIcon.alpha == closeAlpha && clearIcon.alpha == clearAlpha) return
        val ms = CapsuleMotion.ICON_SWAP_MS.toLong()
        closeIcon.animate().alpha(closeAlpha).setDuration(ms).start()
        clearIcon.animate().alpha(clearAlpha).setDuration(ms).start()
    }

    private fun onCapsuleAction() {
        when (CapsuleMotion.actionFor(input.text)) {
            CapsuleAction.CLOSE -> hideCapsule()
            CapsuleAction.CLEAR -> clearCapsuleTextLocally()
        }
    }

    /**
     * Empties the capsule field locally and sends NOTHING to the desktop.
     *
     * [AirTypeSession.clearText] → `StreamSession.clearText()` returns no envelope, resets
     * its own baseline and marks the stream append-only, so the next characters are typed
     * *after* the text the desktop already holds. The field is emptied with the change
     * watcher suspended, which is what keeps the local clear from being turned into an
     * indirect delete of everything already typed on the PC.
     */
    private fun clearCapsuleTextLocally() {
        suppressWatcher = true
        try {
            AirTypeSession.clearText()
            input.setText("")
            input.setSelection(0)
        } finally {
            suppressWatcher = false
        }
        updateCapsuleAction()
        input.post {
            input.requestFocus()
            ime?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
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
        capsuleAnimator?.cancel()
        capsuleAnimator = null
        removeCapsuleWindow.run()
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
                updateCapsuleAction()
                if (suppressWatcher) return
                // Same single entry point as the phone screen: one diff, one message.
                AirTypeSession.onTextChanged(s?.toString().orEmpty())
            }
        })
        root.addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // One 36dp slot, two glyphs stacked and cross-faded. The slot size never changes,
        // so switching between close and clear cannot shift the field or the capsule.
        val closeView = ImageView(context).apply {
            id = R.id.airtype_capsule_close
            setImageResource(R.drawable.ic_floating_close)
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setColorFilter(Color.parseColor("#7A808C"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val clearView = ImageView(context).apply {
            id = R.id.airtype_capsule_clear
            setImageResource(R.drawable.ic_capsule_clear)
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(dp(8), dp(8), dp(8), dp(8))
            alpha = 0f
        }
        val action = FrameLayout(context).apply {
            id = R.id.airtype_capsule_action
            isClickable = true
            isFocusable = true
            background = null
            setOnClickListener { onCapsuleAction() }
        }
        val glyphParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
        action.addView(closeView, glyphParams)
        action.addView(clearView, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER))
        root.addView(action, LinearLayout.LayoutParams(dp(36), dp(36)))
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
        updateCapsuleAction(animate = false)
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
        const val EXIT_MS = 120L

        /** Capsule stays above this fraction of the screen so the keyboard never hides it. */
        const val CAPSULE_Y_LIMIT = 0.55f
    }
}
