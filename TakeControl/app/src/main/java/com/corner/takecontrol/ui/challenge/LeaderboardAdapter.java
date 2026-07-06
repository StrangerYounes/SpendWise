package com.corner.takecontrol.ui.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.LeaderboardEntry;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private final String currentUserId;

    public LeaderboardAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        int rank = position + 1;

        holder.rankText.setText(String.valueOf(rank));
        int rankColor = ContextCompat.getColor(holder.itemView.getContext(), getRankColorRes(rank));
        holder.rankText.getBackground().setTint(rankColor);
        holder.rankText.setTextColor(rank <= 3
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.white)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));

        String displayName = entry.getDisplayName();
        boolean isCurrentUser = currentUserId != null
                && currentUserId.equals(entry.getUserId());
        if (isCurrentUser) {
            displayName = holder.itemView.getContext().getString(R.string.you)
                    + " · " + displayName;
            holder.leaderboardCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.current_user_highlight));
            holder.leaderboardCard.setStrokeWidth(2);
            holder.leaderboardCard.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        } else {
            holder.leaderboardCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
            holder.leaderboardCard.setStrokeWidth(1);
            holder.leaderboardCard.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_variant));
        }

        holder.nameText.setText(displayName);
        holder.percentText.setText(holder.itemView.getContext()
                .getString(R.string.progress_percent, entry.getCompletionPercent()));
        holder.progressBar.setProgress(entry.getCompletionPercent());
    }

    private int getRankColorRes(int rank) {
        switch (rank) {
            case 1:
                return R.color.rank_gold;
            case 2:
                return R.color.rank_silver;
            case 3:
                return R.color.rank_bronze;
            default:
                return R.color.rank_default;
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView leaderboardCard;
        final TextView rankText;
        final TextView nameText;
        final TextView percentText;
        final LinearProgressIndicator progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            leaderboardCard = itemView.findViewById(R.id.leaderboardCard);
            rankText = itemView.findViewById(R.id.rankText);
            nameText = itemView.findViewById(R.id.nameText);
            percentText = itemView.findViewById(R.id.percentText);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
