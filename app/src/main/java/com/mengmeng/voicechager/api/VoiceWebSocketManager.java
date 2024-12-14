package com.mengmeng.voicechager.api;

import com.mengmeng.voicechager.models.VoiceType;
import com.mengmeng.voicechager.utils.AuthUtils;
import com.mengmeng.voicechager.utils.LogUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class VoiceWebSocketManager extends WebSocketListener {
    private WebSocket webSocket;
    private final VoiceCallback callback;
    private boolean isBusinessReady = false;
    private final VoiceType voiceType;
    private int currentSeq = 0;
    private static final int MAX_FRAME_SIZE = 1024; // 1KB per frame as per API requirement
    
    // 添加音频缓存
    private List<byte[]> audioFrames = new ArrayList<>();
    
    public interface VoiceCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
        void onReady();
    }
    
    public VoiceWebSocketManager(VoiceCallback callback, VoiceType voiceType) {
        this.callback = callback;
        this.voiceType = voiceType;
    }
    
    public void connect() {
        String url = AuthUtils.getAuthUrl(
            ApiClient.getApiKey(),
            ApiClient.getApiSecret()
        );
        
        if (url == null) {
            callback.onError("鉴权失败");
            return;
        }
        
        // 添加 URL 检查日志
        if (!url.startsWith("wss://")) {
            LogUtils.e("WebSocket URL 协议错误: " + url);
            callback.onError("WebSocket URL 协议错误");
            return;
        }
        
        LogUtils.d("WebSocket URL: " + url);
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        webSocket = ApiClient.getClient().newWebSocket(request, this);
    }
    
    private void sendBusinessParams() {
        try {
            JSONObject frame = new JSONObject();
            
            // header (必传)
            JSONObject header = new JSONObject();
            header.put("app_id", ApiClient.getAppId());
            header.put("status", 0);  // 0-开始
            frame.put("header", header);
            
            // parameter (必传)
            JSONObject parameter = new JSONObject();
            JSONObject xvc = new JSONObject();
            
            xvc.put("voiceName", voiceType.getCode());
            xvc.put("speed", 0);
            xvc.put("volume", 0);
            xvc.put("pitch", 0);
            xvc.put("vocoder_mode", 0);
            
            // result (必传) - 修改为回mp3格式
            JSONObject result = new JSONObject();
            result.put("encoding", "lame");        // 使用lame(mp3)编码
            result.put("sample_rate", 16000);
            result.put("channels", 1);
            result.put("bit_depth", 16);
            result.put("frame_size", 0);          // 使用默认帧大小
            xvc.put("result", result);
            
            parameter.put("xvc", xvc);
            frame.put("parameter", parameter);
            
            // payload (必传)
            JSONObject payload = new JSONObject();
            JSONObject inputAudio = new JSONObject();
            inputAudio.put("encoding", "raw");    // 发送PCM原始数据
            inputAudio.put("sample_rate", 16000);
            inputAudio.put("channels", 1);
            inputAudio.put("bit_depth", 16);
            inputAudio.put("status", 0);
            inputAudio.put("seq", 0);
            inputAudio.put("frame_size", 0);      // 使用默认帧大小
            payload.put("input_audio", inputAudio);
            frame.put("payload", payload);
            
            String message = frame.toString();
            boolean sent = webSocket.send(message);
            if (!sent) {
                callback.onError("业务参数发送失败");
                return;
            }
            LogUtils.d("开始发送音频数据...");
        } catch (Exception e) {
            callback.onError("发送业务参数失败");
        }
    }
    
    public void sendAudio(byte[] audioData) {
        LogUtils.d("准备��送音频数据大小: " + audioData.length + " 字节");
        
        if (webSocket != null && isBusinessReady) {
            try {
                if (audioData == null || audioData.length == 0) {
                    callback.onError("音频数据为空");
                    return;
                }

                LogUtils.d("开始转换音色...");
                int offset = 0;
                int frameCount = 0;
                while (offset < audioData.length) {
                    int frameSize = Math.min(MAX_FRAME_SIZE, audioData.length - offset);
                    byte[] frameData = new byte[frameSize];
                    System.arraycopy(audioData, offset, frameData, 0, frameSize);
                    
                    if (frameData.length > 10485760) {
                        callback.onError("音频数据超过大小限制");
                        return;
                    }
                    
                    String base64Audio = android.util.Base64.encodeToString(frameData, android.util.Base64.NO_WRAP);
                    
                    JSONObject frame = new JSONObject();
                    
                    // header
                    JSONObject header = new JSONObject();
                    header.put("app_id", ApiClient.getAppId());
                    header.put("status", offset + frameSize >= audioData.length ? 2 : 1);
                    frame.put("header", header);
                    
                    // payload
                    JSONObject payload = new JSONObject();
                    JSONObject inputAudio = new JSONObject();
                    inputAudio.put("encoding", "raw");    // 保持输入为raw格式
                    inputAudio.put("sample_rate", 16000);
                    inputAudio.put("channels", 1);
                    inputAudio.put("bit_depth", 16);
                    inputAudio.put("frame_size", 0);      // 使用默认帧大小,与初始化参数保持一致
                    inputAudio.put("status", offset + frameSize >= audioData.length ? 2 : 1);
                    inputAudio.put("seq", frameCount);
                    inputAudio.put("audio", base64Audio);
                    
                    payload.put("input_audio", inputAudio);
                    frame.put("payload", payload);
                    
                    String message = frame.toString();
                    if (frameCount % 50 == 0) { // 大幅减少进度日志
                        LogUtils.d(String.format("音频转换进度: %.1f%%", 
                            (float)offset / audioData.length * 100));
                    }
                    
                    boolean sent = webSocket.send(message);
                    if (!sent) {
                        callback.onError("音频数据发送失败");
                        return;
                    }
                    
                    Thread.sleep(20);
                    offset += frameSize;
                    frameCount++;
                }
            } catch (Exception e) {
                callback.onError("音频处理失败");
            }
        } else {
            callback.onError("音频服务未就绪");
        }
    }
    
    public void sendEndFrame() {
        if (webSocket != null && isBusinessReady) {
            try {
                JSONObject frame = new JSONObject();
                
                // header
                JSONObject header = new JSONObject();
                header.put("app_id", ApiClient.getAppId());
                header.put("status", 2);  // 2-结束
                frame.put("header", header);
                
                // payload
                JSONObject payload = new JSONObject();
                JSONObject inputAudio = new JSONObject();
                inputAudio.put("encoding", "raw");    // 修改为raw格式
                inputAudio.put("sample_rate", 16000);
                inputAudio.put("channels", 1);
                inputAudio.put("bit_depth", 16);
                inputAudio.put("status", 2);
                inputAudio.put("seq", 9999999);
                inputAudio.put("frame_size", 0);
                payload.put("input_audio", inputAudio);
                frame.put("payload", payload);
                
                webSocket.send(frame.toString());
            } catch (Exception e) {
                LogUtils.e("结束处理失败", e);
            }
        }
    }
    
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        LogUtils.d("WebSocket 连接成功");
        LogUtils.d("协议: " + response.protocol());
        LogUtils.d("TLS 版本: " + (response.handshake() != null ? 
            response.handshake().tlsVersion() : "unknown"));
        sendBusinessParams();
    }
    
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            JSONObject response = new JSONObject(text);
            
            if (response.has("header")) {
                JSONObject header = response.getJSONObject("header");
                int code = header.getInt("code");
                String message = header.optString("message", "未知错误");
                int status = header.optInt("status", -1);
                
                if (code != 0) {
                    callback.onError(message);
                    return;
                }

                if (status == 0 && code == 0) {
                    LogUtils.d("开始接收音频数据...");
                    isBusinessReady = true;
                    callback.onReady();
                    return;
                }
            }

            if (response.has("payload")) {
                JSONObject payload = response.getJSONObject("payload");
                if (payload.has("result")) {
                    JSONObject result = payload.getJSONObject("result");
                    String encoding = result.optString("encoding", "");
                    LogUtils.d("接收到音频数据格式: " + encoding); // 检查返回的音频格式
                    
                    int status = result.optInt("status", -1);
                    String audioBase64 = result.optString("audio", "");
                    
                    if (!audioBase64.isEmpty()) {
                        byte[] audioData = android.util.Base64.decode(audioBase64, android.util.Base64.NO_WRAP);
                        LogUtils.d("接收到音频帧大小: " + audioData.length + " 字节");
                        
                        // 缓存音频帧
                        audioFrames.add(audioData);
                        
                        // 如果是结束帧，组合所有音频数据
                        if (status == 2) {
                            // 计算总大小
                            int totalSize = 0;
                            for (byte[] frame : audioFrames) {
                                totalSize += frame.length;
                            }
                            
                            // 组合音频数据
                            byte[] completeAudio = new byte[totalSize];
                            int offset = 0;
                            for (byte[] frame : audioFrames) {
                                System.arraycopy(frame, 0, completeAudio, offset, frame.length);
                                offset += frame.length;
                            }
                            
                            LogUtils.d("音频转换完成，总大小: " + completeAudio.length + " 字节");
                            
                            // 清空缓存
                            audioFrames.clear();
                            
                            // 返回完整的音频数据
                            callback.onSuccess(completeAudio);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("处理响应失败: " + e.getMessage());
            callback.onError("处理响应失败");
            audioFrames.clear(); // 发生错误时清空缓存
        }
    }
    
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        try {
            byte[] audioData = bytes.toByteArray();
            if (audioData != null && audioData.length > 0) {
                LogUtils.d("成功接收音频数据，大小: " + audioData.length + " 字节");
                callback.onSuccess(audioData);
            } else {
                String error = "接收到的音频数据为空";
                LogUtils.e(error);
                callback.onError(error);
            }
        } catch (Exception e) {
            String error = "处理接收到的音频数据时发生异常: " + e.getMessage();
            LogUtils.e(error);
            e.printStackTrace();
            callback.onError(error);
        }
    }
    
    private void handleMessage(String message) {
        // 移除旧的handleMessage方法，因为所有处理逻辑都已经移到onMessage方法中
    }
    
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
            isBusinessReady = false;
        }
    }
    
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        audioFrames.clear(); // 连接失败时清空缓存
        callback.onError("音频服务连接失败");
    }
    
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        LogUtils.d("========== WebSocket正在关闭 ==========");
        LogUtils.d(String.format("关闭码: %d, 原因: %s", code, reason));
    }
    
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        LogUtils.d("========== WebSocket已关闭 ==========");
        LogUtils.d(String.format("关闭码: %d, 原因: %s", code, reason));
    }

    // 添加重置方法
    public void reset() {
        audioFrames.clear(); // 重置时清空缓存
        isBusinessReady = false;
        currentSeq = 0;
        if (webSocket != null) {
            webSocket.close(1000, "Reset connection");
            webSocket = null;
        }
    }
} 