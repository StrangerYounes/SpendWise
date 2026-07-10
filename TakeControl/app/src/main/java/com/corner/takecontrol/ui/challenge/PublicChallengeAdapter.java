package com.corner.takecontrol.ui.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;

import java.util.ArrayList;
import java.util.List;

public class PublicChallengeAdapter extends RecyclerView.Adapter<PublicChallengeAdapter.ViewHolder> {

    public interface OnChallengeClickListener {
        void onClick(Challenge challenge);
    }

    private final List<Challenge> challenges = new ArrayList<>();
    private final OnChallengeClickListener listener;

    public PublicChallengeAdapter(OnChallengeClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Challenge> list) {
        challenges.clear();
        if (list != null) {
            challenges.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_public_challenge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Challenge challenge = challenges.get(position);
        holder.titleText.setText(challenge.getTitle());
        holder.descriptionText.setText(challenge.getDescription());
        
        String members = holder.itemView.getContext().getString(R.string.members_count, challenge.getMemberCount());
        holder.memberCountText.setText(members);

        boolean hasPassword = challenge.getEncryptedPassword() != null && !challenge.getEncryptedPassword().isEmpty();
        holder.lockIcon.setVisibility(hasPassword ? View.VISIBLE : View.GONE);

        holder.joinButton.setOnClickListener(v -> listener.onClick(challenge));
        holder.itemView.setOnClickListener(v -> listener.onClick(challenge));
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;
        final TextView memberCountText;
        final ImageView lockIcon;
        final View joinButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            memberCountText = itemView.findViewById(R.id.memberCountText);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            joinButton = itemView.findViewById(R.id.joinButton);
        }
    }
}
