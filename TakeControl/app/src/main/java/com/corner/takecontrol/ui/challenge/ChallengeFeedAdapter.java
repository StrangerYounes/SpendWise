package com.corner.takecontrol.ui.challenge;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengePost;
import com.corner.takecontrol.util.ImageLoader;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class ChallengeFeedAdapter extends RecyclerView.Adapter<ChallengeFeedAdapter.ViewHolder> {

    private final List<ChallengePost> posts = new ArrayList<>();

    public void submitList(List<ChallengePost> list) {
        posts.clear();
        if (list != null) {
            posts.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengePost post = posts.get(position);

        holder.userNameText.setText(post.getUserName());
        holder.contentText.setText(post.getContent());

        if (post.getTimestamp() != null) {
            long time = post.getTimestamp().toDate().getTime();
            holder.timestampText.setText("• " + DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        } else {
            holder.timestampText.setText("");
        }

        ImageLoader.loadProfileImage(post.getUserEncryptedPhoto(), post.getUserPhotoUrl(), holder.profileImage, R.drawable.ic_streak);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView profileImage;
        final TextView userNameText;
        final TextView timestampText;
        final TextView contentText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userNameText = itemView.findViewById(R.id.userNameText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
        }
    }
}
