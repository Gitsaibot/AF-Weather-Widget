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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

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

	public static final int[] WEATHER_ICONS_DAY = {
		R.drawable.weather_icon_clearsky_day,
		R.drawable.weather_icon_fair_day,
		R.drawable.weather_icon_partlycloudy_day,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_lightrainshowers_day,
		R.drawable.weather_icon_lightrainshowersandthunder_day,
		R.drawable.weather_icon_lightsleetshowers_day,
		R.drawable.weather_icon_lightsnowshowers_day,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_fog,
		R.drawable.weather_icon_clearsky_day, // invalid for day
		R.drawable.weather_icon_fair_day, // invalid for day
		R.drawable.weather_icon_lightrainshowers_day, // invalid for day
		R.drawable.weather_icon_lightsnowshowers_day, // invalid for day
		R.drawable.weather_icon_lightssleetshowersandthunder_day,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder
	};

	public static final int[] WEATHER_ICONS_NIGHT = {
		R.drawable.weather_icon_clearsky_night,
		R.drawable.weather_icon_fair_night,
		R.drawable.weather_icon_partlycloudy_night,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_lightrainshowers_night,
		R.drawable.weather_icon_lightrainshowersandthunder_night,
		R.drawable.weather_icon_lightsleetshowers_night,
		R.drawable.weather_icon_lightsnowshowers_night,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_fog,
		R.drawable.weather_icon_clearsky_night, // invalid for night
		R.drawable.weather_icon_fair_night, // invalid for night
		R.drawable.weather_icon_lightrainshowers_night, // invalid for night
		R.drawable.weather_icon_lightsnowshowers_night, // invalid for night
		R.drawable.weather_icon_lightssleetshowersandthunder_night,
		R.drawable.weather_icon_lightssnowshowersandthunder_night,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder
	};

	public static final int[] WEATHER_ICONS_POLAR = {
		R.drawable.weather_icon_clearsky_polartwilight,
		R.drawable.weather_icon_fair_day,
		R.drawable.weather_icon_fair_polartwilight,
		R.drawable.weather_icon_cloudy,
		R.drawable.weather_icon_lightrainshowers_polartwilight,
		R.drawable.weather_icon_lightrainshowersandthunder_day,
		R.drawable.weather_icon_lightsleetshowers_day,
		R.drawable.weather_icon_lightsnowshowers_polartwilight,
		R.drawable.weather_icon_lightrain,
		R.drawable.weather_icon_rain,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleet,
		R.drawable.weather_icon_lightsnow,
		R.drawable.weather_icon_lightsnowandthunder,
		R.drawable.weather_icon_fog,
		R.drawable.weather_icon_clearsky_polartwilight,
		R.drawable.weather_icon_fair_day,
		R.drawable.weather_icon_lightrainshowers_polartwilight,
		R.drawable.weather_icon_lightsnowshowers_polartwilight,
		R.drawable.weather_icon_lightssleetshowersandthunder_polartwilight,
		R.drawable.weather_icon_lightssnowshowersandthunder_polartwilight,
		R.drawable.weather_icon_lightrainandthunder,
		R.drawable.weather_icon_lightsleetandthunder
	};
	
	public static final int WEATHER_ICON_DAY_SUN = 1;
	public static final int WEATHER_ICON_NIGHT_SUN = 1;
	public static final int WEATHER_ICON_POLAR_SUN = 1;
	public static final int WEATHER_ICON_DAY_POLAR_LIGHTCLOUD = 2;
	public static final int WEATHER_ICON_NIGHT_LIGHTCLOUD = 2;
	public static final int WEATHER_ICON_DAY_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_NIGHT_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_POLAR_PARTLYCLOUD = 3;
	public static final int WEATHER_ICON_CLOUD = 4;
	public static final int WEATHER_ICON_DAY_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_NIGHT_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_POLAR_LIGHTRAINSUN = 5;
	public static final int WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN = 6;
	public static final int WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN = 6;
	public static final int WEATHER_ICON_DAY_POLAR_SLEETSUN = 7;
	public static final int WEATHER_ICON_NIGHT_SLEETSUN = 7;
	public static final int WEATHER_ICON_DAY_SNOWSUN = 8;
	public static final int WEATHER_ICON_NIGHT_SNOWSUN = 8;
	public static final int WEATHER_ICON_POLAR_SNOWSUN = 8;
	public static final int WEATHER_ICON_LIGHTRAIN = 9;
	public static final int WEATHER_ICON_RAIN = 10;
	public static final int WEATHER_ICON_RAINTHUNDER = 11;
	public static final int WEATHER_ICON_SLEET = 12;
	public static final int WEATHER_ICON_SNOW = 13;
	public static final int WEATHER_ICON_SNOWTHUNDER = 14;
	public static final int WEATHER_ICON_FOG = 15;
	//public static final int WEATHER_ICON_POLAR_SUN = 16;
	//public static final int WEATHER_ICON_DAY_POLAR_LIGHTCLOUD = 17;
	//public static final int WEATHER_ICON_POLAR_LIGHTRAINSUN = 18;
	//public static final int WEATHER_ICON_POLAR_SNOWSUN = 19;
	public static final int WEATHER_ICON_DAY_SLEETSUNTHUNDER = 20;
	public static final int WEATHER_ICON_DAY_SNOWSUNTHUNDER = 21;
	public static final int WEATHER_ICON_LIGHTRAINTHUNDER = 22;
	public static final int WEATHER_ICON_SLEETTHUNDER = 23;
	
	public static final int BORDER_COLOR = 0;
	public static final int BACKGROUND_COLOR = 1;
	public static final int TEXT_COLOR = 2;
	public static final int PATTERN_COLOR = 3;
	public static final int DAY_COLOR = 4;
	public static final int NIGHT_COLOR = 5;
	public static final int GRID_COLOR = 6;
	public static final int GRID_OUTLINE_COLOR = 7;
	public static final int MAX_RAIN_COLOR = 8;
	public static final int MIN_RAIN_COLOR = 9;
	public static final int ABOVE_FREEZING_COLOR = 10;
	public static final int BELOW_FREEZING_COLOR = 11;
	
	public static final int TOP_TEXT_NEVER = 1;
	public static final int TOP_TEXT_LANDSCAPE = 2;
	public static final int TOP_TEXT_PORTRAIT = 3;
	public static final int TOP_TEXT_ALWAYS = 4;
	
	private AfUtils() {
		
	}
	
	public final static int even(int value) {
		return (value % 2 != 0) ? value - 1 : value;
	}
	
	public final static int odd(int value) {
		return (value % 2 != 0) ? value : value - 1;
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
		if ((n % 2 != 0 && prime && n > 2) || n == 2) {
			return true;
		} else {
			return false;
		}
	}
	
	public static HttpGet buildGzipHttpGet(String url)
	{
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Accept-Encoding", "gzip");
		return httpGet;
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
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, StandardCharsets.UTF_8));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
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
	
	public static InputStream getGzipInputStream(HttpResponse httpResponse)
			throws IllegalStateException, IOException
	{
		InputStream content = httpResponse.getEntity().getContent();
		
		Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			content = new GZIPInputStream(content);
		}
		
		return content;
	}
	
	public static String getUserAgent(Context context)
	{
		StringBuilder userAgent = new StringBuilder();
		
		userAgent.append("AF Weather Widget");

		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			userAgent.append("/");
			userAgent.append(pInfo.versionName);
		} catch (NameNotFoundException e) { }
		
		userAgent.append("https://github.com/Gitsaibot/AF-Weather-Widget;" + BuildConfig.USER_AGENT);

		return userAgent.toString();
	}
	
	public static long getWidgetViewId(Context context, Uri widgetUri)
	{
		long viewRowId = -1;
		
		ContentResolver resolver = context.getContentResolver();
		
		Cursor widgetCursor = null;
		try
		{
			widgetCursor = resolver.query(widgetUri, null, null, null, null);
			
			if (widgetCursor != null)
			{
				if (widgetCursor.moveToFirst())
				{
					int viewColumnIndex = widgetCursor.getColumnIndex(AfWidgetsColumns.VIEWS);
					if (viewColumnIndex != -1)
					{
						viewRowId = widgetCursor.getLong(viewColumnIndex);
					}
				}
			}
		}
		finally
		{
			if (widgetCursor != null)
			{
				widgetCursor.close();
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
	
	public static HttpClient setupHttpClient(Context context) {
		HttpParams httpParameters = new BasicHttpParams();
		httpParameters.setParameter(CoreConnectionPNames.SO_TIMEOUT, 7777);
		httpParameters.setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent(context));
		return new DefaultHttpClient(httpParameters);
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
				"content://net.gitsaibot.af/aixrender/%d/%d/%s",
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return PendingIntent.getActivity(context, 0, editWidgetIntent, PendingIntent.FLAG_IMMUTABLE);
		} else {
			return PendingIntent.getActivity(context, 0, editWidgetIntent, 0);
		}
	}
	
	public static PendingIntent buildDisableSpecificDimensionsIntent(Context context, Uri widgetUri)
	{
		Intent intent = new Intent(AfService.ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS, widgetUri, context, AfServiceReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}
	
	public static PendingIntent buildWidgetProviderAutoIntent(Context context, Uri widgetUri)
	{
		Intent intent = new Intent(AfService.ACTION_UPDATE_ALL_PROVIDER_AUTO, widgetUri, context, AfServiceReceiver.class);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
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
	
	public static boolean isWifiConnected(Context context)
	{
		ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectionManager == null) return false;
		NetworkInfo wifiInfo = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo == null) return false;
		return wifiInfo.isConnected();
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
