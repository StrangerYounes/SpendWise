package com.corner.takecontrol.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentProfileCustomizationBinding;
import com.corner.takecontrol.util.ProgressionUtil;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class ProfileCustomizationFragment extends Fragment {

    private FragmentProfileCustomizationBinding binding;
    private ProfileCustomizationViewModel viewModel;
    private CosmeticRewardAdapter framesAdapter;
    private CosmeticRewardAdapter titlesAdapter;

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> cropImage =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    android.net.Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        viewModel.uploadAndSetPhoto(resultUri, requireContext().getContentResolver());
                        binding.profileImage.setImageURI(resultUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    android.content.Intent data = result.getData();
                    if (data != null) {
                        Throwable cropError = UCrop.getError(data);
                        if (cropError != null) {
                            Toast.makeText(requireContext(), cropError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    private void startCrop(@NonNull android.net.Uri uri) {
        String destinationFileName = "cropped_profile_image.jpg";
        android.net.Uri destinationUri = android.net.Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName));

        UCrop uCrop = UCrop.of(uri, destinationUri);
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(512, 512);

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        
        // Framing options: allow rotation and scaling
        options.setAllowedGestures(com.yalantis.ucrop.UCropActivity.SCALE, com.yalantis.ucrop.UCropActivity.ROTATE, com.yalantis.ucrop.UCropActivity.ALL);
        
        // Style it to match app
        options.setToolbarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_dark));
        options.setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white));
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary));

        cropImage.launch(uCrop.withOptions(options).getIntent(requireContext()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileCustomizationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileCustomizationViewModel.class);

        setupRecyclerViews();

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.displayNameInput.setText(profile.getDisplayName());
                
                com.corner.takecontrol.util.ImageLoader.loadProfileImage(
                        profile.getEncryptedPhoto(), profile.getPhotoUrl(), binding.profileImage, R.drawable.ic_streak);

                // Update frame in preview
                String frameId = profile.getEquippedFrameId();
                int colorRes = ProgressionUtil.getFrameColorRes(frameId);
                binding.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)));

                framesAdapter.setData(ProgressionUtil.getAllFrames(), profile.getUnlockedFrames(), 
                        profile.getEquippedFrameId() != null ? profile.getEquippedFrameId() : "NONE");
                titlesAdapter.setData(ProgressionUtil.getAllTitles(), profile.getUnlockedTitles(), 
                        profile.getEquippedTitleId() != null ? profile.getEquippedTitleId() : "NONE");
            }
        });

        binding.saveProfileButton.setOnClickListener(v -> {
            String newName = binding.displayNameInput.getText().toString();
            viewModel.updateDisplayName(newName);
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
        });

        binding.changePhotoButton.setOnClickListener(v -> {
            pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupRecyclerViews() {
        framesAdapter = new CosmeticRewardAdapter(id -> viewModel.equipFrame(id));
        binding.framesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.framesRecyclerView.setAdapter(framesAdapter);

        titlesAdapter = new CosmeticRewardAdapter(id -> viewModel.equipTitle(id));
        binding.titlesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.titlesRecyclerView.setAdapter(titlesAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
