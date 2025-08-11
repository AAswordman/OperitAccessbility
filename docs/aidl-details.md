# IAccessibilityProvider 详解

在 [aidl](aidl.md) 一文中我们介绍了 `IAccessibilityProvider` 接口及实现主体脉络,本文继续介绍核心功能的实现,以便理解和掌握.

## 1.`getUiHierarchy`获取UI层次结构

实现方式:通过递归遍历 `AccessibilityNodeInfo` 的UI树结构,生成XML格式的视图树

关键代码:

```kotlin
private fun captureUiHierarchyAsXml(): String {
    val rootNode = rootInActiveWindow ?: return ""
    // 使用XmlSerializer序列化节点树
}

```

输出示例

```xml
<node class="android.widget.FrameLayout" bounds="[0,0][1080,1920]">
  <node class="android.widget.Button" text="确定" bounds="[400,800][680,920]"/>
</node>

```

## `performClick()` - 在指定坐标(x,y)执行点击操作

## 节点操作-文本设置

文本设置

```kotlin
val arguments = Bundle().apply {
    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
}
targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
```
