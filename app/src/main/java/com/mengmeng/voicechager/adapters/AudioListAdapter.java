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
import java.util.ArrayList;
import java.util.List;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {
    private List<AudioItem> audioItems = new ArrayList<>();
    private OnItemClickListener listener;
    private AudioItem selectedItem;

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
        holder.audioNameText.setText(item.getName());
        holder.audioDateText.setText(item.getDate());
        
        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        cardView.setChecked(item == selectedItem);
        
        holder.itemView.setOnClickListener(v -> {
            if (selectedItem != item) {
                AudioItem oldSelection = selectedItem;
                selectedItem = item;
                if (oldSelection != null) {
                    notifyItemChanged(audioItems.indexOf(oldSelection));
                }
                notifyItemChanged(position);
                if (listener != null) {
                    listener.onItemSelected(item);
                }
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
} 