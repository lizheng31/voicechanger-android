package com.mengmeng.voicechager.api;

import com.mengmeng.voicechager.utils.AuthUtils;
import com.mengmeng.voicechager.utils.LogUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONObject;

public class VoiceWebSocketManager {
    private WebSocket webSocket;
    private final VoiceCallback callback;
    private boolean isBusinessReady = false;
    
    public interface VoiceCallback {
        void onSuccess(byte[] audioData);
        void onError(String error);
        void onReady();
    }
    
    public VoiceWebSocketManager(VoiceCallback callback) {
        this.callback = callback;
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
        
        LogUtils.d("Connecting to WebSocket: " + url);
        
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        webSocket = ApiClient.getClient().newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                LogUtils.d("WebSocket Connected");
                sendBusinessParams();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                LogUtils.d("Received text message: " + text);
                handleMessage(text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                LogUtils.d("Received binary message: " + bytes.size() + " bytes");
                callback.onSuccess(bytes.toByteArray());
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                String error = "WebSocket failure: " + t.getMessage();
                LogUtils.e(error, t);
                callback.onError(error);
            }
        });
    }
    
    private void sendBusinessParams() {
        try {
            JSONObject common = new JSONObject();
            common.put("app_id", ApiClient.getAppId());
            
            JSONObject business = new JSONObject();
            business.put("aue", "raw");
            business.put("auf", "audio/L16;rate=16000");
            business.put("vcn", "xiaoyan");
            business.put("speed", 50);
            business.put("volume", 50);
            business.put("pitch", 50);
            business.put("tte", "UTF8");
            
            JSONObject data = new JSONObject();
            data.put("status", 0);
            data.put("encoding", "raw");
            data.put("compress", "raw");
            data.put("format", "audio/L16;rate=16000");
            
            JSONObject frame = new JSONObject();
            frame.put("common", common);
            frame.put("business", business);
            frame.put("data", data);
            
            String message = frame.toString();
            LogUtils.d("Sending business params: " + message);
            webSocket.send(message);
        } catch (Exception e) {
            String error = "Failed to send business params";
            LogUtils.e(error, e);
            callback.onError(error);
        }
    }
    
    public void sendAudio(byte[] audioData) {
        if (webSocket != null && isBusinessReady) {
            try {
                JSONObject frame = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("status", 1);
                data.put("data", android.util.Base64.encodeToString(audioData, android.util.Base64.NO_WRAP));
                frame.put("data", data);
                
                String message = frame.toString();
                LogUtils.d("Sending audio frame: " + audioData.length + " bytes");
                webSocket.send(message);
            } catch (Exception e) {
                String error = "Failed to send audio data";
                LogUtils.e(error, e);
                callback.onError(error);
            }
        } else {
            LogUtils.w("WebSocket not ready for audio");
        }
    }
    
    public void sendEndFrame() {
        if (webSocket != null && isBusinessReady) {
            try {
                JSONObject frame = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("status", 2);
                frame.put("data", data);
                
                String message = frame.toString();
                LogUtils.d("Sending end frame");
                webSocket.send(message);
            } catch (Exception e) {
                String error = "Failed to send end frame";
                LogUtils.e(error, e);
            }
        }
    }
    
    private void handleMessage(String message) {
        try {
            JSONObject response = new JSONObject(message);
            
            if (response.has("code")) {
                int code = response.getInt("code");
                if (code != 0) {
                    String error = response.optString("message", "Unknown error");
                    LogUtils.e("Business error: " + error);
                    callback.onError(error);
                    return;
                }
            }
            
            if (response.has("data")) {
                JSONObject data = response.getJSONObject("data");
                int status = data.getInt("status");
                
                switch (status) {
                    case 0:
                        isBusinessReady = true;
                        callback.onReady();
                        break;
                    case 1:
                        // 音频数据会通过二进制消息返回
                        break;
                    case 2:
                        close();
                        break;
                }
            }
        } catch (Exception e) {
            String error = "Failed to parse response";
            LogUtils.e(error, e);
        }
    }
    
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
            isBusinessReady = false;
        }
    }
} 