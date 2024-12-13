package com.mengmeng.voicechager;

import android.app.Application;
import com.mengmeng.voicechager.utils.LogUtils;

public class VoiceChangerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.init();
    }
} 