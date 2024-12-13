package com.mengmeng.voicechager.utils;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

public class LogUtils {
    private static final String TAG = "VoiceChanger";
    
    public static void init() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)
                .methodCount(2)
                .tag(TAG)
                .build();
        
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
    }
    
    public static void d(String message) {
        Logger.d(message);
    }
    
    public static void i(String message) {
        Logger.i(message);
    }
    
    public static void w(String message) {
        Logger.w(message);
    }
    
    public static void e(String message) {
        Logger.e(message);
    }
    
    public static void e(String message, Throwable throwable) {
        Logger.e(throwable, message);
    }
} 