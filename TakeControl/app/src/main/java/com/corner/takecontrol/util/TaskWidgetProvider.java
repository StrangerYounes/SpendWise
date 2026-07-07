package com.corner.takecontrol.util;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.corner.takecontrol.MainActivity;
import com.corner.takecontrol.R;

public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.corner.takecontrol.ACTION_REFRESH";
    public static final String ACTION_OPEN_CHALLENGE = "com.corner.takecontrol.ACTION_OPEN_CHALLENGE";
    public static final String EXTRA_CHALLENGE_ID = "extra_challenge_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction()) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, TaskWidgetProvider.class);
            int[] ids = mgr.getAppWidgetIds(cn);

            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetListView);

            // Update the "Last updated" time
            String time = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date());
            for (int id : ids) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);
                views.setTextViewText(R.id.widgetLastUpdated, time);
                views.setViewVisibility(R.id.widgetLastUpdated, android.view.View.VISIBLE);
                mgr.partiallyUpdateAppWidget(id, views);
            }
        } else if (ACTION_OPEN_CHALLENGE.equals(intent.getAction())) {
            String challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID);
            if (challengeId != null) {
                Intent appIntent = new Intent(context, MainActivity.class);
                appIntent.putExtra("challengeId", challengeId);
                appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(appIntent);
            }
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

        // Refresh button
        Intent refreshIntent = new Intent(context, TaskWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshPendingIntent);

        // Open App on Title click
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetTitle, mainPendingIntent);

        // Set up RemoteViewsService for the list
        Intent serviceIntent = new Intent(context, WidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widgetListView, serviceIntent);
        views.setEmptyView(R.id.widgetListView, R.id.emptyWidgetText);

        // Template for item clicks
        Intent clickIntent = new Intent(context, TaskWidgetProvider.class);
        clickIntent.setAction(ACTION_OPEN_CHALLENGE);
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widgetListView, clickPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
