package com.corner.takecontrol.ui.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> entries = new ArrayList<>();

    public void submitList(List<LeaderboardEntry> list) {
        entries.clear();
        if (list != null) {
            entries.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.rankText.setText(String.valueOf(position + 1));
        holder.nameText.setText(entry.getDisplayName());
        holder.percentText.setText(holder.itemView.getContext().getString(R.string.progress_percent, entry.getCompletionPercent()));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rankText;
        final TextView nameText;
        final TextView percentText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            nameText = itemView.findViewById(R.id.nameText);
            percentText = itemView.findViewById(R.id.percentText);
        }
    }
}
