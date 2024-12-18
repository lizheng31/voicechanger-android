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
import java.util.Locale;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {
    private List<AudioItem> audioItems = new ArrayList<>();
    private AudioItem selectedItem;
    private AudioPlayerManager audioPlayerManager;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayClick(AudioItem item, MaterialButton playButton);
        void onDeleteClick(AudioItem item);
        void onSendClick(AudioItem item);
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
        
        // 设置文件名和日期
        holder.nameText.setText(item.getName());
        holder.dateText.setText(item.getDate());
        
        // 设置文件大小
        File file = new File(item.getPath());
        String fileSize = formatFileSize(file.length());
        holder.sizeText.setText(fileSize);
        
        // 设置卡片选中状态
        boolean isSelected = (selectedItem != null && 
            selectedItem.getPath().equals(item.getPath()));
        
        // 设置选中状态的视觉效果
        holder.cardView.setChecked(isSelected);
        if (isSelected) {
            // 选中状态：深色边框，浅色背景
            holder.cardView.setStrokeColor(
                holder.cardView.getContext().getColor(R.color.selected_item_stroke));
            holder.cardView.setStrokeWidth(2);
            holder.cardView.setCardElevation(4f);
            holder.cardView.setCardBackgroundColor(
                holder.cardView.getContext().getColor(R.color.selected_item_background));
            holder.nameText.setTextColor(
                holder.cardView.getContext().getColor(R.color.text_primary));
        } else {
            // 未选中状态：恢复默认
            holder.cardView.setStrokeColor(
                holder.cardView.getContext().getColor(R.color.md_theme_light_outline));
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setCardElevation(2f);
            holder.cardView.setCardBackgroundColor(item.isConverted() ? 
                holder.cardView.getContext().getColor(R.color.converted_audio_background) : 
                holder.cardView.getContext().getColor(R.color.original_audio_background));
            holder.nameText.setTextColor(
                holder.cardView.getContext().getColor(R.color.md_theme_light_onSurface));
        }

        // 点击整个卡片的处理
        holder.cardView.setOnClickListener(v -> {
            // 如��点击已选中的项，则取消选择
            if (isSelected) {
                setSelectedItem(null);
            } else {
                setSelectedItem(item);
            }
            
            if (listener != null) {
                listener.onItemSelected(selectedItem);
            }
        });

        // 播放按钮处理
        holder.playButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(item, holder.playButton);
            }
        });

        // 删除按钮处理
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });

        // 只为变声后的音频显示发送按钮
        if (item.isConverted()) {
            holder.sendButton.setVisibility(View.VISIBLE);
            holder.sendButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSendClick(item);
                }
            });
        } else {
            holder.sendButton.setVisibility(View.GONE);
        }
    }

    // 格式化文件大小
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024f);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024f * 1024f));
        }
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    public AudioItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(AudioItem item) {
        AudioItem oldSelection = selectedItem;
        selectedItem = item;
        
        // 更新旧选中项的视图
        if (oldSelection != null) {
            int oldIndex = audioItems.indexOf(oldSelection);
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex);
            }
        }
        
        // 更新新选中项的视图
        if (item != null) {
            int newIndex = audioItems.indexOf(item);
            if (newIndex != -1) {
                notifyItemChanged(newIndex);
            }
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView nameText;
        TextView dateText;
        TextView sizeText;
        MaterialButton playButton;
        MaterialButton deleteButton;
        MaterialButton sendButton;

        public ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            nameText = view.findViewById(R.id.audioNameText);
            dateText = view.findViewById(R.id.audioDateText);
            sizeText = view.findViewById(R.id.audioSizeText);
            playButton = view.findViewById(R.id.playButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            sendButton = view.findViewById(R.id.sendButton);
        }
    }
} 