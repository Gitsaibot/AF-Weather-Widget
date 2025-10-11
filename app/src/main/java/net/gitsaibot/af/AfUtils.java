package net.gitsaibot.af;

import static java.util.Locale.US;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfLocationsColumns;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfProvider.AfSunMoonData;
import net.gitsaibot.af.AfProvider.AfViews;
import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.AfProvider.AfWidgetsColumns;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

public class AfUtils {
	
	private final static String TAG = "AfUtils";
	
	public static final int ORIENTATION_NORMAL = 0;
	public static final int ORIENTATION_PORTRAIT_FIXED = 1;
	public static final int ORIENTATION_LANDSCAPE_FIXED = 2;
	
	public static final int WIDGET_STATE_NONE = 0;
	public static final int WIDGET_STATE_MESSAGE = 1;
	public static final int WIDGET_STATE_RENDER = 2;
	
	public final static int PROVIDER_AUTO = 1;
	public final static int PROVIDER_NMET = 2;
	public final static int PROVIDER_NWS = 3;

	public static final int PI_FLAG_IMMUTABLE = PendingIntent.FLAG_IMMUTABLE;

	public static final int[] WEATHER_ICONS_DAY = {
		R.drawable.weather_icon_clearsky_day,
		R.drawable.weather_icon_fair_day,
		R.drawable.weather_icon_partlycloudy_day,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_rainshowers_day,
		R.drawable.weather_icon_rainshowersandthunder_day,
		R.drawable.weather_icon_sleetshowers_day,
		R.drawable.weather_icon_snowshowers_day,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_heavyrain,
		R.drawable.weather_icon_heavyrainandthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowandthunder,
		R.drawable.weather_icon_fog,
		0, // 16 placeholder
		0, // 17 placeholder
		0, // 18 placeholder
		0, // 19 placeholder
		R.drawable.weather_icon_sleetshowersandthunder_day,
		R.drawable.weather_icon_snowshowersandthunder_day,
		R.drawable.weather_icon_rainandthunder,
		R.drawable.weather_icon_sleetandthunder,
		R.drawable.weather_icon_lightrainshowersandthunder_day,
		R.drawable.weather_icon_heavyrainshowersandthunder_day,
		R.drawable.weather_icon_lightsleetshowersandthunder_day,
		R.drawable.weather_icon_heavysleetshowersandthunder_day,
		R.drawable.weather_icon_lightsnowshowersandthunder_day,
		R.drawable.weather_icon_heavysnowshowersandthunder_day,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder,
		R.drawable.weather_icon_heavysleetandthunder,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_heavysnowandthunder,
		0, // 35 placeholder
		0, // 36 placeholder
		0, // 37 placeholder
		0, // 38 placeholder
		0, // 39 placeholder
		R.drawable.weather_icon_lightrainshowers_day,
		R.drawable.weather_icon_heavyrainshowers_day,
		R.drawable.weather_icon_lightsleetshowers_day,
		R.drawable.weather_icon_heavysleetshowers_day,
		R.drawable.weather_icon_lightsnowshowers_day,
		R.drawable.weather_icon_heavysnowshowers_day,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_heavysleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_heavysnow,
	};

	public static final int[] WEATHER_ICONS_NIGHT = {
		R.drawable.weather_icon_clearsky_night,
		R.drawable.weather_icon_fair_night,
		R.drawable.weather_icon_partlycloudy_night,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_rainshowers_night,
		R.drawable.weather_icon_rainshowersandthunder_night,
		R.drawable.weather_icon_sleetshowers_night,
		R.drawable.weather_icon_snowshowers_night,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_heavyrain,
		R.drawable.weather_icon_heavyrainandthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowandthunder,
		R.drawable.weather_icon_fog,
		0, // 16 placeholder
		0, // 17 placeholder
		0, // 18 placeholder
		0, // 19 placeholder
		R.drawable.weather_icon_sleetshowersandthunder_night,
		R.drawable.weather_icon_snowshowersandthunder_night,
		R.drawable.weather_icon_rainandthunder,
		R.drawable.weather_icon_sleetandthunder,
		R.drawable.weather_icon_lightrainshowersandthunder_night,
		R.drawable.weather_icon_heavyrainshowersandthunder_night,
		R.drawable.weather_icon_lightsleetshowersandthunder_night,
		R.drawable.weather_icon_heavysleetshowersandthunder_night,
		R.drawable.weather_icon_lightsnowshowersandthunder_night,
		R.drawable.weather_icon_heavysnowshowersandthunder_night,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder,
		R.drawable.weather_icon_heavysleetandthunder,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_heavysnowandthunder,
		0, // 35 placeholder
		0, // 36 placeholder
		0, // 37 placeholder
		0, // 38 placeholder
		0, // 39 placeholder
		R.drawable.weather_icon_lightrainshowers_night,
		R.drawable.weather_icon_heavyrainshowers_night,
		R.drawable.weather_icon_lightsleetshowers_night,
		R.drawable.weather_icon_heavysleetshowers_night,
		R.drawable.weather_icon_lightsnowshowers_night,
		R.drawable.weather_icon_heavysnowshowers_night,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_heavysleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_heavysnow,
	};

	public static final int[] WEATHER_ICONS_POLAR = {
		R.drawable.weather_icon_clearsky_polartwilight,
		R.drawable.weather_icon_fair_polartwilight,
		R.drawable.weather_icon_partlycloudy_polartwilight,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_rainshowers_polartwilight,
		R.drawable.weather_icon_rainshowersandthunder_polartwilight,
		R.drawable.weather_icon_sleetshowers_polartwilight,
		R.drawable.weather_icon_snowshowers_polartwilight,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_heavyrain,
		R.drawable.weather_icon_heavyrainandthunder,
		R.drawable.weather_icon_sleet,
		R.drawable.weather_icon_snow,
		R.drawable.weather_icon_snowandthunder,
		R.drawable.weather_icon_fog,
		0, // 16 placeholder
		0, // 17 placeholder
		0, // 18 placeholder
		0, // 19 placeholder
		R.drawable.weather_icon_sleetshowersandthunder_polartwilight,
		R.drawable.weather_icon_snowshowersandthunder_polartwilight,
		R.drawable.weather_icon_rainandthunder,
		R.drawable.weather_icon_sleetandthunder,
		R.drawable.weather_icon_lightrainshowersandthunder_polartwilight,
		R.drawable.weather_icon_heavyrainshowersandthunder_polartwilight,
		R.drawable.weather_icon_lightsleetshowersandthunder_polartwilight,
		R.drawable.weather_icon_heavysleetshowersandthunder_polartwilight,
		R.drawable.weather_icon_lightsnowshowersandthunder_polartwilight,
		R.drawable.weather_icon_heavysnowshowersandthunder_polartwilight,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder,
		R.drawable.weather_icon_heavysleetandthunder,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_heavysnowandthunder,
		0, // 35 placeholder
		0, // 36 placeholder
		0, // 37 placeholder
		0, // 38 placeholder
		0, // 39 placeholder
		R.drawable.weather_icon_lightrainshowers_polartwilight,
		R.drawable.weather_icon_heavyrainshowers_polartwilight,
		R.drawable.weather_icon_lightsleetshowers_polartwilight,
		R.drawable.weather_icon_heavysleetshowers_polartwilight,
		R.drawable.weather_icon_lightsnowshowers_polartwilight,
		R.drawable.weather_icon_heavysnowshowers_polartwilight,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_heavysleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_heavysnow,
	};

	public static final int WEATHER_ICON_CLEARSKY = 1;
	public static final int WEATHER_ICON_FAIR = 2;
	public static final int WEATHER_ICON_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_CLOUDY = 4;
	public static final int WEATHER_ICON_RAINSHOWERS = 5;
	public static final int WEATHER_ICON_RAINSHOWERANDTHUNDER = 6;
	public static final int WEATHER_ICON_SLEETSHOWERS = 7;
	public static final int WEATHER_ICON_SNOWSHOWERS = 8;
	public static final int WEATHER_ICON_RAIN = 9;
	public static final int WEATHER_ICON_HEAVYRAIN = 10;
	public static final int WEATHER_ICON_HEAVYRAINANDTHUNDER = 11;
	public static final int WEATHER_ICON_SLEET = 12;
	public static final int WEATHER_ICON_SNOW = 13;
	public static final int WEATHER_ICON_SNOWANDTHUNDER = 14;
	public static final int WEATHER_ICON_FOG = 15;
	public static final int WEATHER_ICON_SLEETSHOWERSANDTHUNDER = 20;
	public static final int WEATHER_ICON_SNOWSHOWERSANDTHUNDER = 21;
	public static final int WEATHER_ICON_RAINANDTHUNDER = 22;
	public static final int WEATHER_ICON_SLEETANDTHUNDER = 23;
	public static final int WEATHER_ICON_LIGHTRAINSHOWERSANDTHUNDER = 24;
	public static final int WEATHER_ICON_HEAVYRAINSHOWERSANDTHUNDER = 25;
	public static final int WEATHER_ICON_LIGHTSLEETSHOWERSANDTHUNDER = 26;
	public static final int WEATHER_ICON_HEAVYSLEETSHOWERSANDTHUNDER = 27;
	public static final int WEATHER_ICON_LIGHTSNOWSHOWERSANDTHUNDER = 28;
	public static final int WEATHER_ICON_HEAVYSNOWSHOWERSANDTHUNDER = 29;
	public static final int WEATHER_ICON_LIGHTRAINANDTHUNDER = 30;
	public static final int WEATHER_ICON_LIGHTSLEETANDTHUNDER = 31;
	public static final int WEATHER_ICON_HEAVYSLEETANDTHUNDER = 32;
	public static final int WEATHER_ICON_LIGHTSNOWANDTHUNDER = 33;
	public static final int WEATHER_ICON_HEAVYSNOWANDTHUNDER = 34;
	public static final int WEATHER_ICON_LIGHTRAINSHOWERS = 40;
	public static final int WEATHER_ICON_HEAVYRAINSHOWERS = 41;
	public static final int WEATHER_ICON_LIGHTSLEETSHOWERS = 42;
	public static final int WEATHER_ICON_HEAVYSLEETSHOWERS = 43;
	public static final int WEATHER_ICON_LIGHTSNOWSHOWERS = 44;
	public static final int WEATHER_ICON_HEAVYSNOWSHOWERS = 45;
	public static final int WEATHER_ICON_LIGHTRAIN = 46;
	public static final int WEATHER_ICON_LIGHTSLEET = 47;
	public static final int WEATHER_ICON_HEAVYSLEET = 48;
	public static final int WEATHER_ICON_LIGHTSNOW = 49;
	public static final int WEATHER_ICON_HEAVYSNOW = 50;

	private AfUtils() {
		
	}
	
	public final static int clamp(int value, int min, int max) {
		int result = value;
		if (min == max) {
			if (value != min) {
				result = min;
			}
		} else if (min < max) {
			if (value < min) {
				result = min;
			} else if (value > max) {
				result = max;
			}
		} else {
			result = clamp(value, max, min);
		}
		return result;
	}
	
	public final static long clamp(long value, long min, long max) {
		long result = value;
		if (min == max) {
			if (value != min) {
				result = min;
			}
		} else if (min < max) {
			if (value < min) {
				result = min;
			} else if (value > max) {
				result = max;
			}
		} else {
			result = clamp(value, max, min);
		}
		return result;
	}
	
	public final static float clamp(float value, float min, float max) {
		float result = value;
		if (min == max) {
			if (value != min) {
				result = min;
			}
		} else if (min < max) {
			if (value < min) {
				result = min;
			} else if (value > max) {
				result = max;
			}
		} else {
			result = clamp(value, max, min);
		}
		return result;
	}
	
	public static long lcap(long x, long c) {
		return Math.max(x, c);
	}
	
	public static long hcap(long x, long c) {
		return Math.min(x, c);
	}
	
	public static int lcap(int x, int c) {
		return Math.max(x, c);
	}
	
	public static int hcap(int x, int c) {
		return Math.min(x, c);
	}
	
	public static float lcap(float x, float c) {
		return Math.max(x, c);
	}
	
	public static float hcap(float x, float c) {
		return Math.min(x, c);
	}
	
	public static boolean isPrime(long n) {
		boolean prime = true;
		for (long i = 3; i <= Math.sqrt(n); i += 2)
			if (n % i == 0) {
				prime = false;
				break;
			}
		return (n % 2 != 0 && prime && n > 2) || n == 2;
	}
	
	public static Point buildDimension(String widthString, String heightString)
	{
		if (widthString == null) {
			throw new IllegalArgumentException("Width is null");
		} else if (TextUtils.isEmpty(widthString.trim())) {
			throw new IllegalArgumentException("Width is empty");
		}
		if (heightString == null) {
			throw new IllegalArgumentException("Height is null");
		} else if (TextUtils.isEmpty(heightString.trim())) {
			throw new IllegalArgumentException("Height is empty");
		}

		int width, height;
		
		try {
			width = Integer.parseInt(widthString);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Width is not a valid number");
		}
		if (width < 1 || width > 10000) {
			throw new IllegalArgumentException("Width is out of range");
		}
		
		try {
			height = Integer.parseInt(heightString);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Height is not a valid number");
		}
		if (height < 1 || height > 10000) {
			throw new IllegalArgumentException("Height is out of range");
		}
		
		return new Point(width, height);
	}
	
	public static String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try (is) {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, StandardCharsets.UTF_8));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			}
			return writer.toString();
		} else {
			return "";
		}
	}
	
	public static void deleteCacheFiles(Context context, int appWidgetId) {
		String appWidgetIdString = Integer.toString(appWidgetId);
		File[] cacheFileList = context.getCacheDir().listFiles();

		for (File file : cacheFileList)
		{
			String fileName = file.getName();

			if (fileName.startsWith("af"))
			{
				String[] s = fileName.split("_");

				if (s.length > 1 && s[1].equals(appWidgetIdString))
				{
					file.delete();
				}
			}
		}
	}
	
	/* Deletes all temporary files, as made with Context.openFileOutput(), matching appWidgetId */
	public static void deleteTemporaryFiles(Context context, int appWidgetId) {
		String appWidgetIdString = Integer.toString(appWidgetId);
		String[] fileNameList = context.fileList();

		for (String fileName : fileNameList)
		{
			if (fileName.startsWith("af"))
			{
				String[] s = fileName.split("_");
				if (s.length > 1 && s[1].equals(appWidgetIdString))
				{
					context.deleteFile(fileName);
				}
			}
		}
	}
	
	public static void deleteTemporaryFile(Context context, int appWidgetId, long updateTime, String orientation) {
		String appWidgetIdString = Integer.toString(appWidgetId);
		String[] fileNameList = context.fileList();

		for (String fileName : fileNameList)
		{
			if (fileName.startsWith("af"))
			{
				String[] s = fileName.split("_");

				if (s.length == 4)
				{
					try
					{
						long fileTime = Long.parseLong(s[2]);

						boolean isFileMatch = (s[1].equals(appWidgetIdString) && s[3].startsWith(orientation));
						boolean isFileObsolete = (fileTime < updateTime - 6 * DateUtils.HOUR_IN_MILLIS);

						if ((isFileMatch && fileTime < updateTime) || isFileObsolete)
						{
							context.deleteFile(fileName);
						}
					}
					catch (NumberFormatException e)
					{
						Log.d(TAG, "Invalid temporary file: " + fileName);
					}
				}
			}
		}
	}
	
	public static void deleteWidget(Context context, int appWidgetId) {
		try {
			AfSettings.removeWidgetSettings(context, null, null, appWidgetId).commit();
		} catch (Exception e) {
			Log.d(TAG, "Failed to successfully remove widget settings. (appWidgetId=" + appWidgetId + ")");
		}
		
		try {
			removeWidgetFromProvider(context, appWidgetId);
		} catch (Exception e) {
			Log.d(TAG, "Failed to successfully remove widget from provider. (appWidgetId=" + appWidgetId + ")");
			e.printStackTrace();
		}
		
		try {
			deleteCacheFiles(context, appWidgetId);
		} catch (Exception e) {
			Log.d(TAG, "Failed to successfully delete cache files.");
			e.printStackTrace();
		}
		try {
			deleteTemporaryFiles(context, appWidgetId);
		} catch (Exception e) {
			Log.d(TAG, "Failed to successfully delete temporary files.");
			e.printStackTrace();
		}
	}

	public static InputStream getGzipInputStream(HttpURLConnection con)
			throws IOException {

		InputStream inputStream = con.getInputStream();

		if ("gzip".equals(con.getContentEncoding())) {
			inputStream = new GZIPInputStream(inputStream);
		}

		return inputStream;
	}
	
	public static String getUserAgent(Context context)
	{
		StringBuilder userAgent = new StringBuilder();
		
		userAgent.append("AF Weather Widget");

		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			userAgent.append("/");
			userAgent.append(pInfo.versionName);
		} catch (NameNotFoundException e) {
			Log.d(TAG, "UserAgent: Name not found.");
			e.printStackTrace();
		}
		
		userAgent.append("https://github.com/Gitsaibot/AF-Weather-Widget;" + BuildConfig.USER_AGENT);

		return userAgent.toString();
	}
	
	public static long getWidgetViewId(Context context, Uri widgetUri)
	{
		long viewRowId = -1;
		
		ContentResolver resolver = context.getContentResolver();

		try (Cursor widgetCursor = resolver.query(widgetUri, null, null, null, null)) {

			if (widgetCursor != null) {
				if (widgetCursor.moveToFirst()) {
					int viewColumnIndex = widgetCursor.getColumnIndex(AfWidgetsColumns.VIEWS);
					if (viewColumnIndex != -1) {
						viewRowId = widgetCursor.getLong(viewColumnIndex);
					}
				}
			}
		}
		
		return viewRowId;
	}
	
	public static void removeWidgetFromProvider(Context context, int appWidgetId)
	{
		Uri widgetUri = Uri.withAppendedPath(AfWidgets.CONTENT_URI, Integer.toString(appWidgetId));
		
		long viewRowId = getWidgetViewId(context, widgetUri);
		Uri viewUri = ContentUris.withAppendedId(AfViews.CONTENT_URI, viewRowId);
		
		ContentResolver resolver = context.getContentResolver();
		
		if (widgetUri != null) {
			resolver.delete(widgetUri, null, null);
		}
		if (viewUri != null) {
			resolver.delete(viewUri, null, null);
		}
		
		if (appWidgetId != -1) {
			resolver.delete(
					Uri.withAppendedPath(
							ContentUris.withAppendedId(
									AfWidgets.CONTENT_URI, appWidgetId), AfWidgets.TWIG_SETTINGS),
					null, null);
		}
		if (viewRowId != -1) {
			resolver.delete(
					Uri.withAppendedPath(
							ContentUris.withAppendedId(
									AfViews.CONTENT_URI, viewRowId), AfViews.TWIG_SETTINGS),
					null, null);
		}
	}

	public static HttpURLConnection setupHttpClient(URL url, Context context) throws IOException {

		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setRequestProperty("User-Agent", AfUtils.getUserAgent(context));
		urlConnection.setRequestProperty("Accept-Encoding", "gzip");

		return urlConnection;
	}

	public static Uri storeBitmap(
			Context context, Bitmap bitmap,
			int appWidgetId, long time, boolean landscape
	)
		throws IOException
	{
		String orientation = landscape ? "landscape" : "portrait";
		String fileName = context.getString(R.string.bufferImageFileName, appWidgetId, time, orientation);
		
		BufferedOutputStream out = null;
		
		try
		{
			File f = new File(context.getFilesDir(), fileName);
			f.setWritable(true, true);
			out = new BufferedOutputStream(new FileOutputStream(f));
			
			bitmap.setDensity(Bitmap.DENSITY_NONE);
			bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
			
			out.flush();
		}
		finally
		{
			if (out != null)
			{
				out.close();
			}
		}
		
		return Uri.parse(String.format(US,
				"content://" + BuildConfig.APPLICATION_ID + "/aixrender/%d/%d/%s",
				appWidgetId, time, orientation));
	}
	
	public static Calendar truncateDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
	public static Calendar truncateHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
	public static void updateWidgetRemoteViews(
			Context context, int appWidgetId,
			String message, boolean overwrite,
			PendingIntent pendingIntent)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		int widgetState = getWidgetState(sharedPreferences, appWidgetId);
		
		if (widgetState != WIDGET_STATE_RENDER || overwrite)
		{
			if (widgetState != WIDGET_STATE_RENDER)
			{
				setWidgetState(sharedPreferences, appWidgetId, widgetState);
			}
			
			RemoteViews updateView = new RemoteViews(context.getPackageName(), R.layout.widget_text);
			updateView.setTextViewText(R.id.widgetText, message);
			updateView.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);
			
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, updateView);
		}
	}
	
	public static PendingIntent buildConfigurationIntent(Context context, Uri widgetUri)
	{
		Intent editWidgetIntent = new Intent(Intent.ACTION_EDIT, widgetUri, context, AfPreferenceActivity.class);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return PendingIntent.getActivity(context, 0, editWidgetIntent, PI_FLAG_IMMUTABLE);
	}
	
	public static PendingIntent buildDisableSpecificDimensionsIntent(Context context, Uri widgetUri)
	{
		Intent intent = new Intent(AfService.ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS, widgetUri, context, AfServiceReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, PI_FLAG_IMMUTABLE);
	}
	
	public static PendingIntent buildWidgetProviderAutoIntent(Context context, Uri widgetUri)
	{
		Intent intent = new Intent(AfService.ACTION_UPDATE_ALL_PROVIDER_AUTO, widgetUri, context, AfServiceReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, PI_FLAG_IMMUTABLE);
	}
	
	public static void clearProviderData(ContentResolver contentResolver)
	{
		contentResolver.delete(AfPointDataForecasts.CONTENT_URI, null, null);
		contentResolver.delete(AfIntervalDataForecasts.CONTENT_URI, null, null);
		
		ContentValues values = new ContentValues();
		values.put(AfLocationsColumns.LAST_FORECAST_UPDATE, 0);
		values.put(AfLocationsColumns.FORECAST_VALID_TO, 0);
		values.put(AfLocationsColumns.NEXT_FORECAST_UPDATE, 0);
		contentResolver.update(AfLocations.CONTENT_URI, values, null, null);
	}
	
	public static void clearOldProviderData(ContentResolver contentResolver)
	{
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		AfUtils.truncateHour(calendar);
		
		// Clear old data
		calendar.add(Calendar.HOUR_OF_DAY, -12);

		contentResolver.delete(
				AfPointDataForecasts.CONTENT_URI.buildUpon().appendQueryParameter(
						"before", Long.toString(calendar.getTimeInMillis())).build(),
				null, null);
		contentResolver.delete(
				AfIntervalDataForecasts.CONTENT_URI.buildUpon().appendQueryParameter(
						"before", Long.toString(calendar.getTimeInMillis())).build(),
				null, null);
		calendar.add(Calendar.HOUR_OF_DAY, -36);
		contentResolver.delete(
				AfSunMoonData.CONTENT_URI.buildUpon().appendQueryParameter(
						"before", Long.toString(calendar.getTimeInMillis())).build(),
				null, null);
	}
	
	public static int getWidgetState(SharedPreferences sharedPreferences, int appWidgetId)
	{
		String key = "global_widget_" + appWidgetId;
		return sharedPreferences.getInt(key, 0);
	}

	public static void setWidgetState(SharedPreferences sharedPreferences, int appWidgetId, int widgetState)
	{
		Editor editor = sharedPreferences.edit();
		editWidgetState(editor, appWidgetId, widgetState);
		editor.apply();
	}
	
	public static Editor editWidgetState(Editor editor, int appWidgetId, int widgetState)
	{
		String key = "global_widget_" + appWidgetId;
		if (widgetState == 0)
		{
			editor.remove(key);
		}
		else
		{
			editor.putInt(key, widgetState);
		}
		return editor;
	}
	
}
