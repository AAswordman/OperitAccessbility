# RemoteBinderService.kt 内容介绍

## 基本结构

这是一个 Android 服务(Service)类，主要作用是作为 IPC(进程间通信)的桥梁，将辅助功能服务(UIAccessibilityService)的能力暴露给其他应用。

```kotlin
package com.ai.assistance.operit.provider

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class RemoteBinderService : Service() {
    // ...
}
```

## 核心成员变量

这是一个 AIDL 接口的实现，用于跨进程通信

```
private lateinit var proxyBinder: IAccessibilityProvider.Stub
```

`lateinit` 表示这个变量会在 `onCreate()` 中初始化

## 服务生命周期方法

onCreate()

在这里初始化 proxyBinder，创建一个匿名内部类实现 AIDL 接口

```kotlin
override fun onCreate() {
    super.onCreate()
    proxyBinder = object : IAccessibilityProvider.Stub() {
        // 实现所有AIDL接口方法
    }
}
```

onBind()

当其他组件绑定到此服务时，返回 proxyBinder 对象

```kotlin
override fun onBind(intent: Intent): IBinder {
    return proxyBinder
}
```

## AIDL 接口方法实现

所有方法都遵循相同模式：

- 检查辅助功能服务是否已连接
- 如果未连接，记录警告日志并返回安全值
- 如果已连接，委托给 `UIAccessibilityService.binder` 处理

## 方法解析示例

以 `getUiHierarchy()` 为例

```kotlin
override fun getUiHierarchy(): String {
    if (!UIAccessibilityService.isServiceConnected) {
        Log.w("RemoteBinderService", "getUiHierarchy: Accessibility Service not connected.")
        return ""
    }
    return UIAccessibilityService.binder?.getUiHierarchy() ?: ""
}
```

- 功能：获取当前UI层级结构
- 检查 isServiceConnected 标志
- 未连接时返回空字符串
- 已连接时调用 `UIAccessibilityService.getUiHierarchy()` 实现

其他方法类似...
