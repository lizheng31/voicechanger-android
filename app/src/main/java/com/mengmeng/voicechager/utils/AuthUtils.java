package com.mengmeng.voicechager.utils;

import android.util.Base64;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthUtils {
    private static final String HOST = "cn-huadong-1.xf-yun.com";
    private static final String PATH = "/v1/private/s5e668773";
    
    public static String getAuthUrl(String apiKey, String apiSecret) {
        try {
            String date = getGMTDate();
            String signature = generateSignature(apiSecret, date);
            String authorization = generateAuthorization(apiKey, signature);
            
            return String.format("wss://%s%s?authorization=%s&date=%s&host=%s",
                HOST,
                PATH,
                authorization,
                date,
                HOST
            );
        } catch (Exception e) {
            LogUtils.e("Generate auth url failed", e);
            return null;
        }
    }
    
    private static String getGMTDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }
    
    private static String generateSignature(String apiSecret, String date) throws Exception {
        String signatureOrigin = String.format("host: %s\ndate: %s\nGET %s HTTP/1.1", HOST, date, PATH);
        
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
        byte[] signatureSha = mac.doFinal(signatureOrigin.getBytes());
        return Base64.encodeToString(signatureSha, Base64.NO_WRAP);
    }
    
    private static String generateAuthorization(String apiKey, String signature) {
        String authorizationOrigin = String.format(
            "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
            apiKey,
            signature
        );
        return Base64.encodeToString(authorizationOrigin.getBytes(), Base64.NO_WRAP);
    }
} 