package com.corner.takecontrol.ui.home;

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

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.databinding.FragmentHomeBinding;
import com.corner.takecontrol.ui.challenge.TemplatesDialogFragment;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ChallengeAdapter adapter;
    private boolean isMenuExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupMenu();

        adapter = new ChallengeAdapter(this::openChallenge);
        binding.challengesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.challengesRecyclerView.setAdapter(adapter);

        // Speed Dial FAB Logic
        binding.mainFab.setOnClickListener(v -> toggleMenu());

        binding.createFab.setOnClickListener(v -> {
            toggleMenu();
            Navigation.findNavController(v).navigate(R.id.action_home_to_create);
        });

        binding.joinFab.setOnClickListener(v -> {
            toggleMenu();
            Navigation.findNavController(v).navigate(R.id.action_home_to_join);
        });

        binding.exploreFab.setOnClickListener(v -> {
            toggleMenu();
            Navigation.findNavController(v).navigate(R.id.action_home_to_explore);
        });

        binding.templatesFab.setOnClickListener(v -> {
            toggleMenu();
            TemplatesDialogFragment dialog = new TemplatesDialogFragment();
            dialog.setOnTemplateSelectedListener(template -> {
                Bundle args = new Bundle();
                args.putString("templateId", template.getId());
                Navigation.findNavController(requireView()).navigate(R.id.action_home_to_create, args);
            });
            dialog.show(getChildFragmentManager(), "Templates");
        });

        viewModel.getChallenges().observe(getViewLifecycleOwner(), this::renderChallenges);
        viewModel.getDisplayName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) {
                adapter.setGreetingName(name);
            }
        });
        viewModel.getCurrentStreak().observe(getViewLifecycleOwner(), streak -> {
            if (streak != null) {
                adapter.setStreak(streak);
            }
        });
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                adapter.setUserProfile(profile);
            }
        });

        viewModel.getShowArchived().observe(getViewLifecycleOwner(), showingArchived -> {
            setupMenu();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupMenu() {
        com.google.android.material.appbar.MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar == null) return;

        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_home);
        
        android.view.MenuItem toggleItem = toolbar.getMenu().findItem(R.id.action_toggle_archived);
        if (toggleItem != null) {
            boolean showingArchived = Boolean.TRUE.equals(viewModel.getShowArchived().getValue());
            toggleItem.setTitle(showingArchived ? R.string.show_active : R.string.show_archived);
        }

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_toggle_archived) {
                viewModel.toggleShowArchived();
                return true;
            } else if (item.getItemId() == R.id.action_sign_out) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                Navigation.findNavController(requireView()).navigate(R.id.loginFragment);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startListening();
    }

    private void renderChallenges(List<Challenge> challenges) {
        boolean empty = challenges == null || challenges.isEmpty();
        binding.emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.challengesRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.submitList(challenges);

        com.google.android.material.appbar.MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            boolean archived = Boolean.TRUE.equals(viewModel.getShowArchived().getValue());
            toolbar.setTitle(archived ? R.string.show_archived : R.string.home_title);
        }
    }

    private void openChallenge(Challenge challenge) {
        Bundle args = new Bundle();
        args.putString("challengeId", challenge.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_home_to_detail, args);
    }

    private void toggleMenu() {
        isMenuExpanded = !isMenuExpanded;

        int visibility = isMenuExpanded ? View.VISIBLE : View.GONE;
        binding.createFab.setVisibility(visibility);
        binding.joinFab.setVisibility(visibility);
        binding.exploreFab.setVisibility(visibility);
        binding.templatesFab.setVisibility(visibility);

        float rotation = isMenuExpanded ? 45f : 0f;
        binding.mainFab.animate().rotation(rotation).setDuration(200).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
