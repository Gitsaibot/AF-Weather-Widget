package net.gitsaibot.af;

import static net.gitsaibot.af.AfSettings.CALIBRATION_STATE_FINISHED;
import static net.gitsaibot.af.AfSettings.CALIBRATION_STATE_VERTICAL;
import static net.gitsaibot.af.AfSettings.LANDSCAPE_HEIGHT;
import static net.gitsaibot.af.AfSettings.LANDSCAPE_WIDTH;
import static net.gitsaibot.af.AfSettings.PORTRAIT_HEIGHT;
import static net.gitsaibot.af.AfSettings.PORTRAIT_WIDTH;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.util.AfWidgetInfo;
import net.gitsaibot.af.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AfWorker extends Worker {

    private static final String TAG = "AfWorker";

    public static final String KEY_ACTION = "ACTION";
    public static final String KEY_WIDGET_URI = "WIDGET_URI";

    // Actions
	public final static String ACTION_DELETE_WIDGET = "net.gitsaibot.af.DELETE_WIDGET";
	public final static String ACTION_UPDATE_ALL = "net.gitsaibot.af.UPDATE_ALL";
	public final static String ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS = "net.gitsaibot.af.UPDATE_WIDGET_MINIMAL_DIMENSIONS";
	public final static String ACTION_UPDATE_ALL_PROVIDER_AUTO = "net.gitsaibot.af.UPDATE_WIDGET_PROVIDER_AUTO";
	public final static String ACTION_UPDATE_ALL_PROVIDER_CHANGE = "net.gitsaibot.af.UPDATE_WIDGET_PROVIDER_CHANGE";
	public final static String ACTION_UPDATE_WIDGET = "net.gitsaibot.af.UPDATE_WIDGET";

	public final static String ACTION_DECREASE_LANDSCAPE_HEIGHT = "net.gitsaibot.af.DECREASE_LANDSCAPE_HEIGHT";
	public final static String ACTION_DECREASE_LANDSCAPE_WIDTH = "net.gitsaibot.af.DECREASE_LANDSCAPE_WIDTH";
	public final static String ACTION_DECREASE_PORTRAIT_HEIGHT = "net.gitsaibot.af.DECREASE_PORTRAIT_HEIGHT";
	public final static String ACTION_DECREASE_PORTRAIT_WIDTH = "net.gitsaibot.af.DECREASE_PORTRAIT_WIDTH";
	public final static String ACTION_INCREASE_LANDSCAPE_HEIGHT = "net.gitsaibot.af.INCREASE_LANDSCAPE_HEIGHT";
	public final static String ACTION_INCREASE_LANDSCAPE_WIDTH = "net.gitsaibot.af.INCREASE_LANDSCAPE_WIDTH";
	public final static String ACTION_INCREASE_PORTRAIT_HEIGHT = "net.gitsaibot.af.INCREASE_PORTRAIT_HEIGHT";
	public final static String ACTION_INCREASE_PORTRAIT_WIDTH = "net.gitsaibot.af.INCREASE_PORTRAIT_WIDTH";

	public final static String ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION = "net.gitsaibot.af.ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION";
	public final static String ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION = "net.gitsaibot.af.ACCEPT_PORTRAIT_VERTICAL_CALIBRATION";
	public final static String ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION = "net.gitsaibot.af.ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION";
	public final static String ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION = "net.gitsaibot.af.ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION";

	Map<String, Pair<String, Integer>> mCalibrationAdjustmentsMap = new HashMap<>() {{
		put(ACTION_DECREASE_LANDSCAPE_HEIGHT, new Pair<>(LANDSCAPE_HEIGHT, -1));
		put(ACTION_INCREASE_LANDSCAPE_HEIGHT, new Pair<>(LANDSCAPE_HEIGHT, +1));
		put(ACTION_DECREASE_LANDSCAPE_WIDTH, new Pair<>(LANDSCAPE_WIDTH, -1));
		put(ACTION_INCREASE_LANDSCAPE_WIDTH, new Pair<>(LANDSCAPE_WIDTH, +1));
		put(ACTION_DECREASE_PORTRAIT_HEIGHT, new Pair<>(PORTRAIT_HEIGHT, -1));
		put(ACTION_INCREASE_PORTRAIT_HEIGHT, new Pair<>(PORTRAIT_HEIGHT, +1));
		put(ACTION_DECREASE_PORTRAIT_WIDTH, new Pair<>(PORTRAIT_WIDTH, -1));
		put(ACTION_INCREASE_PORTRAIT_WIDTH, new Pair<>(PORTRAIT_WIDTH, +1));
	}};

	Map<String, String> mCalibrationAcceptActionsMap = new HashMap<>() {{
		put(ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION, PORTRAIT_WIDTH);
		put(ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION, PORTRAIT_HEIGHT);
		put(ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION, LANDSCAPE_WIDTH);
		put(ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION, LANDSCAPE_HEIGHT);
	}};

    public AfWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String action = inputData.getString(KEY_ACTION);
        String widgetUriString = inputData.getString(KEY_WIDGET_URI);

        if (action == null) {
            return Result.failure();
        }

        Uri widgetUri = null;
        if (widgetUriString != null) {
            widgetUri = Uri.parse(widgetUriString);
        }

        Log.d(TAG, "doWork() " + action + " " + widgetUri);

        handleWork(action, widgetUri);

        return Result.success();
    }

    private void handleWork(String action, Uri widgetUri) {
		if (	action.equals(ACTION_UPDATE_WIDGET) ||
				mCalibrationAdjustmentsMap.containsKey(action) ||
				mCalibrationAcceptActionsMap.containsKey(action))
		{
			updateWidget(action, widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL)) {
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_PROVIDER_CHANGE))
		{
			AfUtils.clearProviderData(getApplicationContext().getContentResolver());
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_PROVIDER_AUTO))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt(getApplicationContext().getString(R.string.provider_string), AfUtils.PROVIDER_AUTO);
			editor.apply();

			AfUtils.clearProviderData(getApplicationContext().getContentResolver());

			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(getApplicationContext().getString(R.string.useDeviceSpecificDimensions_bool), false);
			editor.apply();

			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_DELETE_WIDGET))
		{
			int appWidgetId = (int)ContentUris.parseId(widgetUri);
			AfUtils.deleteWidget(getApplicationContext(), appWidgetId);
		}
		else {
			Log.d(TAG, "onHandleIntent() called with unhandled action (" + action + ")");
		}
	}

    private void updateWidget(String action, Uri widgetUri)
	{
		int appWidgetId = (int)ContentUris.parseId(widgetUri);
        Context context = getApplicationContext();

		AfWidgetInfo widgetInfo;
		try {
			widgetInfo = AfWidgetInfo.build(context, widgetUri);
			widgetInfo.loadSettings(context);
		} catch (Exception e) {
			PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(context, widgetUri);
			AfUtils.updateWidgetRemoteViews(context, appWidgetId, "Failed to get widget information", true, pendingIntent);
			Log.d(TAG, "onHandleIntent() failed: Could not retrieve widget information (" + e.getMessage() + ")");
			return;
		}

		AfSettings afSettings = AfSettings.build(context, widgetInfo);
		afSettings.loadSettings();

		int calibrationTarget = afSettings.getCalibrationTarget();

		if (calibrationTarget == widgetInfo.getAppWidgetId()) {
			calibrationMethod(widgetInfo, afSettings, action);
		} else {
			try {
				AfUpdate afUpdate = AfUpdate.build(context, widgetInfo, afSettings);
				afUpdate.process();
			} catch (Exception e) {
				PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(context, widgetUri);
				AfUtils.updateWidgetRemoteViews(context, appWidgetId, "Failed to update widget", true, pendingIntent);
				Log.d(TAG, "AfUpdate of " + widgetUri + " failed! (" + e.getMessage() + ")");
				e.printStackTrace();
			}
		}
	}

	private void updateAllWidgets(Uri widgetUri)
	{
        Context context = getApplicationContext();
		AfSettings.clearAllWidgetStates(PreferenceManager.getDefaultSharedPreferences(context));

		// Update all widgets except widgetUri
		int widgetIdExclude = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (widgetUri != null)
		{
			widgetIdExclude = (int)ContentUris.parseId(widgetUri);
			updateWidget(ACTION_UPDATE_WIDGET, widgetUri);
		}

		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, AfWidget.class));
		for (int appWidgetId : appWidgetIds) {
			if (appWidgetId != widgetIdExclude)
			{
                AfWorkManager.enqueueWork(context, new Intent(ACTION_UPDATE_WIDGET).setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId)));
			}
		}
	}

	private void calibrationMethod(AfWidgetInfo widgetInfo, AfSettings afSettings, String action) {
        Context context = getApplicationContext();
		Log.d(TAG, "calibrationMethod() " + action);

		if (mCalibrationAcceptActionsMap.containsKey(action)) {
			String property = mCalibrationAcceptActionsMap.get(action);

			afSettings.saveCalibratedDimension(property);

			if (	action.equals(ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION) ||
					action.equals(ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION))
			{
				afSettings.setCalibrationState(CALIBRATION_STATE_VERTICAL);

				// Update relevant widget only, still calibrating
                AfWorkManager.enqueueWork(context, new Intent(ACTION_UPDATE_WIDGET).setData(widgetInfo.getWidgetUri()));
			} else {
				afSettings.setCalibrationState(CALIBRATION_STATE_FINISHED);
				afSettings.exitCalibrationMode();

				PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(context, widgetInfo.getWidgetUri());
				AfUtils.updateWidgetRemoteViews(context, widgetInfo.getAppWidgetId(), context.getString(R.string.widget_loading), true, pendingIntent);

				// Update all widgets after ended calibration
                AfWorkManager.enqueueWork(context, new Intent(ACTION_UPDATE_ALL).setData(widgetInfo.getWidgetUri()));
			}
		} else {
			Pair<String, Integer> adjustParams = mCalibrationAdjustmentsMap.get(action);

			if (adjustParams != null) {
				afSettings.adjustCalibrationDimension(adjustParams.first, adjustParams.second);
			}

			try {
				setupCalibrationWidget(widgetInfo, afSettings);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Bitmap renderCalibrationBitmap(int width, int height, boolean vertical) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		Paint p = new Paint() {{
			setColor(Color.BLACK);
			setStrokeWidth(0);
		}};

		if (vertical) {
			for (int y = 0; y < height; y += 2) {
				canvas.drawLine(0.0f, (float)y, (float)width, (float)y, p);
			}
		} else {
			for (int x = 0; x < width; x += 2) {
				canvas.drawLine((float)x, 0.0f, (float)x, (float)height, p);
			}
		}

		return bitmap;
	}

	private void setupCalibrationWidget(AfWidgetInfo afWidgetInfo, AfSettings afSettings) throws IOException {
        Context context = getApplicationContext();
		int calibrationState = afSettings.getCalibrationState();
		boolean vertical = (calibrationState == AfSettings.CALIBRATION_STATE_VERTICAL);

		int appWidgetId = afWidgetInfo.getAppWidgetId();
		Uri widgetUri = afWidgetInfo.getWidgetUri();

		Point portraitDimensions = afSettings.getCalibrationPixelDimensionsOrStandard(false);
		Point landscapeDimensions = afSettings.getCalibrationPixelDimensionsOrStandard(true);

		Bitmap portraitBitmap = renderCalibrationBitmap(portraitDimensions.x, portraitDimensions.y, vertical);
		Bitmap landscapeBitmap = renderCalibrationBitmap(landscapeDimensions.x, landscapeDimensions.y, vertical);

		long now = System.currentTimeMillis();

		Uri portraitUri = AfUtils.storeBitmap(context, portraitBitmap, appWidgetId, now, false);
		Uri landscapeUri = AfUtils.storeBitmap(context, landscapeBitmap, appWidgetId, now, true);

		RemoteViews updateView = new RemoteViews(context.getPackageName(), R.layout.af_calibrate);

		setupPendingIntent(updateView, widgetUri, R.id.landscape_decrease, vertical ? ACTION_DECREASE_LANDSCAPE_HEIGHT : ACTION_DECREASE_LANDSCAPE_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.landscape_increase, vertical ? ACTION_INCREASE_LANDSCAPE_HEIGHT : ACTION_INCREASE_LANDSCAPE_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_decrease,  vertical ? ACTION_DECREASE_PORTRAIT_HEIGHT  : ACTION_DECREASE_PORTRAIT_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_increase,  vertical ? ACTION_INCREASE_PORTRAIT_HEIGHT  : ACTION_INCREASE_PORTRAIT_WIDTH);

		setupPendingIntent(updateView, widgetUri, R.id.landscape_accept,   vertical ? ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION : ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_accept,    vertical ? ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION  : ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION);

		updateView.setTextViewText(R.id.portraitText, "Portrait/" + (vertical ? "Vertical" : "Horizontal") + "\n" + portraitDimensions.x + "x" + portraitDimensions.y);
		updateView.setTextViewText(R.id.landscapeText, "Landscape/" + (vertical ? "Vertical" : "Horizontal") + "\n" + landscapeDimensions.x + "x" + landscapeDimensions.y);

		updateView.setImageViewUri(R.id.landscapeCalibrationImage, landscapeUri);
		updateView.setImageViewUri(R.id.portraitCalibrationImage, portraitUri);

		AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateView);
	}

	private void setupPendingIntent(RemoteViews remoteViews, Uri widgetUri, int resource, String action) {
        Context context = getApplicationContext();
		Intent intent = new Intent(action, widgetUri, context, AfServiceReceiver.class);
		remoteViews.setOnClickPendingIntent(resource, PendingIntent.getBroadcast(context, 0, intent, AfUtils.PI_FLAG_IMMUTABLE));
	}
}
