package com.corner.takecontrol.util;

import com.corner.takecontrol.data.model.TaskFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PeriodKeyUtil {

    private static final DateTimeFormatter DAILY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTHLY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private PeriodKeyUtil() {
    }

    public static String getCurrentPeriodKey(TaskFrequency frequency) {
        return getPeriodKey(frequency, LocalDate.now());
    }

    public static String getPeriodKey(TaskFrequency frequency, LocalDate date) {
        switch (frequency) {
            case WEEKLY:
                return getWeeklyKey(date);
            case MONTHLY:
                return getMonthlyKey(date);
            case DAILY:
            default:
                return getDailyKey(date);
        }
    }

    public static String getDailyKey(LocalDate date) {
        return date.format(DAILY_FORMAT);
    }

    public static String getWeeklyKey(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return String.format(Locale.US, "%d-W%02d", year, week);
    }

    public static String getMonthlyKey(LocalDate date) {
        return YearMonth.from(date).format(MONTHLY_FORMAT);
    }

    public static List<String> getPeriodKeysBetween(TaskFrequency frequency, LocalDate startInclusive, LocalDate endInclusive) {
        List<String> keys = new ArrayList<>();
        if (endInclusive.isBefore(startInclusive)) {
            return keys;
        }

        switch (frequency) {
            case DAILY:
                LocalDate day = startInclusive;
                while (!day.isAfter(endInclusive)) {
                    keys.add(getDailyKey(day));
                    day = day.plusDays(1);
                }
                break;
            case WEEKLY:
                LocalDate weekStart = startInclusive.with(DayOfWeek.MONDAY);
                if (weekStart.isBefore(startInclusive)) {
                    weekStart = startInclusive;
                }
                while (!weekStart.isAfter(endInclusive)) {
                    keys.add(getWeeklyKey(weekStart));
                    weekStart = weekStart.plusWeeks(1);
                }
                break;
            case MONTHLY:
                YearMonth month = YearMonth.from(startInclusive);
                YearMonth endMonth = YearMonth.from(endInclusive);
                while (!month.isAfter(endMonth)) {
                    keys.add(month.format(MONTHLY_FORMAT));
                    month = month.plusMonths(1);
                }
                break;
            default:
                break;
        }
        return keys;
    }

    public static String getPeriodLabel(TaskFrequency frequency, String periodKey) {
        switch (frequency) {
            case WEEKLY:
                return "Week " + periodKey;
            case MONTHLY:
                return periodKey;
            case DAILY:
            default:
                return periodKey;
        }
    }
}
