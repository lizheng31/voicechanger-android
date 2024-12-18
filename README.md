# VoiceChager - Android 变声器应用

一款简洁现代的 Android 变声器应用，支持录音、变声和音频管理功能。

## 功能概述

### 已实现功能
- ✅ 高质量音频录制 (16kHz, 单声道, 128kbps)
- ✅ 多种预设音色变声
- ✅ 音频文件管理和预览
- ✅ Material Design 3 界面设计
- ✅ 音频时长和大小显示

### 开发中功能
- 🚧 微信语音集成
- 🚧 悬浮球快捷控制
- 🚧 更多音色支持

## 系统架构

### 1. 核心模块

#### 1.1 音频录制模块 (AudioRecordService)
- 音频录制和编码
- 文件保存管理
- 录音状态控制

#### 1.2 变声处理模块 (VoiceUploadManager)
- WebSocket 音频传输
- 实时变声处理
- 音频格式转换

#### 1.3 音频管理模块 (AudioPlayerManager)
- 音频文件管理
- 播放控制
- 文件操作

### 2. 界面模块

#### 2.1 主界面 (MainActivity)
- 录音控制
- 音色选择
- 文件列表显示

#### 2.2 悬浮控制模块 (开发中)
- 快捷音色切换
- 录音快捷入口
- 状态显示

### 3. 微信集成模块 (开发中)

#### 3.1 无障碍服务
```java
public class VoiceAccessibilityService extends AccessibilityService {
    // 微信按钮识别
    // 音频播放控制
    // 事件同步处理
}
```

#### 3.2 操作流程
1. 检测微信语音按钮
2. 触发音频播放
3. 同步发送控制

## 技术栈

- 音频处理：MediaRecorder, FFmpeg
- 网络通信：OkHttp, WebSocket
- UI 框架：Material Design 3
- 权限管理：PermissionX

## 项目结构

```
app/src/main/
├── java/com/mengmeng/voicechager/
│   ├── api/                    # API 相关
│   ├── models/                 # 数据模型
│   ├── services/              # 服务组件
│   ├── utils/                 # 工具类
│   └── MainActivity.java      # 主界面
└── res/                       # 资源文件
```

## 开发计划

### 1. 近期目标 (1-2周)
- [ ] 完善音频管理功能
- [ ] 优化变声效果
- [ ] 添加更多音色选项

### 2. 中期目标 (3-4周)
- [ ] 实现微信集成基础框架
- [ ] 开发悬浮球控制
- [ ] 优化用户交互体验

### 3. 长期目标 (1-2月)
- [ ] 完善微信集成功能
- [ ] 提升系统稳定性
- [ ] 优化性能和内存占用

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 发起 Pull Request

## 开发环境

- Android Studio Hedgehog | 2023.1.1
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)
- Java 11

## 许可证

[MIT License](LICENSE)
