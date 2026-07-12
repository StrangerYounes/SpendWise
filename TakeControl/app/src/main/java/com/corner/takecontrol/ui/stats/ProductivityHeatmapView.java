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

public class ProductivityHeatmapView extends View {

    private Map<String, Integer> dailyCompletions = new HashMap<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int cellSize = 30;
    private int cellMargin = 6;
    private int numWeeks = 20;

    public ProductivityHeatmapView(Context context) {
        super(context);
    }

    public ProductivityHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(Map<String, Integer> dailyCompletions) {
        this.dailyCompletions = dailyCompletions != null ? dailyCompletions : new HashMap<>();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (cellSize + cellMargin) * 7 + getPaddingTop() + getPaddingBottom();
        int width = (cellSize + cellMargin) * numWeeks + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dailyCompletions == null) return;

        Calendar cal = Calendar.getInstance();
        // Go back to the start of the first week shown
        cal.add(Calendar.WEEK_OF_YEAR, -(numWeeks - 1));
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        float x = getPaddingLeft();
        float yStart = getPaddingTop();

        for (int w = 0; w < numWeeks; w++) {
            float y = yStart;
            for (int d = 0; d < 7; d++) {
                String dateKey = sdf.format(cal.getTime());
                int count = dailyCompletions.getOrDefault(dateKey, 0);

                paint.setColor(getColorForCount(count));
                rect.set(x, y, x + cellSize, y + cellSize);
                canvas.drawRoundRect(rect, 4, 4, paint);

                cal.add(Calendar.DAY_OF_YEAR, 1);
                y += cellSize + cellMargin;
            }
            x += cellSize + cellMargin;
        }
    }

    private int getColorForCount(int count) {
        if (count == 0) return Color.parseColor("#EBEDF0"); // Empty color
        if (count < 2) return Color.parseColor("#9BE9A8");
        if (count < 4) return Color.parseColor("#40C463");
        if (count < 6) return Color.parseColor("#30A14E");
        return Color.parseColor("#216E39");
    }
}
