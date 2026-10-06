package com.gravassist.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdbAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AdbAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        LoggerService.log("ADB Service", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if needed
    }

    override fun onInterrupt() {
        LoggerService.log("ADB Service", "Accessibility Service Interrupted", "WARNING")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        LoggerService.log("ADB Service", "Accessibility Service Destroyed")
    }

    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        dispatchGesture(builder.build(), null, null)
        LoggerService.log("ADB Touch", "Click at ($x, $y)")
    }

    fun inputText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focusedNode != null) {
            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            LoggerService.log("ADB Input", "Injected text: $text")
            true
        } else {
            LoggerService.log("ADB Input", "No focused input field found", "FAILED")
            false
        }
    }
}
