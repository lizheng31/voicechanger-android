package com.mengmeng.voicechager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.mengmeng.voicechager.R;
import com.mengmeng.voicechager.models.AudioItem;
import com.mengmeng.voicechager.utils.AudioPlayerManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import android.content.res.ColorStateList;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {
    private List<AudioItem> audioItems = new ArrayList<>();
    private OnItemClickListener listener;
    private AudioItem selectedItem = null;
    private AudioPlayerManager audioPlayerManager;

    public interface OnItemClickListener {
        void onPlayClick(AudioItem item, MaterialButton playButton);
        void onDeleteClick(AudioItem item);
        void onItemSelected(AudioItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setAudioItems(List<AudioItem> items) {
        this.audioItems = items;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioItem item = audioItems.get(position);
        
        // 设置音频名称
        holder.audioNameText.setText(item.getDisplayName());
        
        // 设置日期
        holder.audioDateText.setText(item.getDate());
        
        // 根据是否是变声文件和选中状态设置不同的背景色
        int backgroundColor;
        if (item == selectedItem) {
            backgroundColor = holder.itemView.getContext().getColor(R.color.selected_item_color);
        } else if (item.isConverted()) {
            backgroundColor = holder.itemView.getContext().getColor(R.color.converted_audio_background);
        } else {
            backgroundColor = holder.itemView.getContext().getColor(R.color.default_item_color);
        }
        holder.itemView.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        
        // 设置播放按钮状态
        if (audioPlayerManager != null && 
            audioPlayerManager.isPlaying() && 
            item.getPath().equals(audioPlayerManager.getCurrentAudioPath())) {
            holder.playButton.setIconResource(android.R.drawable.ic_media_pause);
        } else {
            holder.playButton.setIconResource(android.R.drawable.ic_media_play);
        }
        
        // 设置选中状态
        holder.itemView.setSelected(selectedItem != null && 
            selectedItem.getPath().equals(item.getPath()));

        holder.itemView.setOnClickListener(v -> {
            AudioItem oldSelection = selectedItem;
            selectedItem = item;
            if (oldSelection != null) {
                notifyItemChanged(audioItems.indexOf(oldSelection));
            }
            notifyItemChanged(position);
            if (listener != null) {
                listener.onItemSelected(item);
            }
        });

        holder.playButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(item, holder.playButton);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView audioNameText;
        TextView audioDateText;
        MaterialButton playButton;
        MaterialButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            audioNameText = itemView.findViewById(R.id.audioNameText);
            audioDateText = itemView.findViewById(R.id.audioDateText);
            playButton = itemView.findViewById(R.id.playButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public AudioItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(AudioItem item) {
        AudioItem oldSelection = selectedItem;
        selectedItem = item;
        if (oldSelection != null) {
            notifyItemChanged(audioItems.indexOf(oldSelection));
        }
        if (item != null) {
            notifyItemChanged(audioItems.indexOf(item));
        }
    }

    public List<AudioItem> getAudioItems() {
        return audioItems;
    }

    public void onPlayClick(AudioItem item, MaterialButton playButton) {
        File audioFile = new File(item.getPath());
        if (!audioFile.exists()) {
            Toast.makeText(playButton.getContext(), "音频文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        // ... 其他播放逻辑 ...
    }

    public void setAudioPlayerManager(AudioPlayerManager audioPlayerManager) {
        this.audioPlayerManager = audioPlayerManager;
    }
} 