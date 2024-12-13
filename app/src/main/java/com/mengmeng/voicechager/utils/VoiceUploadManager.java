package com.mengmeng.voicechager.utils;

import com.mengmeng.voicechager.api.ApiClient;
import com.mengmeng.voicechager.api.VoiceWebSocketManager;
import java.io.File;

public class VoiceUploadManager {
    private VoiceWebSocketManager webSocketManager;
    
    public interface VoiceCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
    }
    
    public void cloneVoice(File audioFile, VoiceCallback callback) {
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
        });
        
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