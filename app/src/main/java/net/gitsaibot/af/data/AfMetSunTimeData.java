package net.gitsaibot.af.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfSunMoonData;
import net.gitsaibot.af.AfProvider.AfSunMoonDataColumns;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.BuildConfig;
import net.gitsaibot.af.util.AfLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AfMetSunTimeData implements AfDataSource {

	public final static String TAG = "AfMetSunTimeData";

	private final static int NUM_DAYS_MINIMUM = 5;
	private final static int NUM_DAYS_REQUEST = 8;

	@SuppressWarnings("serial")
	private Map<String, Integer> moonPhaseMap = new HashMap<String, Integer>() {{
		put("new moon", AfSunMoonData.NEW_MOON);
		put("waxing crescent", AfSunMoonData.WAXING_CRESCENT);
		put("first quarter", AfSunMoonData.FIRST_QUARTER);
		put("waxing gibbous", AfSunMoonData.WAXING_GIBBOUS);
		put("full moon", AfSunMoonData.FULL_MOON);
		put("waning gibbous", AfSunMoonData.WANING_GIBBOUS);
		put("third quarter", AfSunMoonData.LAST_QUARTER);
		put("waning crescent", AfSunMoonData.WANING_CRESCENT);
	}};

	private Context mContext;

	private AfUpdate mAfUpdate;

	private SimpleDateFormat mDateFormat;
	private SimpleDateFormat mTimeFormat;

	private TimeZone mUtcTimeZone;

	private long mStartDate;
	private long mEndDate;
	
	private AfMetSunTimeData(Context context, AfUpdate afUpdate)
	{
		mContext = context;
		mAfUpdate = afUpdate;
		
		mUtcTimeZone = TimeZone.getTimeZone("UTC");
		
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		mDateFormat.setTimeZone(mUtcTimeZone);
		
		mTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.US);
	}
	
	public static AfMetSunTimeData build(Context context, AfUpdate afUpdate)
	{
		return new AfMetSunTimeData(context, afUpdate);
	}
	
	private int getNumExistingDataSets(long locationId)
	{
		ContentResolver contentResolver = mContext.getContentResolver();
		
		Cursor cursor = null;
		
		try {
			final Uri uri = AfLocations.CONTENT_URI.buildUpon()
					.appendPath(Long.toString(locationId))
					.appendPath(AfLocations.TWIG_SUNMOONDATA)
					.appendQueryParameter("start", Long.toString(mStartDate))
					.appendQueryParameter("end", Long.toString(mEndDate)).build();

			cursor = contentResolver.query(uri, null, null, null, null);
			
			return cursor.getCount();
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	private JSONObject fetchData(String type, Double latitude, Double longitude, Date date, String offset) throws AfDataUpdateException {
		try {
			String url = String.format(
					Locale.US,
					"https://" + BuildConfig.API_KEY + "/weatherapi/sunrise/3.0/%s?lat=%.1f&lon=%.1f&date=%s&offset=%s",
					type, latitude, longitude, mDateFormat.format(date), offset);

			HttpClient httpClient = AfUtils.setupHttpClient(mContext);
			HttpGet httpGet = AfUtils.buildGzipHttpGet(url);
			HttpResponse httpResponse = httpClient.execute(httpGet);

			if (httpResponse.getStatusLine().getStatusCode() == 429)
			{
				throw new AfDataUpdateException(url, AfDataUpdateException.Reason.RATE_LIMITED);
			}

			InputStream content = AfUtils.getGzipInputStream(httpResponse);
			JSONObject jObject = new JSONObject(AfUtils.convertStreamToString(content)).getJSONObject("properties");
			return jObject;
		}
		catch (Exception e)
		{
			Log.d(TAG, String.format("fetchData(): " + e.getMessage() + " thrown."));
			throw new AfDataUpdateException();
		}

	}

	private ContentValues parseData(Date date, JSONObject sun, JSONObject moon,
			long locationId, long currentUtcTime)
			throws AfDataUpdateException, IOException, JSONException
	{
		try{
			float solarnoonElevationValue;

			ContentValues contentValues = new ContentValues();
			contentValues.put(AfSunMoonDataColumns.LOCATION, locationId);
			contentValues.put(AfSunMoonDataColumns.TIME_ADDED, currentUtcTime);
			Log.d(TAG, String.format("\t%s", date));
			contentValues.put(AfSunMoonDataColumns.DATE, date.getTime());

			solarnoonElevationValue = Float.parseFloat(sun.getJSONObject("solarnoon").getString("disc_centre_elevation"));
			JSONObject sunrise = sun.getJSONObject("sunrise");
			if (!sunrise.isNull("time"))
			{
				Log.d(TAG, String.format("Sunrise: %s",mTimeFormat.parse(sunrise.getString("time")).getTime()));
				contentValues.put(AfSunMoonDataColumns.SUN_RISE, mTimeFormat.parse(sunrise.getString("time")).getTime());
			}
			if (!contentValues.containsKey(AfSunMoonDataColumns.SUN_RISE))
			{
				contentValues.put(AfSunMoonDataColumns.SUN_RISE, solarnoonElevationValue >= 0.0f ? 0 : AfSunMoonData.NEVER_RISE);
			}

			JSONObject sunset = sun.getJSONObject("sunset");
			if (!sunset.isNull("time"))
			{
				contentValues.put(AfSunMoonDataColumns.SUN_SET, mTimeFormat.parse(sunset.getString("time")).getTime());
			}
			if (!contentValues.containsKey(AfSunMoonDataColumns.SUN_SET))
			{
				contentValues.put(AfSunMoonDataColumns.SUN_SET, AfSunMoonData.NEVER_SET);
			}

			JSONObject moonrise = moon.getJSONObject("moonrise");
			if (!moonrise.isNull("time"))
			{
				contentValues.put(AfSunMoonDataColumns.MOON_RISE, mTimeFormat.parse(moonrise.getString("time")).getTime());
			}
			if (!contentValues.containsKey(AfSunMoonDataColumns.MOON_RISE))
			{
				contentValues.put(AfSunMoonDataColumns.MOON_RISE, AfSunMoonData.NEVER_RISE);
			}

			JSONObject moonset = moon.getJSONObject("moonset");
			if (!moonset.isNull("time"))
			{
				contentValues.put(AfSunMoonDataColumns.MOON_SET, mTimeFormat.parse(moonset.getString("time")).getTime());
			}
			if (!contentValues.containsKey(AfSunMoonDataColumns.MOON_SET))
			{
				contentValues.put(AfSunMoonDataColumns.MOON_SET, AfSunMoonData.NEVER_SET);
			}
			contentValues.put(AfSunMoonDataColumns.MOON_PHASE, parseMoonPhaseValue(Float.parseFloat(moon.getString("moonphase"))));

			return contentValues;
		}
		catch (Exception e)
		{
			Log.d(TAG, String.format("parseData(): " + e.getMessage() + " thrown."));
			throw new AfDataUpdateException();
		}

	}

	private int parseMoonPhaseValue(float value)
	{
		if (value >= 0.0 && value < 2.0) {
			return AfSunMoonData.NEW_MOON;
		}
		else if (value >= 3.0 && value < 72.0) {
			return AfSunMoonData.WAXING_CRESCENT;
		}
		else if (value >= 72.0 && value < 108.0) {
			return AfSunMoonData.FIRST_QUARTER;
		}
		else if (value >= 388.0 && value < 178.0) {
			return AfSunMoonData.WAXING_GIBBOUS;
		}
		else if (value >= 178.0 && value < 182.0) {
			return AfSunMoonData.FULL_MOON;
		}
		else if (value >= 182.0 && value < 252.0) {
			return AfSunMoonData.WANING_GIBBOUS;
		}
		else if (value >= 252.0 && value < 288.0) {
			return AfSunMoonData.LAST_QUARTER;
		}
		else if (value >= 288.0 && value < 358.0) {
			return AfSunMoonData.WANING_CRESCENT;
		}
		else if (value >358.0 && value <= 360.0) {
			return AfSunMoonData.NEW_MOON;
		}
		return AfSunMoonData.NO_MOON_PHASE_DATA;
	}

	private Calendar setupDateParameters(long time)
	{
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(time);
		AfUtils.truncateDay(calendar);
		
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		mStartDate = calendar.getTimeInMillis();
		
		Calendar endCalendar = (Calendar) calendar.clone();
		endCalendar.add(Calendar.DAY_OF_YEAR, NUM_DAYS_REQUEST - 1);
		mEndDate = endCalendar.getTimeInMillis();
		return calendar;
	}
	
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException
	{
		try {
			mAfUpdate.updateWidgetRemoteViews("Getting sun time data...", false);
			
			Double latitude = afLocationInfo.getLatitude();
			Double longitude = afLocationInfo.getLongitude();
			String offset = afLocationInfo.getOffset();
			
			if (latitude == null || longitude == null)
			{
				throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			Calendar calendar = setupDateParameters(currentUtcTime);
			
			int numExistingDataSets = getNumExistingDataSets(afLocationInfo.getId());
			
			Log.d(TAG, String.format("update(): For location %s (%d), there are %d existing datasets.",
					afLocationInfo.getTitle(), afLocationInfo.getId(), numExistingDataSets));
			
			if (numExistingDataSets < NUM_DAYS_MINIMUM)
			{
				List<ContentValues> contentValuesList = new ArrayList<>();
				for (int i = 1; i < NUM_DAYS_REQUEST; i++) {
					Date date = calendar.getTime();
					JSONObject sun = fetchData("sun", latitude, longitude, date, offset);
					JSONObject moon = fetchData("moon", latitude, longitude, date, offset);
					ContentValues contentValues = parseData(date, sun, moon,
						afLocationInfo.getId(), currentUtcTime);
					contentValuesList.add(contentValues);
					calendar.add(Calendar.DAY_OF_YEAR, 1);
				}

				if (contentValuesList.size() > 0)
				{
					updateDatabase(contentValuesList.toArray(new ContentValues[0]));
				}

				Log.d(TAG, String.format("update(): %d datasets were added to location %s (%d).",
						contentValuesList.size(), afLocationInfo.getTitle(), afLocationInfo.getId()));
			}
		}
		catch (Exception e)
		{
			if (afLocationInfo != null)
			{
				Log.d(TAG, String.format("update(): " + e.getMessage() + " thrown for location %s (%d).",
						afLocationInfo.getTitle(), afLocationInfo.getId()));
			}
			throw new AfDataUpdateException();
		}
	}
	
	private void updateDatabase(ContentValues[] contentValuesArray)
	{
		ContentResolver contentResolver = mContext.getContentResolver();
		
		for (ContentValues contentValues : contentValuesArray)
		{
			contentResolver.insert(AfSunMoonData.CONTENT_URI, contentValues);
		}
	}
}
