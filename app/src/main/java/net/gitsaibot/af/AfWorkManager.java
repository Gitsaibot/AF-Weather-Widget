package net.gitsaibot.af;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class AfWorkManager {

    public static void enqueueWork(Context context, Intent work) {
        String action = work.getAction();
        Uri widgetUri = work.getData();

        Data.Builder dataBuilder = new Data.Builder();
        if (action != null) {
            dataBuilder.putString(AfWorker.KEY_ACTION, action);
        }
        if (widgetUri != null) {
            dataBuilder.putString(AfWorker.KEY_WIDGET_URI, widgetUri.toString());
        }

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AfWorker.class)
                .setInputData(dataBuilder.build())
                .build();

        String uniqueWorkName = "";
        if (widgetUri != null) {
            uniqueWorkName = widgetUri.toString();
        } else if (action != null) {
            uniqueWorkName = action;
        }

        WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest);
    }
}
