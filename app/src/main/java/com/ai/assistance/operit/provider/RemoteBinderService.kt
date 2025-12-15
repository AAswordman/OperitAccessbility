package com.ai.assistance.operit.provider

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class RemoteBinderService : Service() {

    private lateinit var proxyBinder: IAccessibilityProvider.Stub

    override fun onCreate() {
        super.onCreate()
        proxyBinder = object : IAccessibilityProvider.Stub() {
            override fun getUiHierarchy(): String {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "getUiHierarchy: Accessibility Service not connected.")
                    return ""
                }
                return UIAccessibilityService.binder?.getUiHierarchy() ?: ""
            }

            override fun performClick(x: Int, y: Int): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "performClick: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.performClick(x, y) ?: false
            }

            override fun performLongPress(x: Int, y: Int): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "performLongPress: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.performLongPress(x, y) ?: false
            }

            override fun performGlobalAction(actionId: Int): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "performGlobalAction: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.performGlobalAction(actionId) ?: false
            }

            override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "performSwipe: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.performSwipe(startX, startY, endX, endY, duration) ?: false
            }

            override fun findFocusedNodeId(): String? {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "findFocusedNodeId: Accessibility Service not connected.")
                    return null
                }
                return UIAccessibilityService.binder?.findFocusedNodeId()
            }

            override fun setTextOnNode(nodeId: String, text: String): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "setTextOnNode: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.setTextOnNode(nodeId, text) ?: false
            }

            override fun takeScreenshot(path: String, format: String): Boolean {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "takeScreenshot: Accessibility Service not connected.")
                    return false
                }
                return UIAccessibilityService.binder?.takeScreenshot(path, format) ?: false
            }

            override fun isAccessibilityServiceEnabled(): Boolean {
                return UIAccessibilityService.isServiceConnected
            }

            override fun getCurrentActivityName(): String {
                if (!UIAccessibilityService.isServiceConnected) {
                    Log.w("RemoteBinderService", "getCurrentActivityName: Accessibility Service not connected.")
                    return ""
                }
                return UIAccessibilityService.currentActivityName
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return proxyBinder
    }
} 
