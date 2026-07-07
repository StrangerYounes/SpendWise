package com.corner.takecontrol.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.corner.takecontrol.data.model.ChallengeTask;

import java.util.Calendar;
import java.util.List;

public class ReminderManager {

    public static void scheduleReminders(Context context, List<ChallengeTask> tasks) {
        if (tasks == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Cannot schedule exact alarms, fallback to inexact or just skip
                return;
            }
        }

        for (ChallengeTask task : tasks) {
            String time = task.getExecutionTime();
            if (time == null || time.isEmpty()) continue;

            String[] parts = time.split(":");
            if (parts.length != 2) continue;

            try {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.MINUTE, -15); // Reminder 15 minutes before

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(context, ReminderReceiver.class);
                intent.putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.getTitle());
                
                int requestCode = task.getId() != null ? task.getId().hashCode() : (int) System.currentTimeMillis();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } catch (NumberFormatException ignored) {}
        }
    }
}
