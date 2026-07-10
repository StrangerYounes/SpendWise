package com.corner.takecontrol.ui.challenge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.databinding.FragmentExploreBinding;
import com.corner.takecontrol.util.SecurityUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private ExploreViewModel viewModel;
    private PublicChallengeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        adapter = new PublicChallengeAdapter(this::onChallengeClicked);
        binding.exploreRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.exploreRecyclerView.setAdapter(adapter);

        binding.searchBar.setHint(R.string.search_public_hint);
        binding.searchView.setHint(R.string.search_public_hint);

        binding.searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = binding.searchView.getText().toString();
            binding.searchBar.setText(query);
            binding.searchView.hide();
            viewModel.filter(query);
            return false;
        });

        viewModel.getFilteredChallenges().observe(getViewLifecycleOwner(), challenges -> {
            adapter.submitList(challenges);
            binding.emptyText.setVisibility(challenges == null || challenges.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getChallengeJoined().observe(getViewLifecycleOwner(), challengeId -> {
            if (challengeId != null) {
                viewModel.consumeChallengeJoined();
                Bundle args = new Bundle();
                args.putString("challengeId", challengeId);
                Navigation.findNavController(requireView()).navigate(R.id.action_explore_to_detail, args);
            }
        });

        viewModel.getSlotLimitExceeded().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                com.corner.takecontrol.util.ChallengeUiUtil.showSlotLimitDialog(requireContext(), status);
            }
        });
    }

    private void onChallengeClicked(Challenge challenge) {
        if (challenge.getEncryptedPassword() == null || challenge.getEncryptedPassword().isEmpty()) {
            viewModel.joinChallenge(challenge.getId());
        } else {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null);
            EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.enter_password_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.join, (dialog, which) -> {
                        String input = passwordInput.getText().toString();
                        try {
                            String decrypted = SecurityUtil.decrypt(challenge.getEncryptedPassword());
                            if (input.equals(decrypted)) {
                                viewModel.joinChallenge(challenge.getId());
                            } else {
                                Toast.makeText(requireContext(), R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Decryption failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
