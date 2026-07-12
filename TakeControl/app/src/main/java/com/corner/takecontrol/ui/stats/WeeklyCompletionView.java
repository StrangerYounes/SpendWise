package com.corner.takecontrol.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyCompletionView extends View {

    public interface OnDayClickListener {
        void onDayClick(String date, int count);
    }

    private Map<String, Integer> dailyCompletions = new HashMap<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat daySdf = new SimpleDateFormat("EEE", Locale.US);
    private OnDayClickListener listener;

    private final List<BarInfo> bars = new ArrayList<>();
    private static class BarInfo {
        RectF rect;
        String date;
        int count;
        BarInfo(RectF rect, String date, int count) {
            this.rect = new RectF(rect);
            this.date = date;
            this.count = count;
        }
    }

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
        axisPaint.setColor(Color.LTGRAY);
        axisPaint.setStrokeWidth(2f);
    }

    public void setData(Map<String, Integer> dailyCompletions) {
        this.dailyCompletions = dailyCompletions != null ? dailyCompletions : new HashMap<>();
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bars.clear();
        int paddingLeft = getPaddingLeft() + 60; // Extra padding for Y axis
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop() + 20;
        int paddingBottom = getPaddingBottom() + 40;

        int width = getWidth() - paddingLeft - paddingRight;
        int height = getHeight() - paddingTop - paddingBottom;
        float barWidth = width / 7f - 20;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        float maxCount = 0;
        Map<String, Integer> last7Days = new HashMap<>();
        List<String> dateKeys = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(cal.getTime());
            dateKeys.add(dateKey);
            int count = dailyCompletions.getOrDefault(dateKey, 0);
            last7Days.put(dateKey, count);
            if (count > maxCount) maxCount = count;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (maxCount == 0) maxCount = 5;
        // Round maxCount up for nice Y axis labels
        int yMax = (int) Math.ceil(maxCount / 5.0) * 5;
        if (yMax == 0) yMax = 5;

        // Draw Y Axis Labels and Grid
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 5; i++) {
            int val = (yMax * i) / 5;
            float y = paddingTop + height - (float) i / 5 * height;
            canvas.drawText(String.valueOf(val), paddingLeft - 10, y + 8, labelPaint);
            canvas.drawLine(paddingLeft, y, paddingLeft + width, y, axisPaint);
        }

        // Draw Bars and X Axis Labels
        labelPaint.setTextAlign(Paint.Align.CENTER);
        cal.add(Calendar.DAY_OF_YEAR, -7);
        float startX = paddingLeft + 10;
        for (int i = 0; i < 7; i++) {
            String dateKey = dateKeys.get(i);
            int count = last7Days.getOrDefault(dateKey, 0);
            float barHeight = ((float) count / yMax) * height;

            float top = paddingTop + height - barHeight;
            float bottom = paddingTop + height;
            barRect.set(startX, top, startX + barWidth, bottom);
            canvas.drawRoundRect(barRect, 8, 8, barPaint);
            bars.add(new BarInfo(barRect, dateKey, count));

            String dayLabel = daySdf.format(cal.getTime());
            canvas.drawText(dayLabel, startX + barWidth / 2, bottom + 30, labelPaint);

            startX += barWidth + 20;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Draw Y axis line
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + height, axisPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            for (BarInfo bar : bars) {
                if (bar.rect.contains(x, y)) {
                    if (listener != null) listener.onDayClick(bar.date, bar.count);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
