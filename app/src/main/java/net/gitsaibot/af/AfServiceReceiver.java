package net.gitsaibot.af;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import net.gitsaibot.af.AfProvider.AfWidgets;

public class AfServiceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, AfWidget.class));
			for (int appWidgetId : appWidgetIds) {
				Intent updateIntent = new Intent(AfWorker.ACTION_UPDATE_WIDGET)
						.setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId));
				AfWorkManager.enqueueWork(context, updateIntent);
			}
		} else {
			AfWorkManager.enqueueWork(context, intent);
		}
	}
}
