package com.mengmeng.voicechager.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.mengmeng.voicechager.MainActivity;
import com.mengmeng.voicechager.R;
import com.mengmeng.voicechager.utils.AudioPlayerManager;
import com.mengmeng.voicechager.utils.LogUtils;

public class VoiceMonitorService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "VoiceMonitor";
    private static final String PREFS_NAME = "VoiceChangerPrefs";
    private static final String KEY_PLAYBACK_DELAY = "playback_delay";
    private static final float DEFAULT_DELAY = 1.0f;
    private static final long FIXED_DELAY = 500; // 0.5秒固定延迟
    private Handler handler;
    private AudioPlayerManager audioPlayerManager;
    private boolean isMonitoring = false;
    private boolean hasPlayed = false;  // 添加标记，避免重复播放

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        audioPlayerManager = new AudioPlayerManager();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        
        try {
            startForeground(NOTIFICATION_ID, createNotification());
        } catch (Exception e) {
            LogUtils.e("启动前台服务失败", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMonitoring();
        return START_STICKY;
    }

    private void startMonitoring() {
        if (isMonitoring) {
            return;
        }

        isMonitoring = true;
        LogUtils.d("开始监控微信语音状态");
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) {
                    return;
                }

                VoiceAccessibilityService service = VoiceAccessibilityService.getInstance();
                if (service == null) {
                    handler.postDelayed(this, 100);
                    return;
                }

                if (service.isButtonPressed()) {
                    String audioPath = VoiceAccessibilityService.getTargetAudioPath();
                    if (audioPath != null) {
                        audioPlayerManager.setOnPlaybackCompleteListener(() -> {
                            startMonitoring();
                        });
                        
                        audioPlayerManager.playAudio(audioPath);
                        
                        isMonitoring = false;
                        return;
                    }
                }

                handler.postDelayed(this, 100);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("语音监控服务运行中")
            .setContentText("等待进入微信语音模式")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMonitoring = false;
        if (audioPlayerManager != null) {
            audioPlayerManager.release();
            audioPlayerManager = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private float getPlaybackDelay() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getFloat(KEY_PLAYBACK_DELAY, DEFAULT_DELAY);
    }
} 