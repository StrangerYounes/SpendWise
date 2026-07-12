package com.corner.takecontrol.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.databinding.FragmentProfileCustomizationBinding;
import com.corner.takecontrol.util.ProgressionUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.List;

public class ProfileCustomizationFragment extends Fragment {

    private FragmentProfileCustomizationBinding binding;
    private ProfileCustomizationViewModel viewModel;
    private CosmeticRewardAdapter framesAdapter;
    private CosmeticRewardAdapter titlesAdapter;
    private String[] scopes = {"None", "Global", "Country", "University", "Company"};
    private String[] timeframes = {"All-Time", "Weekly", "Monthly"};

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
        options.setAllowedGestures(com.yalantis.ucrop.UCropActivity.SCALE, com.yalantis.ucrop.UCropActivity.ROTATE, com.yalantis.ucrop.UCropActivity.ALL);
        options.setToolbarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_dark));
        options.setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white));
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary));
        cropImage.launch(uCrop.withOptions(options).getIntent(requireContext()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileCustomizationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileCustomizationViewModel.class);

        setupRecyclerViews();
        setupFlexSpinners();

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.displayNameInput.setText(profile.getDisplayName());
                binding.countryInput.setText(profile.getCountry());
                binding.universityInput.setText(profile.getUniversity());
                binding.companyInput.setText(profile.getCompany());

                String currentScope = profile.getFlexedRankScope() != null ? profile.getFlexedRankScope() : "None";
                binding.flexScopeSpinner.setText(currentScope, false);

                String timeframe = "All-Time";
                if ("weeklyXp".equals(profile.getFlexedRankTimeframe())) timeframe = "Weekly";
                else if ("monthlyXp".equals(profile.getFlexedRankTimeframe())) timeframe = "Monthly";
                binding.flexTimeframeSpinner.setText(timeframe, false);

                com.corner.takecontrol.util.ImageLoader.loadProfileImage(profile.getEncryptedPhoto(), profile.getPhotoUrl(), binding.profileImage, R.drawable.ic_streak);

                String frameId = profile.getEquippedFrameId();
                int colorRes = ProgressionUtil.getFrameColorRes(frameId);
                binding.profileImage.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)));

                framesAdapter.setData(ProgressionUtil.getAllFrames(), profile.getUnlockedFrames(), profile.getEquippedFrameId() != null ? profile.getEquippedFrameId() : "NONE");
                titlesAdapter.setData(ProgressionUtil.getAllTitles(), profile.getUnlockedTitles(), profile.getEquippedTitleId() != null ? profile.getEquippedTitleId() : "NONE");

                updateCustomLabels(profile.getCustomCategories(), profile.getCustomActions());
            }
        });

        binding.saveProfileButton.setOnClickListener(v -> {
            String newName = binding.displayNameInput.getText().toString();
            String country = binding.countryInput.getText().toString();
            String university = binding.universityInput.getText().toString();
            String company = binding.companyInput.getText().toString();
            
            String flexScope = binding.flexScopeSpinner.getText().toString();
            String flexTimeframe = binding.flexTimeframeSpinner.getText().toString();
            String timeframeKey = "xp";
            if ("Weekly".equals(flexTimeframe)) timeframeKey = "weeklyXp";
            else if ("Monthly".equals(flexTimeframe)) timeframeKey = "monthlyXp";

            viewModel.updateProfileInfo(newName, country, university, company);
            viewModel.updateFlexedRank(flexScope.equals("None") ? null : flexScope, timeframeKey);

            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
        });

        binding.changePhotoButton.setOnClickListener(v -> {
            pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder().setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });

        binding.addCategoryBtn.setOnClickListener(v -> showLabelDialog(null, true));
        binding.addActionBtn.setOnClickListener(v -> showLabelDialog(null, false));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), error -> { if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show(); });
    }

    private void updateCustomLabels(List<String> categories, List<String> actions) {
        binding.categoriesChipGroup.removeAllViews();
        for (String cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat);
            chip.setOnClickListener(v -> showLabelDialog(cat, true));
            binding.categoriesChipGroup.addView(chip);
        }

        binding.actionsChipGroup.removeAllViews();
        for (String act : actions) {
            Chip chip = new Chip(requireContext());
            chip.setText(act);
            chip.setOnClickListener(v -> showLabelDialog(act, false));
            binding.actionsChipGroup.addView(chip);
        }
    }

    private void showLabelDialog(String existingLabel, boolean isCategory) {
        EditText input = new EditText(requireContext());
        input.setText(existingLabel);
        input.setHint(isCategory ? "Category Name" : "Action Name");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding * 2, padding, padding * 2, padding);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingLabel == null ? (isCategory ? "Add Category" : "Add Action") : (isCategory ? "Edit Category" : "Edit Action"))
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String label = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(label)) {
                        if (isCategory) {
                            if (existingLabel == null) viewModel.addCategory(label);
                            else viewModel.editCategory(existingLabel, label);
                        } else {
                            if (existingLabel == null) viewModel.addAction(label);
                            else viewModel.editAction(existingLabel, label);
                        }
                    }
                });

        if (existingLabel != null) {
            builder.setNeutralButton(R.string.remove, (dialog, which) -> {
                if (isCategory) viewModel.deleteCategory(existingLabel);
                else viewModel.deleteAction(existingLabel);
            });
        }
        builder.setNegativeButton(R.string.cancel, null).show();
    }

    private void setupFlexSpinners() {
        android.widget.ArrayAdapter<String> scopeAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, scopes);
        binding.flexScopeSpinner.setAdapter(scopeAdapter);

        android.widget.ArrayAdapter<String> timeframeAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, timeframes);
        binding.flexTimeframeSpinner.setAdapter(timeframeAdapter);
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
