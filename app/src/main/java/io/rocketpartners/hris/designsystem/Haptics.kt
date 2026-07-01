package io.rocketpartners.hris.designsystem

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized, semantic haptic feedback, driven from a [View] so the system honors the user's touch
 * feedback setting. Compose call sites use `LocalView.current`. Mirrors iOS `Haptics`; Android lacks
 * distinct success/warning/error primitives before API 30, so those degrade to a context click.
 */
object Haptics {
    /** Light tick for discrete selection changes (filters, segment switches, pickers). */
    fun selection(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Physical tap for button presses / commits. */
    fun impact(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /** Positive outcome (submitted, approved, saved). */
    fun success(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /** Cautionary outcome (cancelled, rejected, destructive confirm). */
    fun warning(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Failure outcome (network/validation error). */
    fun error(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
