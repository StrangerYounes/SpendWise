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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.databinding.FragmentProfileCustomizationBinding;
import com.corner.takecontrol.util.ProgressionUtil;

public class ProfileCustomizationFragment extends Fragment {

    private FragmentProfileCustomizationBinding binding;
    private ProfileCustomizationViewModel viewModel;
    private CosmeticRewardAdapter framesAdapter;
    private CosmeticRewardAdapter titlesAdapter;

    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    viewModel.updatePhotoUrl(uri.toString());
                    binding.profileImage.setImageURI(uri);
                }
            });

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
                if (profile.getPhotoUrl() != null) {
                    try {
                        binding.profileImage.setImageURI(android.net.Uri.parse(profile.getPhotoUrl()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

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
