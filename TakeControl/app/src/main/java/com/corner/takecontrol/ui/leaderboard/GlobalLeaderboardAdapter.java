package com.corner.takecontrol.ui.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.util.ImageLoader;
import com.corner.takecontrol.util.ProgressionUtil;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class GlobalLeaderboardAdapter extends RecyclerView.Adapter<GlobalLeaderboardAdapter.ViewHolder> {

    public interface OnProfileClickListener {
        void onProfileClick(String userId);
    }

    private final List<UserProfile> users = new ArrayList<>();
    private final String currentUserId;
    private final OnProfileClickListener listener;
    private String sortByField = "xp";

    public GlobalLeaderboardAdapter(String currentUserId, OnProfileClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submitList(List<UserProfile> list, String sortByField) {
        this.sortByField = sortByField;
        users.clear();
        if (list != null) {
            users.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_global_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = users.get(position);
        int rank = position + 1;

        holder.rankText.setText(String.valueOf(rank));
        int rankColor = ContextCompat.getColor(holder.itemView.getContext(), getRankColorRes(rank));
        holder.rankText.getBackground().setTint(rankColor);
        
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(user.getId());
        if (isCurrentUser) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.current_user_highlight));
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        holder.nameText.setText(user.getDisplayName());
        
        String titleId = user.getEquippedTitleId();
        if (titleId != null) {
            holder.equippedTitleText.setText(ProgressionUtil.getCosmeticName(titleId));
            holder.equippedTitleText.setVisibility(View.VISIBLE);
        } else {
            holder.equippedTitleText.setVisibility(View.GONE);
        }

        ImageLoader.loadProfileImage(user.getEncryptedPhoto(), user.getPhotoUrl(), holder.profileImage, R.drawable.ic_streak);
        
        int frameColorRes = ProgressionUtil.getFrameColorRes(user.getEquippedFrameId());
        holder.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(
                holder.itemView.getContext().getColor(frameColorRes)));

        long xpValue;
        switch (sortByField) {
            case "weeklyXp": xpValue = user.getWeeklyXp(); break;
            case "monthlyXp": xpValue = user.getMonthlyXp(); break;
            default: xpValue = user.getXp(); break;
        }
        holder.xpValueText.setText(String.format("%,d XP", xpValue));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClick(user.getId());
        });
    }

    private int getRankColorRes(int rank) {
        switch (rank) {
            case 1: return R.color.rank_gold;
            case 2: return R.color.rank_silver;
            case 3: return R.color.rank_bronze;
            default: return R.color.rank_default;
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rankText;
        final ShapeableImageView profileImage;
        final TextView nameText;
        final TextView equippedTitleText;
        final TextView xpValueText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            equippedTitleText = itemView.findViewById(R.id.equippedTitleText);
            xpValueText = itemView.findViewById(R.id.xpValueText);
        }
    }
}
