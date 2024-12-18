package com.mengmeng.voicechager.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.mengmeng.voicechager.R;
import com.mengmeng.voicechager.utils.AudioPlayerManager;
import com.mengmeng.voicechager.utils.LogUtils;

public class VoiceMonitorService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "VoiceMonitor";
    private Handler handler;
    private AudioPlayerManager audioPlayerManager;
    private boolean isMonitoring = false;

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
        if (!isMonitoring) {
            startMonitoring();
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        isMonitoring = true;
        LogUtils.d("开始监控微信语音状态");
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) {
                    LogUtils.d("监控已停止");
                    return;
                }

                VoiceAccessibilityService service = VoiceAccessibilityService.getInstance();
                if (service == null) {
                    LogUtils.d("无障碍服务未运行");
                    handler.postDelayed(this, 100);
                    return;
                }

                if (service.isInWeChatVoiceMode()) {
                    LogUtils.d("检测到微信语音模式");
                    String audioPath = VoiceAccessibilityService.getTargetAudioPath();
                    if (audioPath != null) {
                        LogUtils.d("准备播放音频: " + audioPath);
                        handler.postDelayed(() -> {
                            try {
                                audioPlayerManager.playAudio(audioPath);
                                LogUtils.d("开始播放音频");
                            } catch (Exception e) {
                                LogUtils.e("播放音频失败", e);
                            }
                        }, 800);
                        
                        isMonitoring = false;
                        stopSelf();
                        return;
                    } else {
                        LogUtils.d("未设置目标音频文件");
                    }
                } else {
                    LogUtils.d("等待进入微信语音模式");
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
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 