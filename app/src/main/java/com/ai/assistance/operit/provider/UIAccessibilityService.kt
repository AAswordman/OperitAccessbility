package com.ai.assistance.operit.provider

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import android.util.Xml

class UIAccessibilityService : AccessibilityService() {

    private val screenshotLock = Any()
    private var lastScreenshotTimestamp: Long = 0L
    private val minScreenshotIntervalMs: Long = 1100L

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

            // 2. 用Path创建一个手势"笔划"
            // 点击的持续时间需要一个合理的值，不能太短，例如50毫秒
            val clickStroke = GestureDescription.StrokeDescription(clickPath, 0L, 50L)

            // 3. 用"笔划"构建完整的手势描述
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

        override fun performLongPress(x: Int, y: Int): Boolean {
            Log.d(TAG, "准备在 ($x, $y) 执行长按...")

            val longPressPath = android.graphics.Path().apply {
                moveTo(x.toFloat(), y.toFloat())
                lineTo(x.toFloat(), y.toFloat())
            }

            val longPressStroke = GestureDescription.StrokeDescription(longPressPath, 0L, 600L)

            val gestureDescription = GestureDescription.Builder()
                .addStroke(longPressStroke)
                .build()

            return this@UIAccessibilityService.dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.i(TAG, "长按手势已成功完成。")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "长按手势被取消。")
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
            Log.d(TAG, "准备为节点 $nodeId 设置文本: '$text'")
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "setTextOnNode 失败: rootInActiveWindow is null")
                return false
            }

            val containerNode = findNodeByBounds(rootNode, nodeId)
            rootNode.recycle()

            if (containerNode == null) {
                Log.w(TAG, "setTextOnNode 失败: 无法通过ID '$nodeId' 找到目标容器节点")
                return false
            }

            var targetNode: AccessibilityNodeInfo? = null
            try {
                // 在容器（或其本身）中寻找第一个可编辑的节点
                targetNode = findFirstEditableNode(containerNode)

                if (targetNode == null) {
                    Log.w(TAG, "setTextOnNode 失败: 在节点 $nodeId 及其子节点中未找到可编辑的节点。")
                    return false
                }

                // 在找到的可编辑节点上执行操作
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                if (!result) {
                    val bounds = android.graphics.Rect()
                    targetNode.getBoundsInScreen(bounds)
                    Log.w(TAG, "setTextOnNode: performAction(ACTION_SET_TEXT) 在目标节点上返回 false. 节点信息: class=${targetNode.className}, text='${targetNode.text}', bounds=${bounds.toShortString()}")
                }
                return result
            } finally {
                containerNode.recycle()
                targetNode?.recycle()
            }
        }

        override fun takeScreenshot(path: String, format: String): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return false
            }
            var resultValue = false
            val normalizedFormat = format.lowercase()
            synchronized(screenshotLock) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastScreenshotTimestamp
                if (elapsed in 0 until minScreenshotIntervalMs) {
                    try {
                        Thread.sleep(minScreenshotIntervalMs - elapsed)
                    } catch (_: InterruptedException) {
                    }
                }
                lastScreenshotTimestamp = System.currentTimeMillis()
                val latch = CountDownLatch(1)
                this@UIAccessibilityService.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    this@UIAccessibilityService.mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            hardwareBuffer.close()
                            if (bitmap != null) {
                                try {
                                    val file = File(path)
                                    val parent = file.parentFile
                                    if (parent != null && !parent.exists()) {
                                        parent.mkdirs()
                                    }
                                    val compressFormat = when (normalizedFormat) {
                                        "png" -> Bitmap.CompressFormat.PNG
                                        "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
                                        else -> Bitmap.CompressFormat.PNG
                                    }
                                    file.outputStream().use { output ->
                                        val quality = if (compressFormat == Bitmap.CompressFormat.JPEG) 90 else 100
                                        resultValue = bitmap.compress(compressFormat, quality, output)
                                    }
                                } catch (_: Exception) {
                                    resultValue = false
                                } finally {
                                    bitmap.recycle()
                                }
                            } else {
                                resultValue = false
                            }
                            latch.countDown()
                        }

                        override fun onFailure(errorCode: Int) {
                            resultValue = false
                            latch.countDown()
                        }
                    }
                )
                try {
                    latch.await()
                } catch (_: InterruptedException) {
                    resultValue = false
                }
            }
            return resultValue
        }

        override fun isAccessibilityServiceEnabled(): Boolean {
            return isServiceConnected
        }

        override fun getCurrentActivityName(): String {
            return currentActivityName
        }
    }


    companion object {
        private const val TAG = "UIAccessibilityService"
        var isServiceConnected = false
            private set
        var binder: IAccessibilityProvider.Stub? = null
            private set
        
        // Cache for the current activity name
        @Volatile
        var currentActivityName: String = ""
            internal set
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
        currentActivityName = ""
        Log.d(TAG, "服务已解绑，状态更新为 false")
        return super.onUnbind(intent)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Listen for window state changes to detect activity changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val className = event.className?.toString()
            if (!className.isNullOrEmpty()) {
                currentActivityName = className
                Log.d(TAG, "Activity changed to: $className")
            }
        }
    }
    
    override fun onInterrupt() {
        isServiceConnected = false
        binder = null
        currentActivityName = ""
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
                child.recycle() // 立即回收，避免泄漏
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val editableNode = findFirstEditableNode(child)
                child.recycle() // 立即回收，避免泄漏
                if (editableNode != null) {
                    return editableNode
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
            // serializeNodeToXml 已经递归回收了包括 rootNode 在内的所有节点，这里无需再次回收
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