package com.mengmeng.voicechager.utils;

import android.media.MediaPlayer;
import java.io.IOException;

public class AudioPlayerManager {
    private MediaPlayer mediaPlayer;
    private String currentAudioPath;

    public void playAudio(String audioPath) throws IOException {
        if (mediaPlayer != null) {
            stopAudio();
        }

        currentAudioPath = audioPath;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(audioPath);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
} 