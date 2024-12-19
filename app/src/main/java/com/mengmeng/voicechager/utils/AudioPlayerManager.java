package com.mengmeng.voicechager.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.IOException;

public class AudioPlayerManager {
    private MediaPlayer mediaPlayer;
    private String currentAudioPath;
    private boolean isPlaying = false;

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

    public void playAudio(String audioPath) {
        LogUtils.d("开始播放音频: " + audioPath);
        
        // 如果已经在播放，先停止
        if (isPlaying) {
            stopPlaying();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(audioPath);
            currentAudioPath = audioPath;
            
            // 设置音频完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                LogUtils.d("音频播放完成");
                isPlaying = false;
                releaseMediaPlayer();
            });

            // 设置错误监听器
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                LogUtils.e("音频播放错误: " + what + ", " + extra);
                isPlaying = false;
                releaseMediaPlayer();
                return true;
            });

            // 同步准备并立即播放
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            LogUtils.d("音频开始播放");
        } catch (Exception e) {
            LogUtils.e("播放音频失败", e);
            isPlaying = false;
            releaseMediaPlayer();
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.pause();
                LogUtils.d("音频已暂停");
            } catch (Exception e) {
                LogUtils.e("暂停音频失败", e);
            }
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.start();
                LogUtils.d("音频继续播放");
            } catch (Exception e) {
                LogUtils.e("继续播放失败", e);
            }
        }
    }

    public void stopPlaying() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
                LogUtils.d("停止播放音频");
            } catch (Exception e) {
                LogUtils.e("停止播放音频失败", e);
            }
            isPlaying = false;
            releaseMediaPlayer();
        }
    }

    public void release() {
        stopPlaying();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                LogUtils.d("释放MediaPlayer资源");
            } catch (Exception e) {
                LogUtils.e("释放MediaPlayer资源失败", e);
            }
            mediaPlayer = null;
            currentAudioPath = null;
        }
    }

    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isPaused() {
        return false;
    }
} 