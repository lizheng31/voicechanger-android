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
    }

    private void initServices() {
        audioRecordService = new AudioRecordService(this);
        audioPlayerManager = new AudioPlayerManager();
        audioPlayerManager.setOnPlaybackCompleteListener(() -> {
            runOnUiThread(() -> {
                convertButton.setText("变声");
            });
        });
    }

    private void initViews() {
        recordButton = findViewById(R.id.recordButton);
        convertButton = findViewById(R.id.convertButton);
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
            if (selectedAudioItem != null) {
                uploadVoice(selectedAudioItem.getPath());
            }
        });
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        PermissionX.init(this)
            .permissions(permissions)
            .request((allGranted, grantedList, deniedList) -> {
                if (!allGranted) {
                    Toast.makeText(this, "需要录音和存储权限才能使用此功能", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
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
        loadAudioList();
    }

    private void uploadVoice(String filePath) {
        if (filePath != null) {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            recordingStatusText.setText("正在处理...");
            voiceUploadManager.cloneVoice(
                new File(audioRecordService.getCurrentFilePath()),
                new VoiceUploadManager.VoiceCallback() {
                    @Override
                    public void onSuccess(byte[] audioData) {
                        runOnUiThread(() -> {
                            // 保存转换后的音频数据
                            String convertedFilePath = saveConvertedAudio(audioData);
                            if (convertedFilePath != null) {
                                recordingStatusText.setText("变声完成");
                                loadAudioList();
                            } else {
                                recordingStatusText.setText("保存失败");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            recordingStatusText.setText("变声失败: " + error);
                        });
                    }
                }
            );
        }
    }

    private String saveConvertedAudio(byte[] audioData) {
        try {
            File outputDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Converted");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String fileName = "converted_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".wav";
            File outputFile = new File(outputDir, fileName);

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(audioData);
            fos.close();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("MainActivity", "Save converted audio failed", e);
            return null;
        }
    }

    private void setupAudioList() {
        audioListView = findViewById(R.id.audioList);
        audioListView.setLayoutManager(new LinearLayoutManager(this));
        
        audioListAdapter = new AudioListAdapter();
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
        File recordingsDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
        if (recordingsDir.exists()) {
            File[] files = recordingsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(new Date(file.lastModified()));
                    items.add(new AudioItem(
                        file.getAbsolutePath(),
                        name,
                        file.getAbsolutePath(),
                        date,
                        false
                    ));
                }
            }
        }
        audioListAdapter.setAudioItems(items);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayerManager.stopAudio();
        if (isRecording) {
            audioRecordService.stopRecording();
        }
    }
}