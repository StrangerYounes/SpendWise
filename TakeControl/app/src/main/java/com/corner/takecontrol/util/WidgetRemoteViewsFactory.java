package com.corner.takecontrol.util;

import android.content.Context;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;

import java.util.ArrayList;
import java.util.List;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final List<ChallengeTask> tasks = new ArrayList<>();

    public WidgetRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        // Initial data fetch could happen here but it's tricky with Firestore async
        // For demonstration, we'll use empty list or mock if needed.
    }

    @Override
    public void onDataSetChanged() {
        // This is called when notifyAppWidgetViewDataChanged is called
    }

    @Override
    public void onDestroy() {
        tasks.clear();
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= tasks.size()) return null;
        
        ChallengeTask task = tasks.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_task);
        views.setTextViewText(R.id.taskTitle, task.getTitle());
        views.setTextViewText(R.id.taskMeta, task.getExecutionTime() != null ? task.getExecutionTime() : "");
        
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
