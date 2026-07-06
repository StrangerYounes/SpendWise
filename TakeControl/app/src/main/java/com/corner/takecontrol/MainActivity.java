package com.corner.takecontrol;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.corner.takecontrol.data.repository.AuthRepository;
import com.corner.takecontrol.databinding.ActivityMainBinding;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = binding.toolbar;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.homeFragment);
        topLevelDestinations.add(R.id.loginFragment);
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            toolbar.getMenu().clear();
            if (destination.getId() == R.id.homeFragment) {
                toolbar.inflateMenu(R.menu.menu_home);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_sign_out) {
                        authRepository.signOut();
                        controller.navigate(R.id.loginFragment);
                        return true;
                    }
                    return false;
                });
            } else {
                toolbar.setOnMenuItemClickListener(null);
            }
        });

        // Initial navigation is handled by LoginFragment which redirects to HomeFragment if signed in
        // This avoids redundant navigation and crashes during process restoration.

    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
