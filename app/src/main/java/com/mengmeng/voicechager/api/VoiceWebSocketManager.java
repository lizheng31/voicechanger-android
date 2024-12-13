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

public class VoiceWebSocketManager extends WebSocketListener {
    private WebSocket webSocket;
    private final VoiceCallback callback;
    private boolean isBusinessReady = false;
    private final VoiceType voiceType;
    private int currentSeq = 0;
    private static final int MAX_FRAME_SIZE = 1024; // 1KB per frame as per API requirement
    
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
            callback.onError("Failed to generate auth url");
            return;
        }
        
        LogUtils.d("========== 开始WebSocket连接 ==========");
        LogUtils.d("连接URL: " + url);
        LogUtils.d("URL协议: " + url.substring(0, url.indexOf("://")));
        
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        webSocket = ApiClient.getClient().newWebSocket(request, this);
    }
    
    private void sendBusinessParams() {
        try {
            LogUtils.d("========== 开始发送业务参数 ==========");
            
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
            
            // result (必传)
            JSONObject result = new JSONObject();
            result.put("encoding", "raw");        // 修改为raw格式，与输入保持一致
            result.put("sample_rate", 16000);
            result.put("channels", 1);
            result.put("bit_depth", 16);
            result.put("frame_size", 1024);
            xvc.put("result", result);
            
            parameter.put("xvc", xvc);
            frame.put("parameter", parameter);
            
            // payload (必传)
            JSONObject payload = new JSONObject();
            JSONObject inputAudio = new JSONObject();
            inputAudio.put("encoding", "raw");    // 修改为raw格式，因为我们发送的是PCM原始数据
            inputAudio.put("sample_rate", 16000);
            inputAudio.put("channels", 1);
            inputAudio.put("bit_depth", 16);
            inputAudio.put("status", 0);
            inputAudio.put("seq", 0);
            inputAudio.put("frame_size", 1024);
            payload.put("input_audio", inputAudio);
            frame.put("payload", payload);
            
            String message = frame.toString();
            LogUtils.d("业务参数: " + message);
            
            boolean sent = webSocket.send(message);
            if (!sent) {
                String error = "业务参数发送失败";
                LogUtils.e(error);
                callback.onError(error);
                return;
            }
            LogUtils.d("业务参数发送完成");
        } catch (Exception e) {
            String error = "发送业务参数失败: " + e.getMessage();
            LogUtils.e(error, e);
            callback.onError(error);
        }
    }
    
    public void sendAudio(byte[] audioData) {
        if (webSocket != null && isBusinessReady) {
            try {
                LogUtils.d("========== 开始发送音频数据 ==========");
                LogUtils.d("音频数据总大小: " + audioData.length + " 字节");

                if (audioData == null || audioData.length == 0) {
                    String error = "音频数据为空";
                    LogUtils.e(error);
                    callback.onError(error);
                    return;
                }

                int offset = 0;
                int frameCount = 0;
                while (offset < audioData.length) {
                    int frameSize = Math.min(MAX_FRAME_SIZE, audioData.length - offset);
                    byte[] frameData = new byte[frameSize];
                    System.arraycopy(audioData, offset, frameData, 0, frameSize);
                    
                    if (frameData.length > 10485760) {
                        String error = "音频数据超过大小限制";
                        LogUtils.e(error);
                        callback.onError(error);
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
                    inputAudio.put("encoding", "raw");    // 修改为raw格式
                    inputAudio.put("sample_rate", 16000);
                    inputAudio.put("channels", 1);
                    inputAudio.put("bit_depth", 16);
                    inputAudio.put("frame_size", frameSize);
                    inputAudio.put("status", offset + frameSize >= audioData.length ? 2 : 1);
                    inputAudio.put("seq", frameCount);
                    inputAudio.put("audio", base64Audio);
                    
                    payload.put("input_audio", inputAudio);
                    frame.put("payload", payload);
                    
                    String message = frame.toString();
                    LogUtils.d(String.format("发送第 %d 帧数据:", frameCount));
                    LogUtils.d("- 状态: " + (offset + frameSize >= audioData.length ? "结束" : "继续"));
                    LogUtils.d("- 帧大小: " + frameSize + " 字节");
                    
                    boolean sent = webSocket.send(message);
                    if (!sent) {
                        String error = "音频数据发送失败";
                        LogUtils.e(error);
                        callback.onError(error);
                        return;
                    }
                    
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        // 忽略中断异常
                    }
                    
                    offset += frameSize;
                    frameCount++;
                }
                
                LogUtils.d("音频数据发送完成，共发送 " + frameCount + " 帧");
            } catch (Exception e) {
                String error = "发送音频数据时发生异常: " + e.getMessage();
                LogUtils.e(error);
                e.printStackTrace();
                callback.onError(error);
            }
        } else {
            String error = "WebSocket未就绪，无法发送音频数据";
            if (webSocket == null) {
                error += " (webSocket为null)";
            }
            if (!isBusinessReady) {
                error += " (业务未就绪)";
            }
            LogUtils.e(error);
            callback.onError(error);
        }
    }
    
    public void sendEndFrame() {
        if (webSocket != null && isBusinessReady) {
            try {
                LogUtils.d("========== 开始发送结束帧 ==========");
                
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
                
                String message = frame.toString();
                LogUtils.d("发送结束帧: " + message);
                webSocket.send(message);
                LogUtils.d("结束帧发送完成");
            } catch (Exception e) {
                String error = "发送结束帧失败";
                LogUtils.e(error, e);
            }
        }
    }
    
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        LogUtils.d("========== WebSocket连接成功 ==========");
        LogUtils.d("响应码: " + response.code());
        LogUtils.d("响应头: " + response.headers());
        sendBusinessParams();
    }
    
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        LogUtils.d("========== 收到文本消息 ==========");
        LogUtils.d("原始消息内容: " + text);
        try {
            JSONObject response = new JSONObject(text);
            
            // 处理header部分
            if (response.has("header")) {
                JSONObject header = response.getJSONObject("header");
                int code = header.getInt("code");
                String message = header.optString("message", "未知错误");
                String sid = header.optString("sid", "");
                int status = header.optInt("status", -1);
                
                LogUtils.d("响应头息详情:");
                LogUtils.d("- 错误码: " + code);
                LogUtils.d("- 错误消息: " + message);
                LogUtils.d("- 会话ID: " + sid);
                LogUtils.d("- 状态: " + status);

                if (code != 0) {
                    String error = String.format("业务错误 - 错误码: %d, 错误信息: %s, 会话ID: %s", 
                        code, message, sid);
                    LogUtils.e(error);
                    callback.onError(error);
                    return;
                }

                // 如果是初始化响应且成功，标记业务就绪
                if (status == 0 && code == 0) {
                    LogUtils.d("业务初始化成功，准备发送音频数据");
                    isBusinessReady = true;
                    callback.onReady();
                    return;
                }
            }

            // 处理payload部分
            if (response.has("payload")) {
                JSONObject payload = response.getJSONObject("payload");
                LogUtils.d("Payload内容: " + payload.toString());
                
                if (payload.has("result")) {
                    JSONObject result = payload.getJSONObject("result");
                    
                    // 记录音频格式信息
                    String encoding = result.optString("encoding", "");
                    int sampleRate = result.optInt("sample_rate", 0);
                    int channels = result.optInt("channels", 0);
                    int bitDepth = result.optInt("bit_depth", 0);
                    int status = result.optInt("status", -1);
                    int seq = result.optInt("seq", -1);
                    String audioBase64 = result.optString("audio", "");
                    int frameSize = result.optInt("frame_size", 0);
                    
                    LogUtils.d("音频信息详情:");
                    LogUtils.d("- 编码格式: " + encoding);
                    LogUtils.d("- 采样率: " + sampleRate);
                    LogUtils.d("- 声道数: " + channels);
                    LogUtils.d("- 位深: " + bitDepth);
                    LogUtils.d("- 状态: " + status);
                    LogUtils.d("- 序号: " + seq);
                    LogUtils.d("- 帧大小: " + frameSize);
                    
                    // 如果包含音频数据，进行处理
                    if (!audioBase64.isEmpty()) {
                        byte[] audioData = android.util.Base64.decode(audioBase64, android.util.Base64.NO_WRAP);
                        callback.onSuccess(audioData);
                    }
                    
                    // 根据状态处理业务逻辑
                    switch (status) {
                        case 0:
                            LogUtils.d("音频处理开始");
                            break;
                        case 1:
                            LogUtils.d("音频处理中");
                            break;
                        case 2:
                            LogUtils.d("音频处理完成");
                            close();
                            break;
                        default:
                            LogUtils.e("未知状态: " + status);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            String error = "解析响应失败: " + e.getMessage();
            LogUtils.e(error);
            LogUtils.e("原始消息: " + text);
            e.printStackTrace();
            callback.onError(error);
        }
    }
    
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        LogUtils.d("========== 收到二进制消息 ==========");
        LogUtils.d("数据大小: " + bytes.size() + " 字节");
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
        String error = "WebSocket连接失败\n";
        if (response != null) {
            error += String.format("响应码: %d\n", response.code());
            error += String.format("响应信息: %s\n", response.message());
            error += String.format("响应头: %s\n", response.headers());
        }
        error += String.format("异常信息: %s", t.getMessage());
        LogUtils.e("========== WebSocket错误 ==========");
        LogUtils.e(error);
        t.printStackTrace();

        callback.onError(error);
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
        isBusinessReady = false;
        currentSeq = 0;
        if (webSocket != null) {
            webSocket.close(1000, "Reset connection");
            webSocket = null;
        }
    }
} 