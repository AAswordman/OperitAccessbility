# Operit Accessbility

一个提供无障碍服务和远程绑定功能的Android应用。

![Kotlin](https://img.shields.io/badge/Kotlin-1.8-蓝色.svg)![Android](https://img.shields.io/badge/Android-26%2B-绿色.svg)![许可证](https://img.shields.io/badge/许可证-Apache%202.0-蓝色.svg)

## 功能特性

- 无障碍服务：通过UIAccessibilityService提供界面无障碍功能
- 远程绑定：通过RemoteBinderService提供跨进程通信能力
- 现代架构：基于Kotlin和Android最新SDK(34)构建

## 系统要求

- Android SDK 26+
- Kotlin 1.8
- Gradle 8.0+

## 构建步骤

在Android Studio中打开项目或直接构建：

```
# 构建-调试版本：
./gradlew assembleDebug

# 构建-正式版本：
./gradlew assembleRelease

```

## 配置说明

签名密钥配置

在项目根目录创建keystore.properties文件，内容如下：

```
RELEASE_KEY_ALIAS=您的密钥别名
RELEASE_KEY_PASSWORD=您的密钥密码
RELEASE_STORE_FILE=路径/到/您的/keystore.jks
RELEASE_STORE_PASSWORD=您的存储密码
```

## 无障碍服务配置

在`res/xml/accessibility_service_config.xml`中配置服务参数

## 项目结构

```
app/
├── src/main/
│   ├── java/com/ai/assistance/operit/provider/
│   │   ├── MainActivity.kt       # 主入口界面
│   │   ├── MyApplication.kt      # 应用类
│   │   ├── RemoteBinderService.kt # IPC服务
│   │   └── UIAccessibilityService.kt # 无障碍服务
│   ├── aidl/                     # AIDL接口定义
│   └── res/                      # 资源文件

```

## 贡献指南

1. Fork本项目
2. 创建特性分支 (git checkout -b feature/新特性),
  - 并提交修改 (git commit -m '添加新特性')
  - 推送到分支 (git push origin feature/新特性)
3. 发起 Pull Request
