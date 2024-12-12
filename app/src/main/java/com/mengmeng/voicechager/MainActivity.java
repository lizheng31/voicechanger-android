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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private MaterialButton recordButton;
    private MaterialButton playButton;
    private MaterialButton saveButton;
    private TextView recordingStatusText;
    private Chronometer recordingTimer;
    private View previewControls;

    private AudioRecordService audioRecordService;
    private AudioPlayerManager audioPlayerManager;
    private boolean isRecording = false;
    private VoiceUploadManager voiceUploadManager;
    private RecyclerView audioListView;
    private AudioListAdapter audioListAdapter;

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
    }

    private void initViews() {
        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);
        saveButton = findViewById(R.id.saveButton);
        recordingStatusText = findViewById(R.id.recordingStatusText);
        recordingTimer = findViewById(R.id.recordingTimer);
        previewControls = findViewById(R.id.previewControls);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.settings) {
                // TODO: 打开设置界面
                return true;
            }
            return false;
        });

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            }
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

        playButton.setOnClickListener(v -> {
            try {
                if (!audioPlayerManager.isPlaying()) {
                    audioPlayerManager.playAudio(audioRecordService.getCurrentFilePath());
                    playButton.setText("暂停");
                } else {
                    audioPlayerManager.pauseAudio();
                    playButton.setText("播放");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveButton.setOnClickListener(v -> {
            uploadVoice();
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
            previewControls.setVisibility(View.GONE);
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
        previewControls.setVisibility(View.VISIBLE);
        loadAudioList();
    }

    private void uploadVoice() {
        String filePath = audioRecordService.getCurrentFilePath();
        if (filePath != null) {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            recordingStatusText.setText("正在处理...");
            voiceUploadManager.cloneVoice(
                audioFile,
                "这是一段测试录音",
                new VoiceUploadManager.CloneCallback() {
                    @Override
                    public void onSuccess(String audioUrl, String taskId) {
                        runOnUiThread(() -> {
                            recordingStatusText.setText("变声成功！");
                            Toast.makeText(MainActivity.this, 
                                "变声成功，音频URL：" + audioUrl, 
                                Toast.LENGTH_SHORT).show();
                            // TODO: 下载或播放变声后的音频
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            recordingStatusText.setText("变声失败");
                            Toast.makeText(MainActivity.this, 
                                "变声失败：" + error,
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            );
        }
    }

    private void setupAudioList() {
        audioListView = findViewById(R.id.audioList);
        audioListView.setLayoutManager(new LinearLayoutManager(this));
        
        audioListAdapter = new AudioListAdapter();
        audioListView.setAdapter(audioListAdapter);
        
        audioListAdapter.setOnItemClickListener(new AudioListAdapter.OnItemClickListener() {
            @Override
            public void onPlayClick(AudioItem item) {
                try {
                    audioPlayerManager.playAudio(item.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeleteClick(AudioItem item) {
                // TODO: 实现删除功能
                File file = new File(item.getPath());
                if (file.delete()) {
                    loadAudioList();
                }
            }
        });

        loadAudioList();
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