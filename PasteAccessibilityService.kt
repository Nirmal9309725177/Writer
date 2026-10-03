package com.example.jieshuovoiceassistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PasteAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_PASTE = "com.example.jieshuovoiceassistant.PASTE"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only act when explicitly requested by the user.
    }

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PASTE) {
            pasteIntoFocusedField()
        }
        return START_NOT_STICKY
    }

    private fun pasteIntoFocusedField() {
        val root = rootInActiveWindow ?: return
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        if (!node.isEditable) return
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
}
