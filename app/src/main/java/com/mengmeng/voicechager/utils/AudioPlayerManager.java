package com.mengmeng.voicechager.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.IOException;

public class AudioPlayerManager {
    private MediaPlayer mediaPlayer;
    private String currentAudioPath;
    private boolean isPaused = false;
    private boolean isInitialized = false;

    public interface OnPlaybackCompleteListener {
        void onComplete();
    }

    private OnPlaybackCompleteListener onPlaybackCompleteListener;

    public void setOnPlaybackCompleteListener(OnPlaybackCompleteListener listener) {
        this.onPlaybackCompleteListener = listener;
    }

    public String getCurrentAudioPath() {
        return currentAudioPath;
    }

    public void playAudio(String audioPath) throws IOException {
        LogUtils.d("开始播放音频: " + audioPath);
        
        // 如果正在播放，先停止
        if (isPlaying()) {
            stopAudio();
        }

        try {
            currentAudioPath = audioPath;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(audioPath);
            
            // 添加错误处理
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                LogUtils.e("音频播放错误: what=" + what + ", extra=" + extra);
                String errorMsg;
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        errorMsg = "服务器错误";
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        errorMsg = "未知错误";
                        break;
                    default:
                        errorMsg = "播放错误: " + what;
                }
                LogUtils.e(errorMsg + ", extra=" + extra);
                isPaused = false;
                isInitialized = false;
                releaseMediaPlayer();
                return false;
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    mp.start();
                    isInitialized = true;
                    isPaused = false;
                    LogUtils.d("音频开始播放");
                } catch (IllegalStateException e) {
                    LogUtils.e("开始播放失败", e);
                    releaseMediaPlayer();
                }
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                try {
                    isPaused = false;
                    isInitialized = false;
                    if (onPlaybackCompleteListener != null) {
                        onPlaybackCompleteListener.onComplete();
                    }
                    LogUtils.d("音频播放完成");
                    releaseMediaPlayer();  // 播放完成后释放资源
                } catch (Exception e) {
                    LogUtils.e("播放完成处理失败", e);
                }
            });

            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            LogUtils.e("播放音频失败", e);
            releaseMediaPlayer();
            throw e;
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && isInitialized && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                isPaused = true;
                LogUtils.d("音频已暂停");
            } catch (IllegalStateException e) {
                LogUtils.e("暂停音频失败", e);
            }
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && isInitialized && isPaused) {
            try {
                mediaPlayer.start();
                isPaused = false;
                LogUtils.d("音频继续播放");
            } catch (IllegalStateException e) {
                LogUtils.e("继续播放失败", e);
            }
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (isInitialized) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                LogUtils.e("停止音频失败", e);
            } finally {
                releaseMediaPlayer();
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                LogUtils.e("释放MediaPlayer资源失败", e);
            } finally {
                mediaPlayer = null;
                currentAudioPath = null;
                isPaused = false;
                isInitialized = false;
                LogUtils.d("释放MediaPlayer资源");
            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && isInitialized && mediaPlayer.isPlaying();
    }

    // 在Activity/Fragment销毁时调用
    public void release() {
        stopAudio();
    }
} 