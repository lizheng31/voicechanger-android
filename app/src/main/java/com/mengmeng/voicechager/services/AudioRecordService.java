package com.mengmeng.voicechager.services;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecordService {
    private final Context context;
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private boolean isRecording = false;

    public AudioRecordService(Context context) {
        this.context = context;
    }

    public void startRecording() throws IOException {
        if (isRecording) {
            return;
        }

        File recordFile = new File(
            getRecordingsDirectory(),
            "recording_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".mp3"
        );

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(16000);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setAudioEncodingBitRate(32000);
        mediaRecorder.setOutputFile(recordFile.getAbsolutePath());
        mediaRecorder.prepare();
        mediaRecorder.start();

        currentFilePath = recordFile.getAbsolutePath();
        isRecording = true;
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaRecorder = null;
        isRecording = false;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    private File getRecordingsDirectory() {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
} 