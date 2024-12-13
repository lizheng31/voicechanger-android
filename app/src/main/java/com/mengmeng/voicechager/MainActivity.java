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
        audioRecordService.stopRecording();
        isRecording = false;
        recordButton.setText("录音");
        recordingStatusText.setText("录音完成");
        recordingTimer.stop();
        recordingTimer.setVisibility(View.GONE);
        LogUtils.d("录音完成，刷新列表");
        loadAudioList();
    }

    private void handleVoiceConversion() {
        // 获取中的音频文件
        AudioItem selectedItem = audioListAdapter.getSelectedItem();
        if (selectedItem == null) {
            Toast.makeText(this, "请���选择要变声的音频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否已经是变声后的文件
        if (selectedItem.isConverted()) {
            Toast.makeText(this, "该文件已经是变声后的文件", Toast.LENGTH_SHORT).show();
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

        // 检查是否已存在相同变声类型的文件
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
            // 创建转换后的音频文件目录
            File outputDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voicechager");
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                LogUtils.e("创建变声文件目录失败: " + outputDir.getAbsolutePath());
            }

            if (!outputDir.canWrite()) {
                throw new IOException("无法写入变声文件目录");
            }

            // 生成文件名，包含时间戳和音色信息
            String fileName = String.format("converted_%s_%s.wav",
                selectedVoiceType.getCode(),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
            
            File outputFile = new File(outputDir, fileName);

            // 写入WAV文件头
            FileOutputStream fos = new FileOutputStream(outputFile);
            
            // WAV文件参数
            final int SAMPLE_RATE = 24000;  // 采样率
            final int CHANNELS = 1;         // 单声道
            final int BITS_PER_SAMPLE = 16; // 位深度
            final int BYTE_RATE = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8);
            final int BLOCK_ALIGN = CHANNELS * (BITS_PER_SAMPLE / 8);
            
            // RIFF header
            writeString(fos, "RIFF"); // ChunkID
            writeInt(fos, 36 + audioData.length); // ChunkSize
            writeString(fos, "WAVE"); // Format
            
            // fmt subchunk
            writeString(fos, "fmt "); // Subchunk1ID
            writeInt(fos, 16); // Subchunk1Size
            writeShort(fos, (short) 1); // AudioFormat (1 = PCM)
            writeShort(fos, (short) CHANNELS); // NumChannels
            writeInt(fos, SAMPLE_RATE); // SampleRate
            writeInt(fos, BYTE_RATE); // ByteRate = SampleRate * NumChannels * (BitsPerSample / 8)
            writeShort(fos, (short) BLOCK_ALIGN); // BlockAlign = NumChannels * (BitsPerSample / 8)
            writeShort(fos, (short) BITS_PER_SAMPLE); // BitsPerSample
            
            // data subchunk
            writeString(fos, "data"); // Subchunk2ID
            writeInt(fos, audioData.length); // Subchunk2Size
            
            // 写入音频数据
            fos.write(audioData);
            fos.close();

            LogUtils.d("保存音频文件: " + outputFile.getAbsolutePath() + 
                ", 小: " + audioData.length + 
                ", 采样率: " + SAMPLE_RATE);

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            LogUtils.e("保存音频文件失败", e);
            return null;
        }
    }

    // 修改写入方法，确保按小端序写入
    private void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }

    private void writeShort(FileOutputStream fos, short value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }

    private void writeString(FileOutputStream fos, String value) throws IOException {
        fos.write(value.getBytes());
    }

    private void setupAudioList() {
        audioListView = findViewById(R.id.audioList);
        audioListView.setLayoutManager(new LinearLayoutManager(this));
        
        audioListAdapter = new AudioListAdapter();
        audioListAdapter.setAudioPlayerManager(audioPlayerManager);
        audioListView.setAdapter(audioListAdapter);
        
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
                        selectedAudioItem = null;
                        convertButton.setEnabled(false);
                    }
                    loadAudioList();
                }
            }

            @Override
            public void onItemSelected(AudioItem item) {
                selectedAudioItem = item;
                convertButton.setEnabled(true);
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

    private void loadAudioList() {
        List<AudioItem> items = new ArrayList<>();
        
        // 加载音频文件目录
        File audioDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voicechager");
        LogUtils.d("加载音频目录: " + audioDir.getAbsolutePath());
        LogUtils.d("音频目录是否存在: " + audioDir.exists());
        if (audioDir.exists()) {
            File[] audioFiles = audioDir.listFiles();
            LogUtils.d("音频文件数量: " + (audioFiles != null ? audioFiles.length : 0));
            if (audioFiles != null) {
                for (File file : audioFiles) {
                    LogUtils.d("检查文件: " + file.getName() + ", 大小: " + file.length());
                    if (file.getName().startsWith("converted_")) {
                        // 处理变声文件
                        String name = file.getName();
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(new Date(file.lastModified()));
                        
                        String voiceType = null;
                        String[] parts = name.split("_");
                        if (parts.length > 1) {
                            voiceType = getVoiceTypeDisplayName(parts[1]);
                        }
                        
                        LogUtils.d("添加变声文件: " + name + ", 音色: " + voiceType);
                        items.add(new AudioItem(
                            file.getAbsolutePath(),
                            name.replace("converted_", "变声: "),
                            file.getAbsolutePath(),
                            date,
                            true,
                            voiceType
                        ));
                    } else if (file.getName().endsWith(".mp3")) {
                        // 处理录音文件
                        String name = file.getName();
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(new Date(file.lastModified()));
                        LogUtils.d("添加录音文件: " + name);
                        items.add(new AudioItem(
                            file.getAbsolutePath(),
                            "原声: " + name,
                            file.getAbsolutePath(),
                            date,
                            false
                        ));
                    }
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
        
        LogUtils.d("加载音频列表: " + items.size() + " 个文件");
        for (AudioItem item : items) {
            LogUtils.d("音频文件: " + item.getPath());
        }
        
        audioListAdapter.setAudioItems(items);
    }

    // 获取变声类型的显示名称
    private String getVoiceTypeDisplayName(String code) {
        for (VoiceType type : VoiceType.values()) {
            if (type.getCode().equals(code)) {
                return type.getDescription();
            }
        }
        return code;
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