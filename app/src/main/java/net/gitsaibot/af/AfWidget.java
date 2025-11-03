package net.gitsaibot.af;

import net.gitsaibot.af.AfProvider.AfWidgets;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

public class AfWidget extends AppWidgetProvider {

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(AfWorker.ACTION_DELETE_WIDGET)
					.setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId));
			AfWorkManager.enqueueWork(context, updateIntent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(
					new ComponentName(context, AfWidget.class));
		}
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(AfWorker.ACTION_UPDATE_WIDGET)
					.setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId));
			AfWorkManager.enqueueWork(context, updateIntent);
		}
	}
	
}
