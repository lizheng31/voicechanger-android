package com.mengmeng.voicechager.utils;

import com.mengmeng.voicechager.api.ApiClient;
import com.mengmeng.voicechager.api.VoiceWebSocketManager;
import com.mengmeng.voicechager.models.VoiceType;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Context;

public class VoiceUploadManager {
    private VoiceWebSocketManager webSocketManager;
    private final Context context;
    
    public VoiceUploadManager(Context context) {
        this.context = context;
    }
    
    public interface VoiceCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
    }
    
    private String saveConvertedAudio(byte[] audioData, VoiceType voiceType) {
        try {
            // 创建保存目录
            File outputDir = new File(context.getExternalFilesDir(null), "converted");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 创建文件名 (使用时间戳)
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = String.format("converted_%s_%s.mp3", timestamp, voiceType.getCode());
            File outputFile = new File(outputDir, fileName);

            // 写入文件
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(audioData);
            fos.close();

            LogUtils.d(String.format("音频文件已保存: %s, 大小: %d 字节", outputFile.getAbsolutePath(), audioData.length));
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            LogUtils.e("保存音频文件失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    private byte[] convertAudioToMP3(byte[] audioData) {
        try {
            // 创建临时文件
            File inputFile = File.createTempFile("input", ".aac", context.getCacheDir());
            File outputFile = File.createTempFile("output", ".mp3", context.getCacheDir());
            
            // 写入输入文件
            FileOutputStream fos = new FileOutputStream(inputFile);
            fos.write(audioData);
            fos.close();
            
            // 使用 FFmpeg 转换，添加 -y 参数强制覆盖输出文件
            String[] cmd = {"-y",                    // 强制覆盖输出文件
                           "-i", inputFile.getPath(), 
                           "-acodec", "libmp3lame",
                           "-ar", "16000",           // 采样率
                           "-ac", "1",               // 单声道
                           "-b:a", "128k",           // 比特率
                           "-f", "mp3",              // 强制输出格式为 MP3
                           outputFile.getPath()};
                           
            LogUtils.d("执行 FFmpeg 命令: " + String.join(" ", cmd));
            int rc = com.arthenica.mobileffmpeg.FFmpeg.execute(cmd);
            
            if (rc == com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {
                // 检查输出文件是否存在和大小
                if (!outputFile.exists()) {
                    LogUtils.e("输出文件不存在");
                    return null;
                }
                
                // 读取转换后的文件
                byte[] mp3Data = Files.readAllBytes(outputFile.toPath());
                LogUtils.d(String.format("音频格式转换成功：输入大小 %d bytes，输出大小 %d bytes", 
                    audioData.length, mp3Data.length));
                
                // 清理临时文件
                inputFile.delete();
                outputFile.delete();
                
                return mp3Data;
            } else {
                LogUtils.e("FFmpeg 转换失败，返回码：" + rc);
                // 打印输出文件信息
                if (outputFile.exists()) {
                    LogUtils.e("输出文件存在，大小：" + outputFile.length() + " bytes");
                } else {
                    LogUtils.e("输出文件不存在");
                }
                return null;
            }
        } catch (Exception e) {
            LogUtils.e("音频格式转换失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    public void cloneVoice(File audioFile, VoiceType voiceType, VoiceCallback callback) {
        // 读取音频文件
        byte[] audioData = readAudioFile(audioFile);
        if (audioData == null) {
            callback.onError("Failed to read audio file");
            return;
        }
        
        // 转换为 MP3 格式
        byte[] mp3Data = convertAudioToMP3(audioData);
        if (mp3Data == null) {
            callback.onError("Failed to convert audio format");
            return;
        }
        
        // 创建 WebSocket 管理器
        webSocketManager = new VoiceWebSocketManager(
            context,
            new VoiceWebSocketManager.VoiceCallback() {
                @Override
                public void onSuccess(byte[] convertedAudio) {
                    // 验证音频数据
                    if (convertedAudio == null || convertedAudio.length == 0) {
                        callback.onError("接收到的音频数据无效");
                        return;
                    }
                    
                    // 保存音频文件
                    String savedPath = saveConvertedAudio(convertedAudio, voiceType);
                    if (savedPath == null) {
                        callback.onError("保存音频文件失败");
                        return;
                    }
                    
                    LogUtils.d("接收到音频数据: " + convertedAudio.length + " bytes");
                    callback.onSuccess(convertedAudio);
                }
                
                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
                
                @Override
                public void onReady() {
                    // 发送转换后的 MP3 数据
                    webSocketManager.sendAudio(mp3Data);
                    webSocketManager.sendEndFrame();
                }
            },
            voiceType
        );
        
        webSocketManager.connect();
    }
    
    private byte[] readAudioFile(File file) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (Exception e) {
            LogUtils.e("Read audio file failed", e);
            return null;
        }
    }
    
    public void close() {
        if (webSocketManager != null) {
            webSocketManager.close();
            webSocketManager = null;
        }
    }
}