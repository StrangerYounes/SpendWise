package com.corner.takecontrol.util;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import com.corner.takecontrol.R;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class ChallengeUiUtil {

    private ChallengeUiUtil() {
    }

    @StringRes
    public static int getStatusLabelRes(ChallengeStatus status) {
        switch (status) {
            case ACTIVE:
                return R.string.status_active;
            case COMPLETED:
                return R.string.status_completed;
            case DRAFT:
            default:
                return R.string.status_draft;
        }
    }

    @ColorRes
    public static int getStatusColorRes(ChallengeStatus status) {
        switch (status) {
            case ACTIVE:
                return R.color.status_active;
            case COMPLETED:
                return R.color.status_completed;
            case DRAFT:
            default:
                return R.color.status_draft;
        }
    }

    public static void bindDaysRemaining(Context context, TextView textView, Challenge challenge) {
        if (challenge == null || challenge.getStatusEnum() != ChallengeStatus.ACTIVE) {
            textView.setVisibility(View.GONE);
            return;
        }

        Timestamp endDate = challenge.getEndDate();
        if (endDate == null) {
            textView.setVisibility(View.GONE);
            return;
        }

        long daysLeft = computeDaysRemaining(endDate.toDate());
        if (daysLeft < 0) {
            textView.setVisibility(View.GONE);
            return;
        }

        textView.setVisibility(View.VISIBLE);
        if (daysLeft == 0) {
            textView.setText(R.string.challenge_ends_today);
        } else if (daysLeft == 1) {
            textView.setText(R.string.days_remaining_one);
        } else {
            textView.setText(context.getString(R.string.days_remaining, (int) daysLeft));
        }
    }

    private static long computeDaysRemaining(Date endDate) {
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);

        Calendar now = Calendar.getInstance();
        long diffMs = end.getTimeInMillis() - now.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toDays(diffMs);
    }
}
