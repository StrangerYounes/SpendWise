package com.corner.takecontrol.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.util.ChallengeUiUtil;
import com.corner.takecontrol.util.XpUtil;

import java.util.ArrayList;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHALLENGE = 1;

    public interface OnChallengeClickListener {
        void onChallengeClick(Challenge challenge);
    }

    public interface OnProfileClickListener {
        void onProfileClick();
    }

    private final List<Challenge> challenges = new ArrayList<>();
    private final OnChallengeClickListener listener;
    private OnProfileClickListener profileClickListener;
    private String greetingName;
    private int currentStreak;
    private UserProfile userProfile;

    public ChallengeAdapter(OnChallengeClickListener listener) {
        this.listener = listener;
    }

    public void setOnProfileClickListener(OnProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    public void setGreetingName(String name) {
        this.greetingName = name;
        if (!challenges.isEmpty()) {
            notifyItemChanged(0);
        }
    }

    public void setStreak(int streak) {
        this.currentStreak = streak;
        if (!challenges.isEmpty()) {
            notifyItemChanged(0);
        }
    }

    public void setUserProfile(UserProfile profile) {
        this.userProfile = profile;
        if (!challenges.isEmpty()) {
            notifyItemChanged(0);
        }
    }

    public void submitList(List<Challenge> list) {
        challenges.clear();
        if (list != null) {
            challenges.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_CHALLENGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge, parent, false);
        return new ChallengeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder header = (HeaderViewHolder) holder;
            if (greetingName != null && !greetingName.isEmpty()) {
                header.greetingText.setText(
                        holder.itemView.getContext().getString(R.string.home_greeting, greetingName));
            } else {
                header.greetingText.setText(R.string.home_greeting_default);
            }

            if (currentStreak > 0) {
                header.streakLayout.setVisibility(View.VISIBLE);
                header.streakText.setText(String.valueOf(currentStreak));
            } else {
                header.streakLayout.setVisibility(View.GONE);
            }

            if (userProfile != null) {
                header.levelLayout.setVisibility(View.VISIBLE);
                header.levelText.setText("Lvl " + userProfile.getLevel());

                if (userProfile.getPhotoUrl() != null) {
                    try {
                        header.profileImage.setImageURI(android.net.Uri.parse(userProfile.getPhotoUrl()));
                    } catch (Exception e) {
                        header.profileImage.setImageResource(R.drawable.ic_streak);
                    }
                } else {
                    header.profileImage.setImageResource(R.drawable.ic_streak);
                }

                String equippedTitleId = userProfile.getEquippedTitleId();
                if (equippedTitleId != null) {
                    header.equippedTitleText.setVisibility(View.VISIBLE);
                    header.equippedTitleText.setText(com.corner.takecontrol.util.ProgressionUtil.getCosmeticName(equippedTitleId));
                } else {
                    header.equippedTitleText.setVisibility(View.GONE);
                }

                String frameId = userProfile.getEquippedFrameId();
                int colorRes = com.corner.takecontrol.util.ProgressionUtil.getFrameColorRes(frameId);
                header.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), colorRes)));

                header.profileImage.setOnClickListener(v -> {
                    if (profileClickListener != null) profileClickListener.onProfileClick();
                });

                long currentXp = userProfile.getXp();
                long levelXpStart = XpUtil.getXpForLevel(userProfile.getLevel());
                long nextLevelXpStart = XpUtil.getXpForLevel(userProfile.getLevel() + 1);
                
                long progressXp = currentXp - levelXpStart;
                long totalXpNeeded = nextLevelXpStart - levelXpStart;
                
                int percent = totalXpNeeded > 0 ? (int) (progressXp * 100 / totalXpNeeded) : 0;
                header.levelProgressBar.setProgress(percent);
                header.xpText.setText(currentXp + " XP");
            } else {
                header.levelLayout.setVisibility(View.GONE);
            }
            return;
        }

        Challenge challenge = challenges.get(position - 1);
        ChallengeViewHolder challengeHolder = (ChallengeViewHolder) holder;
        challengeHolder.titleText.setText(challenge.getTitle());

        String description = challenge.getDescription();
        if (description != null && !description.isEmpty()) {
            challengeHolder.descriptionText.setText(description);
            challengeHolder.descriptionText.setVisibility(View.VISIBLE);
        } else {
            challengeHolder.descriptionText.setVisibility(View.GONE);
        }

        challengeHolder.statusText.setText(
                holder.itemView.getContext().getString(
                        ChallengeUiUtil.getStatusLabelRes(challenge.getStatusEnum())));
        challengeHolder.statusText.getBackground().setTint(ContextCompat.getColor(
                holder.itemView.getContext(),
                ChallengeUiUtil.getStatusColorRes(challenge.getStatusEnum())));

        ChallengeUiUtil.bindDaysRemaining(
                holder.itemView.getContext(), challengeHolder.daysRemainingText, challenge);

        String members = challenge.getMemberCount() == 1
                ? holder.itemView.getContext().getString(R.string.solo_challenge)
                : holder.itemView.getContext().getString(R.string.members_count, challenge.getMemberCount());
        challengeHolder.metaText.setText(challenge.getDurationDays() + " days · " + members);

        challengeHolder.itemView.setOnClickListener(v -> listener.onChallengeClick(challenge));
    }

    @Override
    public int getItemCount() {
        return challenges.isEmpty() ? 0 : challenges.size() + 1;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView greetingText;
        final TextView equippedTitleText;
        final com.google.android.material.imageview.ShapeableImageView profileImage;
        final View streakLayout;
        final TextView streakText;
        final View levelLayout;
        final TextView levelText;
        final com.google.android.material.progressindicator.LinearProgressIndicator levelProgressBar;
        final TextView xpText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            greetingText = itemView.findViewById(R.id.greetingText);
            equippedTitleText = itemView.findViewById(R.id.equippedTitleText);
            profileImage = itemView.findViewById(R.id.profileImage);
            streakLayout = itemView.findViewById(R.id.streakLayout);
            streakText = itemView.findViewById(R.id.streakText);
            levelLayout = itemView.findViewById(R.id.levelLayout);
            levelText = itemView.findViewById(R.id.levelText);
            levelProgressBar = itemView.findViewById(R.id.levelProgressBar);
            xpText = itemView.findViewById(R.id.xpText);
        }
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;
        final TextView statusText;
        final TextView daysRemainingText;
        final TextView metaText;

        ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            statusText = itemView.findViewById(R.id.statusText);
            daysRemainingText = itemView.findViewById(R.id.daysRemainingText);
            metaText = itemView.findViewById(R.id.metaText);
        }
    }
}
