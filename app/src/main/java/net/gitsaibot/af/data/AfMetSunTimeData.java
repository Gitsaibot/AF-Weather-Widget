package net.gitsaibot.af.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

public class AfMetSunTimeData implements AfDataSource {

	public final static String TAG = "AfMetSunTimeData";

	private final static int NUM_DAYS_MINIMUM = 5;
	private final static int NUM_DAYS_REQUEST = 15;

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
		
		mTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
		mTimeFormat.setTimeZone(mUtcTimeZone);
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

	private List<ContentValues> parseData(
			InputStream content, long locationId, long currentUtcTime, int maxDays)
			throws ParseException, XmlPullParserException, IOException
	{
		List<ContentValues> contentValuesList = new ArrayList<>();
		ContentValues contentValues = null;

		float solarnoonElevationValue = 0.0f;
		Float moonElevationAtStartOfDay = null;
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(content, null);
		
		int eventType = parser.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.END_TAG:
				if (parser.getName().equalsIgnoreCase("time") && contentValues != null)
				{
					if (!contentValues.containsKey(AfSunMoonDataColumns.SUN_RISE))
					{
						contentValues.put(AfSunMoonDataColumns.SUN_RISE, solarnoonElevationValue >= 0.0f ? 0 : AfSunMoonData.NEVER_RISE);
					}

					if (!contentValues.containsKey(AfSunMoonDataColumns.SUN_SET))
					{
						contentValues.put(AfSunMoonDataColumns.SUN_SET, AfSunMoonData.NEVER_SET);
					}

					if (!contentValues.containsKey(AfSunMoonDataColumns.MOON_RISE))
					{
						contentValues.put(
							AfSunMoonDataColumns.MOON_RISE,
							(moonElevationAtStartOfDay != null && moonElevationAtStartOfDay >= 0.0f) ? 0 : AfSunMoonData.NEVER_RISE);
					}

					if (!contentValues.containsKey(AfSunMoonDataColumns.MOON_SET))
					{
						contentValues.put(AfSunMoonDataColumns.MOON_SET, AfSunMoonData.NEVER_SET);
					}

					contentValuesList.add(contentValues);
					contentValues = null;
					moonElevationAtStartOfDay = null;
				}
				break;
			case XmlPullParser.START_TAG:
				if (parser.getName().equalsIgnoreCase("time"))
				{
					if (contentValuesList.size() < maxDays)
					{
						String dateString = parser.getAttributeValue(null, "date");
						long date = mDateFormat.parse(dateString).getTime();

						contentValues = new ContentValues();
						contentValues.put(AfSunMoonDataColumns.LOCATION, locationId);
						contentValues.put(AfSunMoonDataColumns.TIME_ADDED, currentUtcTime);
						contentValues.put(AfSunMoonDataColumns.DATE, date);
					}
				}
				else if (contentValues != null)
				{
					String time = parser.getAttributeValue(null, "time");

					if (time != null)
					{
						long timeValue = mTimeFormat.parse(time.substring(0, 19)).getTime();

						if (parser.getName().equalsIgnoreCase("sunrise"))
						{
							contentValues.put(AfSunMoonDataColumns.SUN_RISE, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("sunset"))
						{
							contentValues.put(AfSunMoonDataColumns.SUN_SET, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("solarnoon"))
						{
							solarnoonElevationValue = Float.parseFloat(
								parser.getAttributeValue(null, "elevation"));
						}
						else if (parser.getName().equalsIgnoreCase("moonrise"))
						{
							contentValues.put(AfSunMoonDataColumns.MOON_RISE, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("moonset"))
						{
							contentValues.put(AfSunMoonDataColumns.MOON_SET, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("moonposition"))
						{
							moonElevationAtStartOfDay = Float.parseFloat(
									parser.getAttributeValue(null, "elevation"));

							float moonphaseValue = Float.parseFloat(
									parser.getAttributeValue(null, "phase"));

							contentValues.put(
									AfSunMoonDataColumns.MOON_PHASE,
									parseMoonPhaseValue(moonphaseValue, parser.getLineNumber()));
						}
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return contentValuesList;
	}

	private int parseMoonPhaseValue(float value, int parserLineNumber) throws ParseException
	{
		if (value >= 0.0 && value < 0.5) {
			return AfSunMoonData.NEW_MOON;
		}
		else if (value >= 0.5 && value < 20.0) {
			return AfSunMoonData.WAXING_CRESCENT;
		}
		else if (value >= 20.0 && value < 30.0) {
			return AfSunMoonData.FIRST_QUARTER;
		}
		else if (value >= 30.0 && value < 49.5) {
			return AfSunMoonData.WAXING_GIBBOUS;
		}
		else if (value >= 49.5 && value < 50.5) {
			return AfSunMoonData.FULL_MOON;
		}
		else if (value >= 50.5 && value < 70.0) {
			return AfSunMoonData.WANING_GIBBOUS;
		}
		else if (value >= 70.0 && value < 80.0) {
			return AfSunMoonData.LAST_QUARTER;
		}
		else if (value >= 80.0 && value < 99.5) {
			return AfSunMoonData.WANING_CRESCENT;
		}
		else if (value > 99.5 && value <= 100.0) {
			return AfSunMoonData.NEW_MOON;
		}
		else {
			throw new ParseException(
				String.format("parseMoonPhaseValue: value %f out of range", value),
				parserLineNumber);
		}
	}

	private void setupDateParameters(long time)
	{
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(time);
		AfUtils.truncateDay(calendar);
		
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		mStartDate = calendar.getTimeInMillis();
		
		calendar.add(Calendar.DAY_OF_YEAR, NUM_DAYS_REQUEST - 1);
		mEndDate = calendar.getTimeInMillis();
	}
	
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException
	{
		try {
			mAfUpdate.updateWidgetRemoteViews("Getting sun time data...", false);
			
			Double latitude = afLocationInfo.getLatitude();
			Double longitude = afLocationInfo.getLongitude();
			
			if (latitude == null || longitude == null)
			{
				throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			setupDateParameters(currentUtcTime);
			
			int numExistingDataSets = getNumExistingDataSets(afLocationInfo.getId());
			
			Log.d(TAG, String.format("update(): For location %s (%d), there are %d existing datasets.",
					afLocationInfo.getTitle(), afLocationInfo.getId(), numExistingDataSets));
			
			if (numExistingDataSets < NUM_DAYS_MINIMUM)
			{
				String url = String.format(
						Locale.US,
						"https://" + BuildConfig.API_KEY + "/weatherapi/sunrise/2.0/?lat=%.1f&lon=%.1f&date=%s&offset=+00:00&days=%d",
						latitude, longitude,
						mDateFormat.format(mStartDate),
						NUM_DAYS_REQUEST);

				HttpClient httpClient = AfUtils.setupHttpClient(mContext);
				HttpGet httpGet = AfUtils.buildGzipHttpGet(url);
				HttpResponse httpResponse = httpClient.execute(httpGet);

				if (httpResponse.getStatusLine().getStatusCode() == 429)
				{
					throw new AfDataUpdateException(url, AfDataUpdateException.Reason.RATE_LIMITED);
				}

				InputStream content = AfUtils.getGzipInputStream(httpResponse);

				List<ContentValues> contentValuesList = parseData(
						content, afLocationInfo.getId(), currentUtcTime, NUM_DAYS_REQUEST);

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
