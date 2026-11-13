package com.oneuitweak.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Enabled so OneUI Tweak can use accessibility features later.
 * It currently ignores all events and does not read window content.
 */
class OneTweakAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
