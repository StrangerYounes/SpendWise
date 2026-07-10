package com.corner.takecontrol.ui.challenge;

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

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.databinding.FragmentJoinChallengeBinding;
import com.corner.takecontrol.util.SecurityUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class JoinChallengeFragment extends Fragment {

    private FragmentJoinChallengeBinding binding;
    private JoinChallengeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJoinChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JoinChallengeViewModel.class);

        binding.joinButton.setOnClickListener(v -> {
            String code = binding.shareCodeInput.getText() != null
                    ? binding.shareCodeInput.getText().toString() : "";
            viewModel.joinChallenge(code);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getJoinedChallengeId().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.startsWith("CHECK_PASSWORD:")) {
                    String id = result.substring("CHECK_PASSWORD:".length());
                    showPasswordDialog(id);
                } else {
                    viewModel.consumeJoinedChallengeId();
                    Bundle args = new Bundle();
                    args.putString("challengeId", result);
                    if (getView() != null) {
                        Navigation.findNavController(getView()).navigate(R.id.action_join_to_detail, args);
                    }
                }
            }
        });

        viewModel.getSlotLimitExceeded().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                com.corner.takecontrol.util.ChallengeUiUtil.showSlotLimitDialog(requireContext(), status);
            }
        });
    }

    private void showPasswordDialog(String challengeId) {
        viewModel.consumeJoinedChallengeId();
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null);
        android.widget.EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.enter_password_title)
                .setView(dialogView)
                .setPositiveButton(R.string.join, (dialog, which) -> {
                    String input = passwordInput.getText().toString();
                    
                    // We need the challenge object to decrypt and verify
                    com.corner.takecontrol.data.repository.ChallengeRepository repo = new com.corner.takecontrol.data.repository.ChallengeRepository();
                    repo.getChallenge(challengeId, new RepositoryCallback<>() {
                        @Override
                        public void onSuccess(Challenge challenge) {
                            try {
                                String decrypted = SecurityUtil.decrypt(challenge.getEncryptedPassword());
                                if (input.equals(decrypted)) {
                                    viewModel.confirmJoin(challengeId);
                                } else {
                                    Toast.makeText(requireContext(), R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Decryption failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
