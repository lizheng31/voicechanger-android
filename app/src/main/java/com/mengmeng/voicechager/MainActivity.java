package com.mengmeng.voicechager;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.permissionx.guolindev.PermissionX;
import com.mengmeng.voicechager.R;
import com.mengmeng.voicechager.services.AudioRecordService;
import com.mengmeng.voicechager.utils.AudioPlayerManager;
import com.mengmeng.voicechager.utils.VoiceUploadManager;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.os.Environment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mengmeng.voicechager.adapters.AudioListAdapter;
import com.mengmeng.voicechager.models.AudioItem;
import android.widget.Toast;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.FileOutputStream;
import android.util.Log;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import com.mengmeng.voicechager.models.VoiceType;
import android.widget.AutoCompleteTextView;
import java.util.Collections;
import com.mengmeng.voicechager.utils.LogUtils;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;
import android.view.MotionEvent;

public class MainActivity extends AppCompatActivity {
    private MaterialButton recordButton;
    private MaterialButton convertButton;
    private TextView recordingStatusText;
    private Chronometer recordingTimer;

    private AudioRecordService audioRecordService;
    private AudioPlayerManager audioPlayerManager;
    private boolean isRecording = false;
    private VoiceUploadManager voiceUploadManager;
    private RecyclerView audioListView;
    private AudioListAdapter audioListAdapter;
    private AudioItem selectedAudioItem;
    private AutoCompleteTextView voiceTypeSpinner;
    private VoiceType selectedVoiceType = VoiceType.CHONGCHONG; // 默认音色

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initServices();
        initViews();
        setupListeners();
        requestPermissions();
        voiceUploadManager = new VoiceUploadManager();
        setupAudioList();
        setupVoiceTypeSpinner();
    }

    private void initServices() {
        audioRecordService = new AudioRecordService(this);
        audioPlayerManager = new AudioPlayerManager();
    }

    private void initViews() {
        recordButton = findViewById(R.id.recordButton);
        convertButton = findViewById(R.id.convertButton);
        convertButton.setText("变声");
        convertButton.setEnabled(false);  // 初始状态下禁用变声按钮
        recordingStatusText = findViewById(R.id.recordingStatusText);
        recordingTimer = findViewById(R.id.recordingTimer);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.settings) {
                // TODO: 打开设置界面
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        convertButton.setOnClickListener(v -> {
            handleVoiceConversion();
        });
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        PermissionX.init(this)
            .permissions(permissions)
            .request((allGranted, grantedList, deniedList) -> {
                if (!allGranted) {
                    Toast.makeText(this, "需要录音和存储权限才能使用此功能", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    createRequiredDirectories();
                }
            });
    }

    private void createRequiredDirectories() {
        try {
            File voicechagerDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voicechager");
            if (!voicechagerDir.exists() && !voicechagerDir.mkdirs()) {
                LogUtils.e("无法创建音频目录");
            }
        } catch (Exception e) {
            LogUtils.e("创建目录失败", e);
        }
    }

    private void startRecording() {
        try {
            audioRecordService.startRecording();
            isRecording = true;
            recordButton.setText("停止");
            recordingStatusText.setText("正在录音...");
            recordingTimer.setVisibility(View.VISIBLE);
            recordingTimer.setBase(android.os.SystemClock.elapsedRealtime());
            recordingTimer.start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "录音失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            audioRecordService.stopRecording();
            isRecording = false;
            recordButton.setText("录音");
            recordingStatusText.setText("录音完成");
            recordingTimer.stop();
            recordingTimer.setVisibility(View.GONE);
            
            // 确保录音文件存在
            String recordedFilePath = audioRecordService.getCurrentFilePath();
            if (recordedFilePath != null) {
                File recordedFile = new File(recordedFilePath);
                if (recordedFile.exists() && recordedFile.length() > 0) {
                    LogUtils.d("录音文件已保存: " + recordedFilePath);
                    loadAudioList();  // 刷新列表
                } else {
                    LogUtils.e("录音文件不存在或为空: " + recordedFilePath);
                    Toast.makeText(this, "录音保存失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                LogUtils.e("没有获取到录音文件路径");
                Toast.makeText(this, "录音保存失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LogUtils.e("停止录音失败", e);
            Toast.makeText(this, "停止录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVoiceConversion() {
        // 获取选中的音频文件
        AudioItem selectedItem = audioListAdapter.getSelectedItem();
        if (selectedItem == null) {
            Toast.makeText(this, "请选择要变声的音频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否已经是变声后的文件
        if (selectedItem.isConverted()) {
            Toast.makeText(this, "请选择原声文件进行变声", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取选中的变声类型
        String selectedText = voiceTypeSpinner.getText().toString();
        VoiceType selectedVoiceType = null;
        for (VoiceType type : VoiceType.values()) {
            if (type.toString().equals(selectedText)) {
                selectedVoiceType = type;
                break;
            }
        }
        
        if (selectedVoiceType == null) {
            Toast.makeText(this, "请选择变声类型", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否已存在相同变声类的文件
        String expectedFileName = "chongchong_" + selectedItem.getId() + "_" + selectedVoiceType.getCode();
        for (AudioItem item : audioListAdapter.getAudioItems()) {
            if (item.getName().startsWith(expectedFileName)) {
                Toast.makeText(this, "已存在相同变声类型的文件", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 开始变声处理
        convertButton.setEnabled(false);
        recordingStatusText.setText("正在变声处理...");
        
        // 调用变声处理
        uploadVoice(selectedItem.getPath());
    }

    private void uploadVoice(String filePath) {
        if (filePath != null) {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            recordingStatusText.setText("正在处理...");
            convertButton.setEnabled(false);
            
            voiceUploadManager.cloneVoice(
                audioFile,
                selectedVoiceType,
                new VoiceUploadManager.VoiceCallback() {
                    @Override
                    public void onSuccess(byte[] audioData) {
                        runOnUiThread(() -> {
                            // 存转换后的音频数据
                            String convertedFilePath = saveConvertedAudio(audioData);
                            if (convertedFilePath != null) {
                                recordingStatusText.setText("变声完成");
                                // 刷新音频列表
                                loadAudioList();
                            } else {
                                recordingStatusText.setText("保存失败");
                            }
                            convertButton.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            recordingStatusText.setText("变声失败: " + error);
                            convertButton.setEnabled(true);
                            Toast.makeText(MainActivity.this, 
                                "变声失败：" + error, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            );
        } else {
            Toast.makeText(this, "请先选择要转换的音频文件", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveConvertedAudio(byte[] audioData) {
        try {
            // 添加日志记录保存的音频大小
            LogUtils.d("保存转换后的音频大小: " + audioData.length + " 字节");
            
            File outputDir = new File(getExternalFilesDir(null), "converted");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String fileName = "converted_" + new SimpleDateFormat("yyyyMMdd_HHmmss", 
                Locale.getDefault()).format(new Date()) + ".mp3";
            File outputFile = new File(outputDir, fileName);

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(audioData);
            fos.close();

            // 验证保存的文件
            if (outputFile.exists()) {
                LogUtils.d("音频文件已保存: " + outputFile.getAbsolutePath() + 
                          ", 文件大小: " + outputFile.length() + " 字节");
            }

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            LogUtils.e("保存音频文件失败: " + e.getMessage());
            return null;
        }
    }

    private void setupAudioList() {
        audioListView = findViewById(R.id.audioList);
        audioListView.setLayoutManager(new LinearLayoutManager(this));
        
        // 添加点击空白处取消选择的功能
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> clearSelection());
        
        audioListAdapter = new AudioListAdapter();
        audioListAdapter.setAudioPlayerManager(audioPlayerManager);
        audioListView.setAdapter(audioListAdapter);
        
        // 防止 RecyclerView 拦截点击事件
        audioListView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // 检查点击是否在任何列表项上
                View child = audioListView.findChildViewUnder(event.getX(), event.getY());
                if (child == null) {
                    // 点击在空白处
                    clearSelection();
                    return true;
                }
            }
            return false;
        });
        
        audioListAdapter.setOnItemClickListener(new AudioListAdapter.OnItemClickListener() {
            @Override
            public void onPlayClick(AudioItem item, MaterialButton playButton) {
                try {
                    if (!audioPlayerManager.isPlaying() || 
                        !item.getPath().equals(audioPlayerManager.getCurrentAudioPath())) {
                        if (audioPlayerManager.isPaused() && 
                            item.getPath().equals(audioPlayerManager.getCurrentAudioPath())) {
                            audioPlayerManager.resumeAudio();
                            playButton.setIconResource(android.R.drawable.ic_media_pause);
                        } else {
                            audioPlayerManager.playAudio(item.getPath());
                            playButton.setIconResource(android.R.drawable.ic_media_pause);
                        }
                    } else {
                        audioPlayerManager.pauseAudio();
                        playButton.setIconResource(android.R.drawable.ic_media_play);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, 
                        "播放失败：" + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(AudioItem item) {
                File file = new File(item.getPath());
                if (file.delete()) {
                    if (selectedAudioItem != null && 
                        selectedAudioItem.getPath().equals(item.getPath())) {
                        clearSelection();
                    }
                    loadAudioList();
                }
            }

            @Override
            public void onItemSelected(AudioItem item) {
                try {
                    // 如果点击已选中的项目，则取消选择
                    if (item != null && selectedAudioItem != null && 
                        selectedAudioItem.getPath().equals(item.getPath())) {
                        clearSelection();
                    } else {
                        selectedAudioItem = item;
                        // 只有选中原声文件时才启用变声按钮
                        convertButton.setEnabled(item != null && !item.isConverted());
                        
                        // 如果选中的是变声后的文件，显示提示
                        if (item != null && item.isConverted()) {
                            recordingStatusText.setText("已选择变声后的文件");
                        } else if (item != null) {
                            recordingStatusText.setText("已选择原声文件");
                        } else {
                            recordingStatusText.setText("");
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e("选择项目时发生错误", e);
                    clearSelection();  // 发生错误时清除选择状态
                }
            }
        });

        loadAudioList();

        audioPlayerManager.setOnPlaybackCompleteListener(() -> {
            runOnUiThread(() -> {
                int position = audioListAdapter.getAudioItems().indexOf(selectedAudioItem);
                if (position != -1) {
                    RecyclerView.ViewHolder viewHolder = 
                        audioListView.findViewHolderForAdapterPosition(position);
                    if (viewHolder != null) {
                        MaterialButton playButton = 
                            viewHolder.itemView.findViewById(R.id.playButton);
                        playButton.setIconResource(android.R.drawable.ic_media_play);
                    }
                }
            });
        });
    }

    // 添加清除选择的辅助方法
    private void clearSelection() {
        try {
            selectedAudioItem = null;
            if (audioListAdapter != null) {
                audioListAdapter.setSelectedItem(null);
            }
            if (convertButton != null) {
                convertButton.setEnabled(false);
            }
            if (recordingStatusText != null) {
                recordingStatusText.setText("");
            }
        } catch (Exception e) {
            LogUtils.e("清除选择状态时发生错误", e);
        }
    }

    private void loadAudioList() {
        List<AudioItem> items = new ArrayList<>();
        
        // 加载录音文件目录
        File recordingsDir = new File(getExternalFilesDir(null), "recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
        
        // 加载转换后的文件目录
        File convertedDir = new File(getExternalFilesDir(null), "converted");
        if (!convertedDir.exists()) {
            convertedDir.mkdirs();
        }

        // 处理录音文件
        File[] recordingFiles = recordingsDir.listFiles();
        if (recordingFiles != null) {
            for (File file : recordingFiles) {
                if (file.getName().endsWith(".mp3")) {
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", 
                        Locale.getDefault()).format(new Date(file.lastModified()));
                    String displayName = formatRecordingFileName(file.getName());
                    items.add(new AudioItem(
                        file.getAbsolutePath(),
                        displayName,
                        file.getAbsolutePath(),
                        date,
                        false
                    ));
                }
            }
        }

        // 处理转换后的文件
        File[] convertedFiles = convertedDir.listFiles();
        if (convertedFiles != null) {
            for (File file : convertedFiles) {
                if (file.getName().startsWith("converted_") && file.getName().endsWith(".mp3")) {
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", 
                        Locale.getDefault()).format(new Date(file.lastModified()));
                    String displayName = formatConvertedFileName(file.getName());
                    String voiceType = extractVoiceType(file.getName());
                    items.add(new AudioItem(
                        file.getAbsolutePath(),
                        displayName,
                        file.getAbsolutePath(),
                        date,
                        true,
                        voiceType
                    ));
                }
            }
        }

        // 按时间倒序排序
        Collections.sort(items, (a, b) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date dateA = sdf.parse(a.getDate());
                Date dateB = sdf.parse(b.getDate());
                return dateB.compareTo(dateA);
            } catch (Exception e) {
                return 0;
            }
        });

        audioListAdapter.setAudioItems(items);
    }

    // 格式化原声录音文件名
    private String formatRecordingFileName(String fileName) {
        // 从 "record_20240101_123456.mp3" 格式转换为更友好的显示
        try {
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("_");
            if (parts.length >= 3) {
                // 提取日期时间部分
                String dateStr = parts[1];
                String timeStr = parts[2];
                
                // 格式化日期时间
                String formattedDateTime = String.format("%s-%s-%s %s:%s",
                    dateStr.substring(0, 4),
                    dateStr.substring(4, 6),
                    dateStr.substring(6, 8),
                    timeStr.substring(0, 2),
                    timeStr.substring(2, 4)
                );
                
                return String.format("原声录音 [MP3] %s", formattedDateTime);
            }
        } catch (Exception e) {
            LogUtils.e("格式化文件名失败", e);
        }
        return String.format("原声录音 [MP3] %s", fileName);
    }

    // 格式化变声后的文件名
    private String formatConvertedFileName(String fileName) {
        // 从 "converted_20240101_123456.mp3" 格式转换为更友好的显示
        try {
            String nameWithoutPrefix = fileName.replace("converted_", "");
            String nameWithoutExt = nameWithoutPrefix.substring(0, nameWithoutPrefix.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("_");
            if (parts.length >= 2) {
                // 提取日期时间部分
                String dateStr = parts[0];
                String timeStr = parts[1];
                
                // 格式化日期时间
                String formattedDateTime = String.format("%s-%s-%s %s:%s",
                    dateStr.substring(0, 4),
                    dateStr.substring(4, 6),
                    dateStr.substring(6, 8),
                    timeStr.substring(0, 2),
                    timeStr.substring(2, 4)
                );
                
                // 如果有音色信息，添加到显示名称中
                String voiceType = extractVoiceType(fileName);
                if (voiceType != null && !voiceType.isEmpty()) {
                    return String.format("变声音频 [%s] [MP3] %s", voiceType, formattedDateTime);
                }
                return String.format("变声音频 [MP3] %s", formattedDateTime);
            }
        } catch (Exception e) {
            LogUtils.e("格式化文件名失败", e);
        }
        return String.format("变声音频 [MP3] %s", fileName.replace("converted_", ""));
    }

    // 从文件名中提取音色类型
    private String extractVoiceType(String fileName) {
        for (VoiceType type : VoiceType.values()) {
            if (fileName.contains(type.getCode())) {
                return type.getDescription();
            }
        }
        return "";
    }

    private void setupVoiceTypeSpinner() {
        voiceTypeSpinner = findViewById(R.id.voiceTypeSpinner);
        ArrayAdapter<VoiceType> adapter = new ArrayAdapter<>(
            this,
            R.layout.item_voice_type,
            VoiceType.values()
        );
        voiceTypeSpinner.setAdapter(adapter);
        
        voiceTypeSpinner.setText(VoiceType.CHONGCHONG.toString(), false);
        
        voiceTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedVoiceType = VoiceType.values()[position];
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayerManager.release();
        if (isRecording) {
            audioRecordService.stopRecording();
        }
    }
}