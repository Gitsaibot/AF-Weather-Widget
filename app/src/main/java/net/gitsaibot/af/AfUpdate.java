package net.gitsaibot.af;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import net.gitsaibot.af.data.AfDataUpdateException;
import net.gitsaibot.af.data.AfGeoNamesData;
import net.gitsaibot.af.data.AfMetSunTimeData;
import net.gitsaibot.af.data.AfMetWeatherData;
import net.gitsaibot.af.data.AfNoaaWeatherData;
import net.gitsaibot.af.util.AfLocationInfo;
import net.gitsaibot.af.util.AfViewInfo;
import net.gitsaibot.af.util.AfWidgetInfo;
import net.gitsaibot.af.widget.AfDetailedWidget;
import net.gitsaibot.af.widget.AfWidgetDataException;
import net.gitsaibot.af.widget.AfWidgetDrawException;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class AfUpdate {
	
	private final static String TAG = "AfUpdate";
	
	public static final int WIDGET_STATE_MESSAGE = 1;
	public static final int WIDGET_STATE_RENDER = 2;
	
	private long mCurrentUtcTime;
	
	private Context mContext;
	
	private Uri mWidgetUri;
	
	private TimeZone mUtcTimeZone;
	
	private AfWidgetInfo mAfWidgetInfo;
	private AfSettings mAfSettings;
	
	private AfUpdate(Context context, AfWidgetInfo afWidgetInfo, AfSettings afSettings) {
		mContext = context;
		mAfWidgetInfo = afWidgetInfo;
		mWidgetUri = afWidgetInfo.getWidgetUri();
		mAfSettings = afSettings;
		
		mUtcTimeZone = TimeZone.getTimeZone("UTC");
	}
	
	public static AfUpdate build(Context context, AfWidgetInfo afWidgetInfo, AfSettings afSettings)
	{
		return new AfUpdate(context, afWidgetInfo, afSettings);
	}
	
	public void process() throws Exception {
		if (mAfWidgetInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing widget info.");
			return;
		}
		
		AfViewInfo afViewInfo = mAfWidgetInfo.getViewInfo();
		if (afViewInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing view info.");
			return;
		}
		
		AfLocationInfo afLocationInfo = afViewInfo.getLocationInfo();
		if (afLocationInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing location info.");
			return;
		}
		
		Log.d(TAG, "process(): Started processing. " + mAfWidgetInfo.toString());
		
		mCurrentUtcTime = Calendar.getInstance(mUtcTimeZone).getTimeInMillis(); 

		final int totalNumAttempts = 3;
		int numAttemptsRemaining = totalNumAttempts;
		boolean updateSuccess = false, drawSuccess = false;

		boolean shouldUpdate = isDataUpdateNeeded(afLocationInfo);
		boolean isWifiConnectionMissing = false;
		boolean isRateLimited = false;
		
		do {
			if (numAttemptsRemaining != totalNumAttempts) {
				updateWidgetRemoteViews("Delay before attempt #" + (totalNumAttempts - numAttemptsRemaining + 1), false);
				Thread.sleep(10000);
			}
			
			if (shouldUpdate) {
				if (!mAfSettings.getCachedWifiOnly() || isWiFiAvailable()) {
					try {
						updateData(afLocationInfo);
						updateSuccess = true;
						isWifiConnectionMissing = false;
					}
					catch (AfDataUpdateException e) {
						if (e.reason == AfDataUpdateException.Reason.RATE_LIMITED) {
							isRateLimited = true;
							numAttemptsRemaining = 0;
						}
						Log.d(TAG, String.format("update() failed: %s", e.toString()));
					}
					catch (Exception e) {
						Log.d(TAG, String.format("update() failed: %s", e.toString()));
					}
				} else {
					isWifiConnectionMissing = true;
				}
			}
			
			try {
				AfDetailedWidget widget = AfDetailedWidget.build(mContext, mAfWidgetInfo, afLocationInfo);
				updateWidgetRemoteViews(widget);
				drawSuccess = true;
			}
			catch (AfWidgetDrawException e) {
				Log.d(TAG, "process(): Failed to draw widget. AixWidgetDrawException=" + e.getMessage());
				e.printStackTrace();
				break;
			}
			catch (AfWidgetDataException e)
			{
				Log.d(TAG, "process(): Failed to draw widget. AixWidgetDataException=" + e.getMessage());
				e.printStackTrace();

				if (updateSuccess)
				{
					updateSuccess = false;
					break;
				}
				else
				{
					shouldUpdate = true;
				}
			}
			catch (Exception e) {
				Log.d(TAG, "process(): Failed to draw widget. Exception=" + e.getMessage());
				e.printStackTrace();
				
				shouldUpdate = true;
			}
		} while (!drawSuccess && (shouldUpdate && !updateSuccess) && (--numAttemptsRemaining > 0));
		
		long updateTime = Long.MAX_VALUE;

		if (shouldUpdate && !updateSuccess) {
			if (mAfSettings.getCachedWifiOnly()) {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				Editor editor = settings.edit();
				editor.putBoolean("global_needwifi", true);
				editor.apply();

				Log.d(TAG, "WiFi needed, but not connected!");
			}

			clearUpdateTimestamps(afLocationInfo);
			updateTime = Math.min(updateTime,
				System.currentTimeMillis()
				+ 10 * DateUtils.MINUTE_IN_MILLIS
				+ Math.round((float)(20 * 60) * Math.random()) * DateUtils.SECOND_IN_MILLIS);
		}

		if (!drawSuccess) {
			if (shouldUpdate && !updateSuccess) {
				if (mAfSettings.getCachedProvider() == AfUtils.PROVIDER_NWS && !isLocationInUS(afLocationInfo)) {
					PendingIntent pendingIntent = AfUtils.buildWidgetProviderAutoIntent(mContext, mWidgetUri);
					AfUtils.updateWidgetRemoteViews(mContext, mAfWidgetInfo.getAppWidgetId(), "NWS source cannot be used outside US.\nTap widget to revert to auto", true, pendingIntent);
				} else if (isRateLimited) {
					updateWidgetRemoteViews("API is currently rate limited", true);
				} else if (isWifiConnectionMissing) {
					updateWidgetRemoteViews("WiFi is required and missing", true);
				} else {
					updateWidgetRemoteViews("Failed to get weather data", true);
				}
			} else {
				if (mAfSettings.getCachedUseSpecificDimensions()) {
					PendingIntent pendingIntent = AfUtils.buildDisableSpecificDimensionsIntent(mContext, mWidgetUri);
					AfUtils.updateWidgetRemoteViews(mContext, mAfWidgetInfo.getAppWidgetId(), "Draw failed!\nTap widget to revert to minimal dimensions", true, pendingIntent);
				} else {
					updateWidgetRemoteViews("Failed to draw widget", true);
				}
			}
		}

		scheduleUpdate(updateTime);
		
		Log.d(TAG, "process() ended!");
	}
	
	private boolean isWiFiAvailable() {
		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		if (wifiInfo != null && wifiInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isLocationInUS(AfLocationInfo locationInfo) {
		String widgetCountryCode = "global_lcountry_" + locationInfo.getId();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean isUS = settings.getString(widgetCountryCode, "").equalsIgnoreCase("us");
		return isUS;
	}
	
	private void scheduleUpdate(long updateTime) {
		Calendar calendar = Calendar.getInstance();
		AfUtils.truncateHour(calendar);
		calendar.add(Calendar.HOUR, 1);
		
		// Add random interval to spread traffic
		calendar.add(Calendar.SECOND, (int)Math.round(180.0f * Math.random()));
		
		updateTime = Math.min(updateTime, calendar.getTimeInMillis());

		Intent updateIntent = new Intent(AfService.ACTION_UPDATE_WIDGET, mWidgetUri, mContext, AfServiceReceiver.class);
		PendingIntent pendingUpdateIntent = PendingIntent.getBroadcast(mContext, 0, updateIntent, 0);
		
		boolean awakeOnly = mAfSettings.getCachedAwakeOnly();
		
		AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(awakeOnly ? AlarmManager.RTC : AlarmManager.RTC_WAKEUP, updateTime, pendingUpdateIntent);
		Log.d(TAG, "Scheduling next update for: " + (new SimpleDateFormat().format(updateTime)) + " AwakeOnly=" + awakeOnly);
	}
	
	private Uri renderWidget(AfDetailedWidget widget, boolean isLandscape) throws AfWidgetDrawException, IOException {
		Point dimensions;
		
		if (mAfSettings.getCachedUseSpecificDimensions()) {
			dimensions = mAfSettings.getPixelDimensionsOrStandard(isLandscape);
		} else {
			dimensions = mAfSettings.getStandardPixelDimensions(mAfWidgetInfo.getNumColumns(), mAfWidgetInfo.getNumRows(), isLandscape, true);
		}

		Bitmap bitmap = widget.render(dimensions.x, dimensions.y, isLandscape);
		return AfUtils.storeBitmap(mContext, bitmap, mAfWidgetInfo.getAppWidgetId(), mCurrentUtcTime, isLandscape);
	}
	
	private void updateWidgetRemoteViews(AfDetailedWidget afDetailedWidget) throws AfWidgetDrawException, IOException
	{
		int orientationMode = mAfSettings.getCachedOrientationMode();
		
		RemoteViews updateView;
		
		if (orientationMode == AfUtils.ORIENTATION_PORTRAIT_FIXED) {
			Uri portraitUri = renderWidget(afDetailedWidget, false);
			updateView = new RemoteViews(mContext.getPackageName(),
					mAfSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom_fixed
					: R.layout.widget_large_tiny_portrait);
			updateView.setImageViewUri(R.id.widgetImage, portraitUri);
			Log.d(TAG, "Updating portrait mode");
		}
		else if (orientationMode == AfUtils.ORIENTATION_LANDSCAPE_FIXED)
		{
			Uri landscapeUri = renderWidget(afDetailedWidget, true);
			updateView = new RemoteViews(mContext.getPackageName(),
					mAfSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom_fixed
					: R.layout.widget_large_tiny_landscape);
			updateView.setImageViewUri(R.id.widgetImage, landscapeUri);
			Log.d(TAG, "Updating landscape mode");
		}
		else
		{
			Uri portraitUri = renderWidget(afDetailedWidget, false);
			Uri landscapeUri = renderWidget(afDetailedWidget, true);
			
			updateView = new RemoteViews(mContext.getPackageName(),
					mAfSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom
					: R.layout.widget_large_tiny);
			
			updateView.setImageViewUri(R.id.widgetImagePortrait, portraitUri);
			updateView.setImageViewUri(R.id.widgetImageLandscape, landscapeUri);
			
			Log.d(TAG, "Updating both portrait and landscape mode");
		}
		
		mAfSettings.setWidgetState(WIDGET_STATE_RENDER);
		
		PendingIntent configurationIntent = AfUtils.buildConfigurationIntent(mContext, mAfWidgetInfo.getWidgetUri());
		updateView.setOnClickPendingIntent(R.id.widgetContainer, configurationIntent);
		AppWidgetManager.getInstance(mContext).updateAppWidget(mAfWidgetInfo.getAppWidgetId(), updateView);
	}
	
	private boolean isDataUpdateNeeded(AfLocationInfo locationInfo) {
		if (locationInfo.getLastForecastUpdate() == null ||
				locationInfo.getForecastValidTo() == null ||
				locationInfo.getNextForecastUpdate() == null)
		{
			return true;
		}
		
		boolean shouldUpdate = false;
		int updateHours = mAfSettings.getCachedNumUpdateHours();
		
		if (updateHours == 0) {
			if (	   (mCurrentUtcTime >= locationInfo.getLastForecastUpdate() + DateUtils.MINUTE_IN_MILLIS)
					&& (mCurrentUtcTime >= locationInfo.getNextForecastUpdate() || locationInfo.getForecastValidTo() < mCurrentUtcTime))
			{
				shouldUpdate = true;
			}
		} else {
			if (mCurrentUtcTime >= locationInfo.getLastForecastUpdate() + updateHours * DateUtils.HOUR_IN_MILLIS) {
				shouldUpdate = true;
			}
		}
		return shouldUpdate;
	}
	
	private void clearUpdateTimestamps(AfLocationInfo locationInfo) {
		locationInfo.setLastForecastUpdate(null);
		locationInfo.setForecastValidTo(null);
		locationInfo.setNextForecastUpdate(null);
		locationInfo.commit(mContext);
	}
	
	public void updateWidgetRemoteViews(String message, boolean overwrite)
	{
		PendingIntent configurationIntent = AfUtils.buildConfigurationIntent(mContext, mAfWidgetInfo.getWidgetUri());
		AfUtils.updateWidgetRemoteViews(mContext, mAfWidgetInfo.getAppWidgetId(), message, overwrite, configurationIntent);
	}
	
	private void updateData(AfLocationInfo afLocationInfo) throws AfDataUpdateException {
		Log.d(TAG, "updateData() started uri=" + afLocationInfo.getLocationUri());

		AfUtils.clearOldProviderData(mContext.getContentResolver());
		AfGeoNamesData.build(mContext, this, mAfSettings).update(afLocationInfo, mCurrentUtcTime);
		AfMetSunTimeData.build(mContext, this, mAfSettings).update(afLocationInfo, mCurrentUtcTime);

		int provider = mAfSettings.getCachedProvider();

		if ((provider == AfUtils.PROVIDER_AUTO && isLocationInUS(afLocationInfo)) || provider == AfUtils.PROVIDER_NWS) {
			AfNoaaWeatherData.build(mContext, this, mAfSettings).update(afLocationInfo, mCurrentUtcTime);
		} else {
			AfMetWeatherData.build(mContext, this, mAfSettings).update(afLocationInfo, mCurrentUtcTime);
		}
	}
	
}
