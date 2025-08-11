# MainActivity.kt 详解

先上主流程

1. UI交互 - 通过按钮点击打开系统无障碍设置
2. 状态监控 - 每秒检查一次无障碍服务的链接状态
3. 生命周期管理 - 只在 Activity 可见时执行状态检查
4. 线程处理 - 使用Handler在主线程执行定时任务

## MainActivity.kt - 主活动界面

作用:

- 这是应用的主入口界面,继承自 `AppCompatActivity`
- 主要功能:
  - 显示一个界面(对应布局文件 `activity_main.xml`)
  - 检查并显示无障碍服务的连接状态
  - 提供按钮跳转到无障碍设置页面

关键代码解析:

```kotlin
// 设置界面布局
setContentView(R.layout.activity_main)

// 跳转到无障碍设置的按钮点击事件
goToSettingsButton.setOnClickListener {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)
}

// 每秒检查一次服务状态
handler.postDelayed(statusCheckRunnable, 1000)
```

## 包声明

package 声明了该文件所属的包名定义

```kotlin
package com.ai.assistance.operit.provider
```

## 导入声明

import 语句导入了所需的Android包

```kotlin
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ai.assistance.operit.provider.R
```

关键导入包括：

- Intent - 用于启动Acitivity
- Handler/Looper - 用于线程处理
- Settings - 访问系统设置


## MainActivity 类定义

```kotlin
class MainActivity : AppCompatActivity()
```

- 继承自 `AppCompatActivity`，这是Android支持库中提供兼容性支持的Activity基类
- 这是应用的主Activity，通常也是应用的入口点

## 成员变量

```kotlin
private lateinit var statusText: TextView
private val handler = Handler(Looper.getMainLooper())
private lateinit var statusCheckRunnable: Runnable
```

- `statusText`:用于显示服务状态的TextView,使用lateinit表示稍后初始化
- `handler`:主线程的Handler,用于在主线程执行任务
- `statusCheckRunnable`:定期检查服务状态的Runnable任务

## 生命周期方法

onCreate方法

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)  // 设置布局文件

    // 初始化UI组件
    statusText = findViewById(R.id.status_text)
    val goToSettingsButton: Button = findViewById(R.id.go_to_settings_button)

    // 设置按钮点击事件
    goToSettingsButton.setOnClickListener {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)  // 打开系统无障碍设置
    }

    // 创建定期检查服务状态的任务
    statusCheckRunnable = Runnable {
        updateStatus()  // 更新状态
        handler.postDelayed(statusCheckRunnable, 1000) // 每秒检查一次
    }
}
```

- `onCreate`是Activity的声明周期方法之一,在Activity创建时调用
- `setContentView`设置了布局文件`activity_main.xml`
- 初始化了UI组件并设置了按钮点击事件
- 点击按钮会打开系统的无障碍设置界面
- 创建了一个每秒执行一次的定时任务来检查服务状态

onResume方法

Activity可见时调用,开始定期检查服务状态

```
override fun onResume() {
    super.onResume()
    handler.post(statusCheckRunnable)  // Activity可见时开始检查
}
```

onPause方法

Activity不可见时调用,停止定时任务检查以节省资源

```kotlin
override fun onPause() {
    super.onPause()
    handler.removeCallbacks(statusCheckRunnable)  // Activity不可见时停止检查
}
```

## 自定义方法

updateStatus方法

- 检查`UIAccessibilityService`是否已连接
- 根据连接状态更新UI:
  - 已连接:显示"已连接"并使用绿色
  - 未连接:显示"未连接"并使用红色

```
private fun updateStatus() {
    if (UIAccessibilityService.isServiceConnected) {
        statusText.text = "服务状态: 已连接"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
    } else {
        statusText.text = "服务状态: 未连接"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
    }
}
```
