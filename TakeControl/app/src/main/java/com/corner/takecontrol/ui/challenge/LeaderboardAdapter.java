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

    public interface OnNudgeListener {
        void onNudge(LeaderboardEntry entry);
    }

    public interface OnKickListener {
        void onKick(LeaderboardEntry entry);
    }

    public interface OnProfileClickListener {
        void onProfileClick();
    }

    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private final String currentUserId;
    private final OnNudgeListener nudgeListener;
    private OnKickListener kickListener;
    private OnProfileClickListener profileClickListener;
    private boolean isOwner = false;

    public LeaderboardAdapter(String currentUserId, OnNudgeListener nudgeListener) {
        this.currentUserId = currentUserId;
        this.nudgeListener = nudgeListener;
    }

    public void setOnKickListener(OnKickListener kickListener) {
        this.kickListener = kickListener;
    }

    public void setOnProfileClickListener(OnProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
        notifyDataSetChanged();
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

        holder.profileImage.setOnClickListener(v -> {
            if (isCurrentUser && profileClickListener != null) {
                profileClickListener.onProfileClick();
            }
        });

        holder.nameText.setText(displayName);

        if (entry.getPhotoUrl() != null) {
            try {
                holder.profileImage.setImageURI(android.net.Uri.parse(entry.getPhotoUrl()));
            } catch (Exception e) {
                holder.profileImage.setImageResource(R.drawable.ic_streak);
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_streak);
        }

        String equippedTitleId = entry.getEquippedTitleId();
        if (equippedTitleId != null) {
            holder.equippedTitleText.setVisibility(View.VISIBLE);
            holder.equippedTitleText.setText(com.corner.takecontrol.util.ProgressionUtil.getCosmeticName(equippedTitleId));
        } else {
            holder.equippedTitleText.setVisibility(View.GONE);
        }

        String frameId = entry.getEquippedFrameId();
        int colorRes = com.corner.takecontrol.util.ProgressionUtil.getFrameColorRes(frameId);
        holder.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), colorRes)));

        holder.percentText.setText(holder.itemView.getContext()
                .getString(R.string.progress_percent, entry.getCompletionPercent()));
        holder.progressBar.setProgress(entry.getCompletionPercent());

        if (!isCurrentUser && entry.getCompletionPercent() < 100) {
            holder.nudgeButton.setVisibility(View.VISIBLE);
            holder.nudgeButton.setOnClickListener(v -> nudgeListener.onNudge(entry));
        } else {
            holder.nudgeButton.setVisibility(View.GONE);
        }

        if (isOwner && !isCurrentUser) {
            holder.kickButton.setVisibility(View.VISIBLE);
            holder.kickButton.setOnClickListener(v -> {
                if (kickListener != null) kickListener.onKick(entry);
            });
        } else {
            holder.kickButton.setVisibility(View.GONE);
        }
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
        final com.google.android.material.imageview.ShapeableImageView profileImage;
        final TextView nameText;
        final TextView equippedTitleText;
        final TextView percentText;
        final LinearProgressIndicator progressBar;
        final View nudgeButton;
        final View kickButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            leaderboardCard = itemView.findViewById(R.id.leaderboardCard);
            rankText = itemView.findViewById(R.id.rankText);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            equippedTitleText = itemView.findViewById(R.id.equippedTitleText);
            percentText = itemView.findViewById(R.id.percentText);
            progressBar = itemView.findViewById(R.id.progressBar);
            nudgeButton = itemView.findViewById(R.id.nudgeButton);
            kickButton = itemView.findViewById(R.id.kickButton);
        }
    }
}
