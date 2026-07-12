package com.corner.takecontrol.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.util.ImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class SocialAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_REQUEST = 1;

    public interface SocialActionListener {
        void onAddFriend(UserProfile user);
        void onCancelRequest(UserProfile user);
        void onRemoveFriend(UserProfile user);
        void onAcceptRequest(AppNotification notification);
        void onDeclineRequest(AppNotification notification);
        void onProfileClick(String userId);
    }

    private final List<Object> items = new ArrayList<>();
    private final SocialActionListener listener;
    private UserProfile currentUser;

    public SocialAdapter(SocialActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<?> list, UserProfile currentUser) {
        this.currentUser = currentUser;
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof UserProfile) return VIEW_TYPE_USER;
        return VIEW_TYPE_REQUEST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new RequestViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof UserViewHolder) {
            UserProfile user = (UserProfile) item;
            UserViewHolder vh = (UserViewHolder) holder;
            vh.nameText.setText(user.getDisplayName());
            vh.levelText.setText("Level " + user.getLevel());
            ImageLoader.loadProfileImage(user.getEncryptedPhoto(), user.getPhotoUrl(), vh.profileImage, R.drawable.ic_streak);

            boolean isFriend = currentUser != null && currentUser.getFriendIds() != null && currentUser.getFriendIds().contains(user.getId());
            boolean alreadySent = currentUser != null && currentUser.getSentRequestIds() != null && currentUser.getSentRequestIds().contains(user.getId());

            if (isFriend) {
                vh.actionButton.setText("Friend");
                vh.actionButton.setEnabled(false);
                vh.secondaryActionButton.setVisibility(View.VISIBLE);
                vh.secondaryActionButton.setText("Remove");
                vh.secondaryActionButton.setOnClickListener(v -> listener.onRemoveFriend(user));
            } else if (alreadySent) {
                vh.actionButton.setText("Sent");
                vh.actionButton.setEnabled(false);
                vh.secondaryActionButton.setVisibility(View.VISIBLE);
                vh.secondaryActionButton.setText("Undo");
                vh.secondaryActionButton.setOnClickListener(v -> listener.onCancelRequest(user));
            } else {
                vh.actionButton.setText("Add Friend");
                vh.actionButton.setEnabled(true);
                vh.actionButton.setOnClickListener(v -> listener.onAddFriend(user));
                vh.secondaryActionButton.setVisibility(View.GONE);
            }
            
            vh.itemView.setOnClickListener(v -> listener.onProfileClick(user.getId()));

        } else if (holder instanceof RequestViewHolder) {
            AppNotification request = (AppNotification) item;
            RequestViewHolder vh = (RequestViewHolder) holder;
            vh.nameText.setText(request.getFromUserName());
            vh.levelText.setText("Sent you a friend request");
            vh.actionButton.setText("Accept");
            vh.actionButton.setOnClickListener(v -> listener.onAcceptRequest(request));
            
            vh.secondaryActionButton.setVisibility(View.VISIBLE);
            vh.secondaryActionButton.setText("Decline");
            vh.secondaryActionButton.setOnClickListener(v -> listener.onDeclineRequest(request));
            
            vh.itemView.setOnClickListener(v -> listener.onProfileClick(request.getFromUserId()));
            // I'll reuse the levelText area for the decline button or just add it to the layout.
            // For now, let's keep it simple.
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView profileImage;
        final TextView nameText;
        final TextView levelText;
        final MaterialButton actionButton;
        final MaterialButton secondaryActionButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            levelText = itemView.findViewById(R.id.levelText);
            actionButton = itemView.findViewById(R.id.actionButton);
            secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        }
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView profileImage;
        final TextView nameText;
        final TextView levelText;
        final MaterialButton actionButton;
        final MaterialButton secondaryActionButton;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            levelText = itemView.findViewById(R.id.levelText);
            actionButton = itemView.findViewById(R.id.actionButton);
            secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        }
    }
}
