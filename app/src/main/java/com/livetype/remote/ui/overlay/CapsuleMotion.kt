package com.livetype.remote.ui.overlay

/** What the single trailing button of the capsule has to do for the current field text. */
enum class CapsuleAction {
    /** Field is empty: the button closes the capsule, exactly as it always did. */
    CLOSE,

    /** Field holds text: the same button clears the field locally. */
    CLEAR,
}

/** Where the capsule window currently is in its open/close cycle. */
enum class CapsulePhase { HIDDEN, SHOWING, SHOWN, HIDING }

/** What the user (or the session) asked the capsule to do. */
enum class CapsuleRequest { SHOW, HIDE, TOGGLE }

/** The one transition that request is allowed to trigger, if any. */
enum class CapsuleCommand { NONE, OPEN, CLOSE }

/**
 * Pure motion and state maths of the writing capsule.
 *
 * Kept free of Android imports so the behaviour can be unit tested on the JVM:
 *  - [actionFor] decides the smart trailing button,
 *  - [springFraction] is the settle curve of the open/close transition,
 *  - [scaleFor] / [translationFor] turn that curve into view values.
 *
 * The curve is the step response of a critically damped spring
 * (`1 - (1 + ωt)·e^(-ωt)`), which is what Apple's `dampingFraction = 1` describes:
 * it leaves zero abruptly, accelerates, and settles without a single overshoot. No
 * cartoonish bounce, no easing-in stall at the start.
 */
object CapsuleMotion {

    /** Time after which the transition is visually finished, in ms. */
    const val OPEN_RESPONSE_MS = 260
    const val CLOSE_RESPONSE_MS = 200

    /** Shorter than this the motion reads as a jump, so very short reversals are skipped. */
    const val MIN_STEP_MS = 48

    /** Cross-fade of the close/clear icon swap inside the fixed 36dp slot. */
    const val ICON_SWAP_MS = 110

    /** The capsule grows out of the bubble: it starts slightly smaller than final. */
    const val FROM_SCALE = 0.82f

    /** Vertical offset (dp) the capsule travels while it grows. */
    const val TRAVEL_DP = 6

    /** `ωt` at which a critically damped spring is considered settled (≈ 0.7 % left). */
    private const val SETTLE_POINT = 7.0

    /**
     * The single-flight rule behind the close button.
     *
     * Every duplicate request is answered here instead of in the view code:
     *  - a hide that arrives while the capsule is already closing is ignored, so the exit
     *    motion can never be restarted or run twice,
     *  - a hide that arrives while it is still opening turns into one close (one animator,
     *    reversed from wherever it got to),
     *  - a show that arrives while it is already open changes nothing.
     */
    fun command(phase: CapsulePhase, request: CapsuleRequest): CapsuleCommand {
        val visible = phase == CapsulePhase.SHOWING || phase == CapsulePhase.SHOWN
        return when (request) {
            CapsuleRequest.SHOW -> if (visible) CapsuleCommand.NONE else CapsuleCommand.OPEN
            CapsuleRequest.HIDE -> if (visible) CapsuleCommand.CLOSE else CapsuleCommand.NONE
            CapsuleRequest.TOGGLE -> if (visible) CapsuleCommand.CLOSE else CapsuleCommand.OPEN
        }
    }

    fun actionFor(text: CharSequence?): CapsuleAction =
        if (text.isNullOrEmpty()) CapsuleAction.CLOSE else CapsuleAction.CLEAR

    fun responseMillis(opening: Boolean): Int =
        if (opening) OPEN_RESPONSE_MS else CLOSE_RESPONSE_MS

    /**
     * Progress (0 → 1) of the transition [elapsedMs] after it started, for a spring whose
     * response is [responseMs]. Monotonic, never above 1, exactly 0 at t = 0.
     */
    fun springFraction(responseMs: Int, elapsedMs: Int): Float {
        if (elapsedMs <= 0) return 0f
        if (responseMs <= 0) return 1f
        val omegaT = SETTLE_POINT * (elapsedMs.toDouble() / responseMs.toDouble())
        if (omegaT >= SETTLE_POINT * 2.5) return 1f
        val value = 1.0 - (1.0 + omegaT) * Math.exp(-omegaT)
        return value.coerceIn(0.0, 1.0).toFloat()
    }

    fun scaleFor(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return FROM_SCALE + (1f - FROM_SCALE) * p
    }

    fun alphaFor(progress: Float): Float = progress.coerceIn(0f, 1f)

    fun translationFor(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return TRAVEL_DP * (1f - p)
    }
}
