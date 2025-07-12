package com.ai.assistance.operit.provider

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter
import android.util.Xml

class UIAccessibilityService : AccessibilityService() {

    private val accessibilityBinder = object : IAccessibilityProvider.Stub() {
        override fun getUiHierarchy(): String {
            return this@UIAccessibilityService.captureUiHierarchyAsXml()
        }

        override fun performClick(x: Int, y: Int): Boolean {
            Log.d(TAG, "准备在 ($x, $y) 执行点击...")

            // 1. 创建一个描述点击路径的Path对象
            // 明确地创建一个长度为0的线段，以确保路径的有效性
            val clickPath = android.graphics.Path().apply {
                moveTo(x.toFloat(), y.toFloat())
                lineTo(x.toFloat(), y.toFloat())
            }

            // 2. 用Path创建一个手势“笔划”
            // 点击的持续时间需要一个合理的值，不能太短，例如50毫秒
            val clickStroke = GestureDescription.StrokeDescription(clickPath, 0L, 50L)

            // 3. 用“笔划”构建完整的手势描述
            val gestureDescription = GestureDescription.Builder()
                .addStroke(clickStroke)
                .build()

            // 4. 分发手势，并直接返回dispatchGesture的结果
            // 这个布尔值表示系统是否成功接收了我们的手势请求
            return this@UIAccessibilityService.dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
                // 这个回调是异步的，主应用无法直接得到它的结果，但对你调试非常重要
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.i(TAG, "手势已成功完成。")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "手势被取消。")
                }
            }, null)
        }

        override fun performGlobalAction(actionId: Int): Boolean {
            return this@UIAccessibilityService.performGlobalAction(actionId)
        }

        override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
            val path = Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return dispatchGesture(gesture, null, null)
        }

        override fun findFocusedNodeId(): String? {
            val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (focusedNode != null) {
                val rect = android.graphics.Rect()
                focusedNode.getBoundsInScreen(rect)
                focusedNode.recycle()
                return rect.toShortString() // 使用 bounds 作为唯一ID
            }
            return null
        }

        override fun setTextOnNode(nodeId: String, text: String): Boolean {
            val rootNode = rootInActiveWindow ?: return false
            val targetNode = findNodeByBounds(rootNode, nodeId)
            rootNode.recycle()

            if (targetNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                targetNode.recycle()
                return result
            }
            return false
        }

        override fun isAccessibilityServiceEnabled(): Boolean {
            return isServiceConnected
        }
    }


    companion object {
        private const val TAG = "UIAccessibilityService"
        var isServiceConnected = false
            private set
        var binder: IAccessibilityProvider.Stub? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        binder = this.accessibilityBinder
        Log.d(TAG, "服务已连接，状态更新为 true")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceConnected = false
        binder = null
        Log.d(TAG, "服务已解绑，状态更新为 false")
        return super.onUnbind(intent)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() {
        isServiceConnected = false
        binder = null
        Log.d(TAG, "服务已中断，状态更新为 false")
    }

    private fun findNodeByBounds(root: AccessibilityNodeInfo, boundsString: String): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        root.getBoundsInScreen(rect)
        if (rect.toShortString() == boundsString) {
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val found = findNodeByBounds(child, boundsString)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    private fun captureUiHierarchyAsXml(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val serializer = Xml.newSerializer()
        val writer = StringWriter()
        try {
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializeNodeToXml(rootNode, serializer)
            serializer.endDocument()
            return writer.toString()
        } catch (e: Exception) {
            Log.e(TAG, "生成UI XML时出错", e)
            return ""
        } finally {
            rootNode.recycle()
        }
    }

    private fun serializeNodeToXml(node: AccessibilityNodeInfo?, serializer: XmlSerializer) {
        if (node == null) return
        serializer.startTag(null, "node")
        // 添加属性
        serializer.attribute(null, "class", node.className?.toString() ?: "")
        serializer.attribute(null, "package", node.packageName?.toString() ?: "")
        serializer.attribute(null, "content-desc", node.contentDescription?.toString() ?: "")
        serializer.attribute(null, "text", node.text?.toString() ?: "")
        serializer.attribute(null, "resource-id", node.viewIdResourceName ?: "")
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        serializer.attribute(null, "bounds", bounds.toShortString() ?: "")
        serializer.attribute(null, "clickable", node.isClickable.toString())
        serializer.attribute(null, "focused", node.isFocused.toString())
        // ...可以按需添加更多属性

        for (i in 0 until node.childCount) {
            serializeNodeToXml(node.getChild(i), serializer)
        }
        serializer.endTag(null, "node")
        node.recycle()
    }
} 