package com.corner.takecontrol.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.corner.takecontrol.MainActivity;
import com.corner.takecontrol.R;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "task_reminders";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_CHALLENGE_ID = "extra_challenge_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        String challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID);
        if (taskTitle == null) taskTitle = "Task Reminder";

        createNotificationChannel(context);

        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.putExtra("challengeId", challengeId);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_streak) // Changed from launcher icon to something more descriptive
                .setContentTitle("Upcoming Task")
                .setContentText(taskTitle + " starts in 15 minutes!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminders";
            String description = "Notifications for upcoming tasks";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
