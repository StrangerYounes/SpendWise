package com.corner.takecontrol;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.repository.AuthRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.corner.takecontrol.databinding.ActivityMainBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private final AuthRepository authRepository = new AuthRepository();
    private final UserRepository userRepository = new UserRepository();
    private ListenerRegistration notificationListener;
    private FirebaseAuth.AuthStateListener authStateListener;
    private String pendingChallengeId;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startListeningToNotifications();
                }
            });

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
            if (destination.getId() == R.id.homeFragment && pendingChallengeId != null) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    binding.getRoot().post(this::navigateToPendingChallenge);
                }
            }
        });

        requestNotificationPermission();
        
        authStateListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                startListeningToNotifications();
            } else {
                if (notificationListener != null) {
                    notificationListener.remove();
                    notificationListener = null;
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

        handleWidgetIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && intent.hasExtra("challengeId")) {
            String challengeId = intent.getStringExtra("challengeId");
            if (challengeId != null) {
                pendingChallengeId = challengeId;
                if (navController != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == R.id.homeFragment) {
                        navigateToPendingChallenge();
                    }
                }
            }
        }
    }

    private void navigateToPendingChallenge() {
        if (pendingChallengeId != null && navController != null) {
            String challengeId = pendingChallengeId;
            pendingChallengeId = null;
            Bundle args = new Bundle();
            args.putString("challengeId", challengeId);
            navController.navigate(R.id.challengeDetailFragment, args);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        // Also check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = ContextCompat.getSystemService(this, android.app.AlarmManager.class);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void startListeningToNotifications() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (notificationListener != null) notificationListener.remove();
        notificationListener = userRepository.listenToNotifications(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<AppNotification> result) {
                if (result != null && !result.isEmpty()) {
                    // Show only the most recent notification if there are many to avoid spamming Snackbars
                    AppNotification notification = result.get(result.size() - 1);
                    showNotificationSnackbar(notification);
                    
                    // Mark all as read
                    for (AppNotification n : result) {
                        userRepository.markNotificationRead(userId, n.getId());
                    }
                }
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("MainActivity", "Notification error: " + message);
            }
        });
    }

    private void showNotificationSnackbar(AppNotification notification) {
        Snackbar.make(binding.getRoot(), notification.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("View", v -> {
                    if (notification.getChallengeId() != null && navController != null) {
                        Bundle args = new Bundle();
                        args.putString("challengeId", notification.getChallengeId());
                        navController.navigate(R.id.challengeDetailFragment, args);
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        if (notificationListener != null) notificationListener.remove();
        if (authStateListener != null) FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
