package com.mengmeng.voicechager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.mengmeng.voicechager.R;
import com.mengmeng.voicechager.models.AudioItem;
import java.util.ArrayList;
import java.util.List;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {
    private List<AudioItem> audioItems = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayClick(AudioItem item);
        void onDeleteClick(AudioItem item);
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        AudioItem item = audioItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return audioItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        View playButton;
        View deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.audioNameText);
            dateText = itemView.findViewById(R.id.audioDateText);
            playButton = itemView.findViewById(R.id.playButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(AudioItem item) {
            nameText.setText(item.getName());
            dateText.setText(item.getDate());
            
            playButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(item);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });
        }
    }
} 