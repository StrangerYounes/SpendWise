package com.corner.takecontrol.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.databinding.FragmentSocialBinding;
import com.google.android.material.tabs.TabLayout;

public class SocialFragment extends Fragment {

    private FragmentSocialBinding binding;
    private SocialViewModel viewModel;
    private SocialAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSocialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SocialViewModel.class);

        adapter = new SocialAdapter(new SocialAdapter.SocialActionListener() {
            @Override
            public void onAddFriend(UserProfile user) {
                viewModel.sendFriendRequest(user);
                Toast.makeText(requireContext(), "Request sent!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelRequest(UserProfile user) {
                viewModel.cancelFriendRequest(user);
                Toast.makeText(requireContext(), "Request cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveFriend(UserProfile user) {
                viewModel.removeFriend(user.getId());
                Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAcceptRequest(AppNotification notification) {
                viewModel.acceptRequest(notification);
                Toast.makeText(requireContext(), "Friend added!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeclineRequest(AppNotification notification) {
                if (notification != null) {
                    viewModel.declineRequest(notification);
                    Toast.makeText(requireContext(), "Request declined", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProfileClick(String userId) {
                Bundle args = new Bundle();
                args.putString("userId", userId);
                Navigation.findNavController(requireView()).navigate(R.id.action_social_to_profile, args);
            }
        });

        binding.socialRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.socialRecyclerView.setAdapter(adapter);

        View.OnClickListener searchAction = v -> {
            String query = binding.searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                if (binding.socialTabLayout.getSelectedTabPosition() != 2) {
                    binding.socialTabLayout.selectTab(binding.socialTabLayout.getTabAt(2));
                }
                viewModel.searchUsers(query);
            } else {
                Toast.makeText(requireContext(), "Enter a name to search", Toast.LENGTH_SHORT).show();
            }
        };

        binding.searchButton.setOnClickListener(searchAction);
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAction.onClick(v);
                return true;
            }
            return false;
        });

        binding.socialTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateViewForTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewModel.getUsers().observe(getViewLifecycleOwner(), list -> {
            if (binding.socialTabLayout.getSelectedTabPosition() != 1) {
                adapter.setData(list, viewModel.getCurrentUserProfile());
                binding.emptyText.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getRequests().observe(getViewLifecycleOwner(), list -> {
            if (binding.socialTabLayout.getSelectedTabPosition() == 1) {
                adapter.setData(list, viewModel.getCurrentUserProfile());
                binding.emptyText.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), error -> { if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show(); });

        updateViewForTab(0);
    }

    private void updateViewForTab(int position) {
        binding.emptyText.setVisibility(View.GONE);
        if (position == 0) {
            binding.searchLayout.setVisibility(View.GONE);
            viewModel.loadFriends();
            binding.emptyText.setText("No friends yet. Try searching for others!");
        } else if (position == 1) {
            binding.searchLayout.setVisibility(View.GONE);
            viewModel.loadRequests();
            binding.emptyText.setText("No pending requests.");
        } else {
            binding.searchLayout.setVisibility(View.VISIBLE);
            adapter.setData(new java.util.ArrayList<>(), viewModel.getCurrentUserProfile());
            binding.emptyText.setText("Search for users by name.");
            binding.emptyText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
