# VoiceChanger - 语音变声器

## 项目简介
VoiceChanger 是一个 Android 语音变声应用，允许用户录制和编辑音频，添加有趣的声音效果。

## 项目结构
```
app/src/main/
├── java/com/mengmeng/voicechager/
│   ├── api/                    # API 相关
│   │   └── VoiceService.java   # 变声服务接口
│   ├── models/                 # 数据模型
│   │   └── AudioItem.java      # 音频项数据模型
│   ├── services/              # 服务组件
│   │   ├── AudioService.java   # 音频处理服务
│   │   └── RecordService.java  # 录音服务
│   ├── utils/                 # 工具类
│   │   ├── AudioUtils.java     # 音频工具
│   │   └── FileUtils.java      # 文件工具
│   └── MainActivity.java      # 主界面
└── res/                       # 资源文件
```

## 功能说明

### 已实现功能
- ✅ 高质量音频录制 (16kHz, 单声道, 128kbps)
- ✅ 多种预设音色变声
- ✅ 音频文件管理和预览
- ✅ Material Design 3 界面设计
- ✅ 音频时长和大小显示

### 开发计划
1. 近期目标 (1-2周)
   - [ ] 完善音频管理功能
   - [ ] 优化变声效果
   - [ ] 添加更多音色选项

2. 中期目标 (3-4周)
   - [ ] 实现微信集成基础框架
   - [ ] 开发悬浮球控制
   - [ ] 优化用户交互体验

3. 长期目标 (1-2月)
   - [ ] 完善微信集成功能
   - [ ] 提升系统稳定性
   - [ ] 优化性能和内存占用

## 技术规范

### API 说明
1. 音频录制接口
```java
void startRecording(String filePath);
void stopRecording();
void pauseRecording();
```

2. 变声处理接口
```java
void applyVoiceEffect(String audioPath, VoiceEffect effect);
void previewEffect(String audioPath, VoiceEffect effect);
```

3. 音频管理接口
```java
List<AudioItem> getAudioList();
void deleteAudio(String audioPath);
void renameAudio(String oldPath, String newPath);
```

### 开发环境
- Android Studio Hedgehog | 2023.1.1
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)
- Java 11

### 构建与运行
1. Clone 项目到本地
2. 在 Android Studio 中打开项目
3. 等待 Gradle 同步完成
4. 点击 "Run" 按钮运行项目

## 贡献指南
1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 发起 Pull Request

## 许可证
[MIT License](LICENSE)
