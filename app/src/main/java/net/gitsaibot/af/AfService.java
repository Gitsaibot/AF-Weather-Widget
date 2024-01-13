package net.gitsaibot.af;

import static net.gitsaibot.af.AfSettings.CALIBRATION_STATE_FINISHED;
import static net.gitsaibot.af.AfSettings.CALIBRATION_STATE_VERTICAL;
import static net.gitsaibot.af.AfSettings.LANDSCAPE_HEIGHT;
import static net.gitsaibot.af.AfSettings.LANDSCAPE_WIDTH;
import static net.gitsaibot.af.AfSettings.PORTRAIT_HEIGHT;
import static net.gitsaibot.af.AfSettings.PORTRAIT_WIDTH;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.util.AfWidgetInfo;
import net.gitsaibot.af.util.Pair;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import androidx.core.app.JobIntentService;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

public class AfService extends JobIntentService {
	
	private static final String TAG = "AfService";

	public static final int JOB_ID = 1;

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

	public static void enqueueWork(Context context, Intent work) {
		enqueueWork(context, AfService.class, JOB_ID, work);
	}

	@Override
	protected void onHandleWork(Intent intent) {
		Log.d(TAG, "onHandleIntent() " + intent.getAction() + " " + intent.getData());
		
		String action = intent.getAction();
		Uri widgetUri = intent.getData();
		
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
			AfUtils.clearProviderData(getContentResolver());
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_PROVIDER_AUTO))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = sharedPreferences.edit();
			editor.putInt(getString(R.string.provider_string), AfUtils.PROVIDER_AUTO);
			editor.apply();
			
			AfUtils.clearProviderData(getContentResolver());
			
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(getString(R.string.useDeviceSpecificDimensions_bool), false);
			editor.apply();

			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_DELETE_WIDGET))
		{
			int appWidgetId = (int)ContentUris.parseId(widgetUri);
			AfUtils.deleteWidget(this, appWidgetId);
		}
		else {
			Log.d(TAG, "onHandleIntent() called with unhandled action (" + action + ")");
		}
	}
	
	private void updateWidget(String action, Uri widgetUri)
	{
		int appWidgetId = (int)ContentUris.parseId(widgetUri);
		
		AfWidgetInfo widgetInfo;
		try {
			widgetInfo = AfWidgetInfo.build(this, widgetUri);
			widgetInfo.loadSettings(this);
		} catch (Exception e) {
			PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(this, widgetUri);
			AfUtils.updateWidgetRemoteViews(this, appWidgetId, "Failed to get widget information", true, pendingIntent);
			Log.d(TAG, "onHandleIntent() failed: Could not retrieve widget information (" + e.getMessage() + ")");
			return;
		}
		
		AfSettings afSettings = AfSettings.build(this, widgetInfo);
		afSettings.loadSettings();
		
		int calibrationTarget = afSettings.getCalibrationTarget();
		
		if (calibrationTarget == widgetInfo.getAppWidgetId()) {
			calibrationMethod(widgetInfo, afSettings, action);
		} else {
			try {
				AfUpdate afUpdate = AfUpdate.build(this, widgetInfo, afSettings);
				afUpdate.process();
			} catch (Exception e) {
				PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(this, widgetUri);
				AfUtils.updateWidgetRemoteViews(this, appWidgetId, "Failed to update widget", true, pendingIntent);
				Log.d(TAG, "AfUpdate of " + widgetUri + " failed! (" + e.getMessage() + ")");
				e.printStackTrace();
			}
		}
	}
	
	private void updateAllWidgets(Uri widgetUri)
	{
		AfSettings.clearAllWidgetStates(PreferenceManager.getDefaultSharedPreferences(this));
		
		// Update all widgets except widgetUri
		int widgetIdExclude = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (widgetUri != null)
		{
			widgetIdExclude = (int)ContentUris.parseId(widgetUri);
			
			
			updateWidget(ACTION_UPDATE_WIDGET, widgetUri);
		}
		
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, AfWidget.class));
		for (int appWidgetId : appWidgetIds) {
			if (appWidgetId != widgetIdExclude)
			{
				Intent updateIntent = new Intent(
						ACTION_UPDATE_WIDGET,
						ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId),
						this, AfService.class);
				AfService.enqueueWork(getApplicationContext(), updateIntent);
			}
		}
	}
	
	private void calibrationMethod(AfWidgetInfo widgetInfo, AfSettings afSettings, String action) {
		Log.d(TAG, "calibrationMethod() " + action);
		
		if (mCalibrationAcceptActionsMap.containsKey(action)) {
			String property = mCalibrationAcceptActionsMap.get(action);
			
			afSettings.saveCalibratedDimension(property);
			
			if (	action.equals(ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION) ||
					action.equals(ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION))
			{
				afSettings.setCalibrationState(CALIBRATION_STATE_VERTICAL);
				
				// Update relevant widget only, still calibrating
				Intent updateIntent = new Intent(ACTION_UPDATE_WIDGET, widgetInfo.getWidgetUri(), this, AfService.class);
				AfService.enqueueWork(getApplicationContext(), updateIntent);
			} else {
				afSettings.setCalibrationState(CALIBRATION_STATE_FINISHED);
				afSettings.exitCalibrationMode();
				
				PendingIntent pendingIntent = AfUtils.buildConfigurationIntent(this, widgetInfo.getWidgetUri());
				AfUtils.updateWidgetRemoteViews(this, widgetInfo.getAppWidgetId(), getString(R.string.widget_loading), true, pendingIntent);
				
				// Update all widgets after ended calibration
				Intent updateIntent = new Intent(ACTION_UPDATE_ALL, widgetInfo.getWidgetUri(), this, AfService.class);
				AfService.enqueueWork(getApplicationContext(), updateIntent);
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
		int calibrationState = afSettings.getCalibrationState();
		boolean vertical = (calibrationState == AfSettings.CALIBRATION_STATE_VERTICAL);
		
		int appWidgetId = afWidgetInfo.getAppWidgetId();
		Uri widgetUri = afWidgetInfo.getWidgetUri();
		
		Point portraitDimensions = afSettings.getCalibrationPixelDimensionsOrStandard(false);
		Point landscapeDimensions = afSettings.getCalibrationPixelDimensionsOrStandard(true);
		
		Bitmap portraitBitmap = renderCalibrationBitmap(portraitDimensions.x, portraitDimensions.y, vertical);
		Bitmap landscapeBitmap = renderCalibrationBitmap(landscapeDimensions.x, landscapeDimensions.y, vertical);
		
		long now = System.currentTimeMillis();
		
		Uri portraitUri = AfUtils.storeBitmap(this, portraitBitmap, appWidgetId, now, false);
		Uri landscapeUri = AfUtils.storeBitmap(this, landscapeBitmap, appWidgetId, now, true);
		
		RemoteViews updateView = new RemoteViews(getPackageName(), R.layout.af_calibrate);
		
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
		
		AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId, updateView);
	}
	
	private void setupPendingIntent(RemoteViews remoteViews, Uri widgetUri, int resource, String action) {
		Intent intent = new Intent(action, widgetUri, this, AfServiceReceiver.class);
		remoteViews.setOnClickPendingIntent(resource, PendingIntent.getBroadcast(this, 0, intent, AfUtils.PI_FLAG_IMMUTABLE));
	}

}
