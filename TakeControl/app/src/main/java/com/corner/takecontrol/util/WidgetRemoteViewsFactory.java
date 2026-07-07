package com.corner.takecontrol.util;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final List<ChallengeTask> tasks = new ArrayList<>();
    private final ChallengeRepository repository = new ChallengeRepository();

    public WidgetRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tasks.clear();
            return;
        }
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        try {
            List<ChallengeTask> todayTasks = repository.getTodayTasksSync(userId);
            tasks.clear();
            if (todayTasks != null) {
                tasks.addAll(todayTasks);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        
        // Fill-in intent for the template
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(TaskWidgetProvider.EXTRA_CHALLENGE_ID, task.getChallengeId());
        views.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent);
        
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
