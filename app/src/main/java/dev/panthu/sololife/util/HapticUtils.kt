package dev.panthu.sololife.util

import android.view.HapticFeedbackConstants
import android.view.View

fun View.hapticTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.hapticConfirm() {
    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}

fun View.hapticReject() {
    performHapticFeedback(HapticFeedbackConstants.REJECT)
}
