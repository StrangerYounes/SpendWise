package com.corner.takecontrol.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.corner.takecontrol.databinding.FragmentUserProfileBinding;
import com.corner.takecontrol.util.ImageLoader;
import com.corner.takecontrol.util.ProgressionUtil;
import com.corner.takecontrol.util.XpUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private UserRepository userRepository;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository();
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (userId != null) {
            loadProfile();
        } else {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId != null && userId.equals(currentUserId)) {
            binding.editProfileButton.setVisibility(View.VISIBLE);
            binding.editProfileButton.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.action_profile_to_customization));
        }
    }

    private void loadProfile() {
        userRepository.getUserProfile(userId, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile != null && isAdded()) {
                    bindProfile(profile);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindProfile(UserProfile profile) {
        binding.displayNameText.setText(profile.getDisplayName());
        
        String titleId = profile.getEquippedTitleId();
        if (titleId != null) {
            binding.equippedTitleText.setText(ProgressionUtil.getCosmeticName(titleId));
            binding.equippedTitleText.setVisibility(View.VISIBLE);
        } else {
            binding.equippedTitleText.setVisibility(View.GONE);
        }

        ImageLoader.loadProfileImage(profile.getEncryptedPhoto(), profile.getPhotoUrl(), binding.profileImage, R.drawable.ic_streak);
        
        int frameColorRes = ProgressionUtil.getFrameColorRes(profile.getEquippedFrameId());
        binding.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(
                getResources().getColor(frameColorRes, requireContext().getTheme())));

        binding.profileImage.setOnClickListener(v -> showLargeImage(profile));

        int level = profile.getLevel();
        binding.levelText.setText("Lvl " + level);
        
        long currentXp = profile.getXp();
        long xpForCurrentLevel = XpUtil.getXpForLevel(level);
        long xpForNextLevel = XpUtil.getXpForLevel(level + 1);
        long progressInLevel = currentXp - xpForCurrentLevel;
        long neededForNextLevel = xpForNextLevel - xpForCurrentLevel;
        
        binding.xpText.setText(currentXp + " / " + xpForNextLevel + " XP");
        binding.xpProgressBar.setMax((int) neededForNextLevel);
        binding.xpProgressBar.setProgress((int) progressInLevel);

        binding.currentStreakText.setText(profile.getCurrentStreak() + " days");
        binding.longestStreakText.setText(profile.getLongestStreak() + " days");

        binding.achievementsChipGroup.removeAllViews();
        if (profile.getAchievements() != null) {
            for (String achId : profile.getAchievements()) {
                Chip chip = new Chip(requireContext());
                chip.setText(ProgressionUtil.getAchievementName(achId));
                chip.setChipIconResource(ProgressionUtil.getAchievementIconRes(achId));
                binding.achievementsChipGroup.addView(chip);
            }
        }
    }

    private void showLargeImage(UserProfile profile) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_large_image, null);
        ImageView largeImageView = dialogView.findViewById(R.id.largeImageView);
        
        ImageLoader.loadProfileImage(profile.getEncryptedPhoto(), profile.getPhotoUrl(), largeImageView, R.drawable.ic_streak);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
