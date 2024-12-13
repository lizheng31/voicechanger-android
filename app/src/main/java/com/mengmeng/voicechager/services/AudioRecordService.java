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

        File directory = getRecordingsDirectory();
        LogUtils.d("录音目录: " + directory.getAbsolutePath());
        if (!directory.canWrite()) {
            LogUtils.e("录音目录无写入权限");
            throw new IOException("无法写入录音目录");
        }

        File recordFile = new File(
            directory,
            "recording_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
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
            LogUtils.d("录音停止成功: " + currentFilePath);
            File recordFile = new File(currentFilePath);
            if (recordFile.exists()) {
                LogUtils.d("录音文件已保存，大小: " + recordFile.length() + " bytes");
            } else {
                LogUtils.e("录音文件未找到");
            }
        } catch (Exception e) {
            LogUtils.e("停止录音失败", e);
            e.printStackTrace();
        }
        mediaRecorder = null;
        isRecording = false;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    private File getRecordingsDirectory() {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voicechager");
        LogUtils.d("创建录音目录: " + directory.getAbsolutePath());
        if (!directory.exists() && !directory.mkdirs()) {
            LogUtils.e("创建录音目录失败");
        }
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                LogUtils.d("录音目录中的文件数量: " + files.length);
            }
        }
        return directory;
    }
} 