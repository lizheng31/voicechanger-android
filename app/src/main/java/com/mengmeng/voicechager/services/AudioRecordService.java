package com.mengmeng.voicechager.services;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.mengmeng.voicechager.utils.LogUtils;

public class AudioRecordService {
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentFilePath;
    private final Context context;

    public AudioRecordService(Context context) {
        this.context = context;
    }

    public void startRecording() throws IOException {
        // 确保录音目录存在
        File recordDir = new File(context.getExternalFilesDir(null), "recordings");
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        // 创建录音文件
        File recordFile = new File(recordDir, 
            "record_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".mp3"
        );

        LogUtils.d("开始录音: " + recordFile.getAbsolutePath());

        try {
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
            LogUtils.d("录音开始成功");
        } catch (Exception e) {
            LogUtils.e("录音失败", e);
            throw e;
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            LogUtils.d("录音结束: " + currentFilePath);
        } catch (Exception e) {
            LogUtils.e("停止录音失败", e);
        }
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public boolean isRecording() {
        return isRecording;
    }
} 