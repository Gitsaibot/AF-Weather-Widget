package net.gitsaibot.af;

import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.util.AfWidgetInfo;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        float density = context.getResources().getDisplayMetrics().density;

        final int portW_px = (int)(minWidth * density);
        final int portH_px = (int)(maxHeight * density);

        final int landW_px = (int)(maxWidth * density);
        final int landH_px = (int)(minHeight * density);

        final PendingResult result = goAsync();

        new Thread(() -> {
            try {
                net.gitsaibot.af.AfSettings.saveExactDimensions(context, appWidgetId, portW_px, portH_px, landW_px, landH_px);

                Uri widgetUri = ContentUris.withAppendedId(net.gitsaibot.af.AfProvider.AfWidgets.CONTENT_URI, appWidgetId);

                AfWidgetInfo widgetInfo = AfWidgetInfo.build(context, widgetUri);
                widgetInfo.loadSettings(context);

                AfSettings settings = AfSettings.build(context, widgetInfo);
                settings.loadSettings();

                AfUpdate update = AfUpdate.build(context, widgetInfo, settings);
                update.process();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                result.finish();
            }
        }).start();
    }
}
