# 语音变声器 (Voice Changer)

一个Android应用，可以录制语音、变声，并在微信语音聊天中使用变声后的音频。

+ ## 开发环境
+ 
+ - Android Studio Hedgehog | 2023.1.1
+ - Minimum SDK: Android 8.0 (API 26)
+ - Target SDK: Android 14 (API 34)
+ - Gradle Version: 8.2.0
+ - Java Version: 11
+ 
+ ## 安装说明
+ 
+ ### 方法一：直接安装
+ 1. 在 [Releases](https://github.com/yourusername/voicechanger/releases) 页面下载最新的 APK
+ 2. 在 Android 设备上安装 APK
+ 3. 首次运行时授予必要权限
+ 
+ ### 方法二：从源码构建
+ 1. Clone 项目到本地
+    ```bash
+    git clone https://github.com/yourusername/voicechanger.git
+    ```
+ 2. 在 Android Studio 中打开项目
+ 3. 等待 Gradle 同步完成
+ 4. 点击 "Run" 按钮或使用 `./gradlew installDebug` 安装到设备
+ 
## 主要功能

1. 语音录制
   - 支持录制原声音频
   - 显示录音时长
   - 实时显示录音状态

2. 变声处理
   - 支持多种变声效果
   - 可预览变声效果
   - 保存变声后的音频

3. 音频管理
   - 列表显示所有录音
   - 支持播放/暂停
   - 支持删除音频
   - 区分原声和变声后的音频

4. 微信发送
   - 支持在微信语音聊天中使用变声音频
   - 自动检测微信语音模式
   - 自动播放选定的音频

## 使用方法

1. 录制语音
   - 点击录音按钮开始录音
   - 再次点击结束录音
   - 录音文件会显示在列表中

2. 变声处理
   - 选择一个原声录音
   - 选择想要的变声效果
   - 点击变声按钮进行转换
   - 转换后的文件会显示在列表中

3. 在微信中使用
   - 选择一个变声后的音频
   - 点击发送按钮
   - 切换到微信聊天界面
   - 进入语音模式（点击"按住 说话"）
   - 应用会自动播放选定的音频

## 注意事项

1. 首次使用需要授予以下权限：
   - 录音权限
   - 存储权限
   - 通知权限
   - 无障碍服务权限

2. 使用微信发送功能时：
   - 需要开启无障碍服务
   - 确保后台服务不被系统清理
   - 给予应用后台运行权限

## 技术实现

- 使用 MediaRecorder 实现录音功能
- 使用 FFmpeg 进行音频处理
- 使用 AccessibilityService 监测微信状态
- 使用 Foreground Service 确保后台稳定运行

## 版本历史

### v1.0.0
- 实现基本的录音功能
- 实现变声处理
- 实现微信发送功能

## 后续计划

1. 添加更多变声效果
2. 优化音频处理质量
3. 支持更多社交软件
4. 添加音频编辑功能
5. 优化用户界面

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目。

## 许可证

[MIT License](LICENSE)
