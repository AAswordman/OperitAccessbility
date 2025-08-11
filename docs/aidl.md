# AIDL

`IAccessibilityProvider.aidl` 是一个 Android 接口定义语言(AIDL)文件，定义了一个跨进程通信(IPC)的接口，本服务中主要用于辅助功能（无障碍服务）相关的操作。

文件内容如下:

```
package com.ai.assistance.operit.provider;

interface IAccessibilityProvider {
    String getUiHierarchy();
    boolean performClick(int x, int y);
    boolean performGlobalAction(int actionId);
    boolean performSwipe(int startX, int startY, int endX, int endY, long duration);
    String findFocusedNodeId();
    boolean setTextOnNode(String nodeId, String text);
    boolean isAccessibilityServiceEnabled();
}

```

## 一.接口功能解析

`IAccessibilityProvider` 接口定义了7个方法，每个方法都与Android的无障碍服务功能相关：

1. `getUiHierarchy()` - 获取当前屏幕的UI层次结构（类似XML格式的视图树）
2. `performClick()` - 在指定坐标(x,y)执行点击操作
3. `performGlobalAction()` - 执行全局操作(比如返回、主页等)
4. `performSwipe()` - 执行滑动操作,从(startX,startY)滑动到(endX,endY),持续时间为duration毫秒
5. `findFocusedNodeId()` - 获取当前焦点的节点ID
6. `setTextOnNode()` - 在指定的节点上设置文本内容
7. `isAccessibilityServiceEnabled()` - 检查无障碍服务是否已启用

## 二.服务实现

实现了一个 Service 来实现 `IAccessibilityProvider` 接口里的所有方法

```
// RemoteBinderService.kt

class RemoteBinderService : Service() {
    // ...
}
```

## 三.声明并暴露服务

在 AndroidManifest.xml 中声明服务:

```xml
<service
    android:name=".provider.RemoteBinderService"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.ai.assistance.operit.provider.IAccessibilityProvider" />
    </intent-filter>
</service>

```

## 四.实现基于无障碍服务的UIAccessibilityService

> 注意事项:
> 上述这些方法多数需要无障碍服务支持，所以需要申请无障碍服务权限

所以在 AndroidManifest.xml 中声明无障碍服务用户权限

```
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```


```
class UIAccessibilityService : AccessibilityService() {
    // ...
}
```

最后 `RemoteBinderService` 中的实现会去调用具备无障碍访问能力的 `UIAccessibilityService` 来完成自动化操作.
