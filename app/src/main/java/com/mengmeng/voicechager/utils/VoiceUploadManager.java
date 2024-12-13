package com.mengmeng.voicechager.utils;

import com.mengmeng.voicechager.api.ApiClient;
import com.mengmeng.voicechager.api.VoiceWebSocketManager;
import com.mengmeng.voicechager.models.VoiceType;
import java.io.File;

public class VoiceUploadManager {
    private VoiceWebSocketManager webSocketManager;
    
    public interface VoiceCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
    }
    
    public void cloneVoice(File audioFile, VoiceType voiceType, VoiceCallback callback) {
        // 读取音频文件
        byte[] audioData = readAudioFile(audioFile);
        if (audioData == null) {
            callback.onError("Failed to read audio file");
            return;
        }
        
        // 创建WebSocket管理器
        webSocketManager = new VoiceWebSocketManager(new VoiceWebSocketManager.VoiceCallback() {
            @Override
            public void onSuccess(byte[] convertedAudio) {
                // 验证音频数据
                if (convertedAudio == null || convertedAudio.length == 0) {
                    callback.onError("接收到的音频数据无效");
                    return;
                }
                
                // 检查音频数据的基本格式
                if (convertedAudio.length < 44) { // WAV头至少44字节
                    LogUtils.d("接收到的音频数据可能不是WAV格式，尝试添加WAV头");
                } else {
                    LogUtils.d("接收到音频数据: " + convertedAudio.length + " bytes");
                }
                
                callback.onSuccess(convertedAudio);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onReady() {
                // 连接就绪后发送音频数据
                webSocketManager.sendAudio(audioData);
                webSocketManager.sendEndFrame();
            }
        }, voiceType);
        
        // 连接WebSocket
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