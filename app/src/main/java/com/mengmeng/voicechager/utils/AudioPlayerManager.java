package com.mengmeng.voicechager.utils;

import android.media.MediaPlayer;
import java.io.IOException;

public class AudioPlayerManager {
    private MediaPlayer mediaPlayer;
    private String currentAudioPath;
    private boolean isPaused = false;

    public String getCurrentAudioPath() {
        return currentAudioPath;
    }

    public void playAudio(String audioPath) throws IOException {
        if (mediaPlayer != null) {
            stopAudio();
        }

        currentAudioPath = audioPath;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(audioPath);
        mediaPlayer.prepare();
        mediaPlayer.start();
        isPaused = false;
        
        mediaPlayer.setOnCompletionListener(mp -> {
            isPaused = false;
            if (onPlaybackCompleteListener != null) {
                onPlaybackCompleteListener.onComplete();
            }
        });
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPaused = false;
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public interface OnPlaybackCompleteListener {
        void onComplete();
    }

    private OnPlaybackCompleteListener onPlaybackCompleteListener;

    public void setOnPlaybackCompleteListener(OnPlaybackCompleteListener listener) {
        this.onPlaybackCompleteListener = listener;
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            currentAudioPath = null;
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
} 