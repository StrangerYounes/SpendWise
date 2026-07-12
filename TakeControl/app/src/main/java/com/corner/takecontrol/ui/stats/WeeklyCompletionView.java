package com.corner.takecontrol.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WeeklyCompletionView extends View {

    private Map<String, Integer> dailyCompletions = new HashMap<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat daySdf = new SimpleDateFormat("EEE", Locale.US);

    public WeeklyCompletionView(Context context) {
        super(context);
        init();
    }

    public WeeklyCompletionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint.setColor(Color.parseColor("#40C463"));
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(24f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(Map<String, Integer> dailyCompletions) {
        this.dailyCompletions = dailyCompletions != null ? dailyCompletions : new HashMap<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom() - 40; // Space for labels
        float barWidth = width / 7f - 20;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        float maxCount = 0;
        Map<String, Integer> last7Days = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(cal.getTime());
            int count = dailyCompletions.getOrDefault(dateKey, 0);
            last7Days.put(dateKey, count);
            if (count > maxCount) maxCount = count;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (maxCount == 0) maxCount = 5; // Default scale

        cal.add(Calendar.DAY_OF_YEAR, -7); // Reset to 7 days ago
        float startX = getPaddingLeft() + 10;
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(cal.getTime());
            int count = last7Days.getOrDefault(dateKey, 0);
            float barHeight = (count / maxCount) * height;

            barRect.set(startX, height + getPaddingTop() - barHeight, startX + barWidth, height + getPaddingTop());
            canvas.drawRoundRect(barRect, 8, 8, barPaint);

            String dayLabel = daySdf.format(cal.getTime());
            canvas.drawText(dayLabel, startX + barWidth / 2, height + getPaddingTop() + 30, labelPaint);

            startX += barWidth + 20;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
}
