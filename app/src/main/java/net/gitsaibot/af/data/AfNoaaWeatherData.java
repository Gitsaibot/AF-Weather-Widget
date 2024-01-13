package net.gitsaibot.af.data;

import static net.gitsaibot.af.AfUtils.WEATHER_ICON_CLOUD;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_PARTLYCLOUD;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_POLAR_LIGHTCLOUD;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_POLAR_SLEETSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_SNOWSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_DAY_SUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_FOG;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_LIGHTRAIN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_LIGHTCLOUD;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_LIGHTRAINSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_PARTLYCLOUD;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_SLEETSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_SNOWSUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_NIGHT_SUN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_RAIN;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_RAINTHUNDER;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_SLEET;
import static net.gitsaibot.af.AfUtils.WEATHER_ICON_SNOW;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.IntervalData;
import net.gitsaibot.af.MultiKey;
import net.gitsaibot.af.PointData;
import net.gitsaibot.af.util.AfLocationInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;

public class AfNoaaWeatherData implements AfDataSource {
	
	public static final String TAG = "AfNoaaWeatherData";
	
	private static class AfNoaaDataSet<T>
	{
		@SuppressWarnings("unused")
		public String timeLayoutKey, type, units;
		public List<T> values = new ArrayList<>();
	}
	
	private static class AfNoaaTimeLayout
	{
		public String key;
		public List<Long> validTimeStartList = new ArrayList<>();
		public List<Long> validTimeEndList = new ArrayList<>();
	}
	
	@SuppressWarnings("serial")
	private final Map<String, Integer> mWeatherIconMap = new HashMap<>() {{
		/* Night icons */
		put("ntsra", WEATHER_ICON_RAINTHUNDER);
		put("nscttsra", WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN);
		put("ip", WEATHER_ICON_SLEET);
		put("nraip", WEATHER_ICON_SLEET);
		put("mix", WEATHER_ICON_NIGHT_SLEETSUN);
		put("nrasn", WEATHER_ICON_NIGHT_SLEETSUN);
		put("nsn", WEATHER_ICON_NIGHT_SNOWSUN);
		put("fzra", WEATHER_ICON_SLEET);
		put("ip", WEATHER_ICON_SLEET);
		put("nra", WEATHER_ICON_RAIN);
		put("hi_nshwrs", WEATHER_ICON_NIGHT_LIGHTRAINSUN);
		put("nra", WEATHER_ICON_LIGHTRAIN);
		put("blizzard", WEATHER_ICON_SNOW);
		put("du", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("fu", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("nfg", WEATHER_ICON_FOG);
		put("nwind", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("novc", WEATHER_ICON_CLOUD);
		put("nbkn", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("nsct", WEATHER_ICON_NIGHT_LIGHTCLOUD);
		put("nfew", WEATHER_ICON_NIGHT_LIGHTCLOUD);
		put("nskc", WEATHER_ICON_NIGHT_SUN);

		/* Day icons */
		put("tsra", WEATHER_ICON_RAINTHUNDER);
		put("scttsra", WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN);
		//put("ip", WEATHER_ICON_SLEET);
		put("raip", WEATHER_ICON_SLEET);
		//put("mix", WEATHER_ICON_DAY_POLAR_SLEETSUN);
		put("rasn", WEATHER_ICON_DAY_POLAR_SLEETSUN);
		put("sn", WEATHER_ICON_DAY_SNOWSUN);
		//put("fzra", WEATHER_ICON_SLEET);
		//put("ip", WEATHER_ICON_SLEET);
		put("shra", WEATHER_ICON_RAIN);
		//put("hi_shwrs", WEATHER_ICON_DAY_LIGHTRAINSUN);
		put("ra", WEATHER_ICON_LIGHTRAIN);
		//put("blizzard", WEATHER_ICON_SNOW);
		//put("du", WEATHER_ICON_DAY_PARTLYCLOUD);
		//put("fu", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("fg", WEATHER_ICON_FOG);
		put("cold", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("hot", WEATHER_ICON_DAY_SUN);
		put("wind", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("ovc", WEATHER_ICON_CLOUD);
		put("bkn", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("sct", WEATHER_ICON_DAY_POLAR_LIGHTCLOUD);
		put("few", WEATHER_ICON_DAY_POLAR_LIGHTCLOUD);
		put("skc", WEATHER_ICON_DAY_SUN);
	}};
	
	private static final int PARSE_TIME_LAYOUT_FLAG				= 1<<0;
	private static final int PARSE_TIME_LAYOUT_KEY_FLAG			= 1<<1;
	private static final int PARSE_TIME_START_FLAG				= 1<<2;
	private static final int PARSE_TIME_END_FLAG				= 1<<3;
	private static final int PARSE_TEMPERATURE_FLAG				= 1<<4;
	private static final int PARSE_PRECIPITATION_FLAG			= 1<<5;
	private static final int PARSE_HUMIDITY_FLAG				= 1<<6;
	private static final int PARSE_VALUE_FLAG					= 1<<7;
	private static final int PARSE_WEATHER_ICONS_FLAG			= 1<<8;
	private static final int PARSE_WEATHER_ICONS_LINK_FLAG		= 1<<9;
	
	@SuppressWarnings("serial")
	private final Map<String, Integer> mKeyFlagMap = new HashMap<>() {{
		put("time-layout", PARSE_TIME_LAYOUT_FLAG);
		put("layout-key", PARSE_TIME_LAYOUT_KEY_FLAG);
		put("start-valid-time", PARSE_TIME_START_FLAG);
		put("end-valid-time", PARSE_TIME_END_FLAG);
		put("temperature", PARSE_TEMPERATURE_FLAG);
		put("precipitation", PARSE_PRECIPITATION_FLAG);
		put("humidity", PARSE_HUMIDITY_FLAG);
		put("value", PARSE_VALUE_FLAG);
		put("conditions-icon", PARSE_WEATHER_ICONS_FLAG);
		put("icon-link", PARSE_WEATHER_ICONS_LINK_FLAG);
	}};
	
	private Context mContext;
	private AfUpdate mAfUpdate;
	
	private SimpleDateFormat mDateFormat;
	private TimeZone mUtcTimeZone;
	
	private AfNoaaWeatherData(Context context, AfUpdate afUpdate)
	{
		mContext = context;
		mAfUpdate = afUpdate;
		
		mUtcTimeZone = TimeZone.getTimeZone("UTC");
		
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		mDateFormat.setTimeZone(mUtcTimeZone);
	}
	
	public static AfNoaaWeatherData build(Context context, AfUpdate afUpdate)
	{
		return new AfNoaaWeatherData(context, afUpdate);
	}

	private ContentValues[] buildIntervalDataContentValues(
			AfLocationInfo afLocationInfo,
			long currentUtcTime,
			Map<String, AfNoaaTimeLayout> timeLayoutMap,
			AfNoaaDataSet<Float> precipitationData,
			AfNoaaDataSet<String> weatherIconData)
	{
		Map<MultiKey, IntervalData> intervalDataMap = new HashMap<>();
		
		boolean isInches = precipitationData.units.equalsIgnoreCase("inches");
		List<Float> precipitationValues = precipitationData.values;
		List<Long> precipitationTimeStartList = timeLayoutMap.get(precipitationData.timeLayoutKey).validTimeStartList;
		List<Long> precipitationTimeEndList = timeLayoutMap.get(precipitationData.timeLayoutKey).validTimeEndList;
		
		for (int i = 0; i < precipitationValues.size(); i++)
		{
			float value = precipitationValues.get(i);
			
			if (isInches)
			{
				value = value * 25.4f;
			}
			
			setupIntervalData(intervalDataMap,
						   precipitationTimeStartList.get(i),
						   precipitationTimeEndList.get(i),
						   currentUtcTime).rainValue = value;
		}
		
		List<String> weatherIconValues = weatherIconData.values;
		List<Long> weatherIconTimeList = timeLayoutMap.get(weatherIconData.timeLayoutKey).validTimeStartList;
		
		for (int i = 0; i < weatherIconValues.size(); i++)
		{
			long time = weatherIconTimeList.get(i);
			Integer weatherIcon = mWeatherIconMap.get(weatherIconValues.get(i));
			if (weatherIcon != null)
			{
				setupIntervalData(intervalDataMap, time, time, currentUtcTime).weatherIcon = weatherIcon;
			}
		}
		
		List<ContentValues> intervalDataContentValuesList = new ArrayList<>();
		
		for (IntervalData intervalData : intervalDataMap.values())
		{
			intervalDataContentValuesList.add(intervalData.buildContentValues(afLocationInfo.getId()));
		}
		
		return intervalDataContentValuesList.toArray(new ContentValues[0]);
	}
	
	private ContentValues[] buildPointDataContentValues(
			AfLocationInfo afLocationInfo,
			long currentUtcTime,
			Map<String, AfNoaaTimeLayout> timeLayoutMap,
			AfNoaaDataSet<Float> temperatureData,
			AfNoaaDataSet<Float> humidityData)
	{
		Map<Long, PointData> pointDataMap = new HashMap<>();
		
		boolean isFahrenheit = temperatureData.units.equalsIgnoreCase("fahrenheit");
		List<Float> temperatureValues = temperatureData.values;
		List<Long> temperatureTimeList = timeLayoutMap.get(temperatureData.timeLayoutKey).validTimeStartList;
		
		for (int i = 0; i < temperatureValues.size(); i++)
		{
			float value = temperatureValues.get(i);
			
			if (isFahrenheit)
			{
				value = (value - 32.0f) * 5.0f / 9.0f;
			}
			
			setupPointData(pointDataMap, temperatureTimeList.get(i), currentUtcTime).temperature = value;
		}
		
		List<Float> humidityValues = humidityData.values;
		List<Long> humidityTimeList = timeLayoutMap.get(humidityData.timeLayoutKey).validTimeStartList;
		for (int i = 0; i < humidityValues.size(); i++)
		{
			setupPointData(pointDataMap, humidityTimeList.get(i), currentUtcTime).humidity = humidityValues.get(i);
		}
		
		List<ContentValues> pointDataContentValuesList = new ArrayList<>();
		
		for (PointData pointData : pointDataMap.values())
		{
			pointDataContentValuesList.add(pointData.buildContentValues(afLocationInfo.getId()));
		}
		
		return pointDataContentValuesList.toArray(new ContentValues[0]);
	}
	
	private boolean checkFlags(int state, int... flags)
	{
		for (int flag : flags)
		{
			if ((state & flag) == 0)
			{
				return false;
			}
		}
		return true;
	}
	
	private String getWeatherIconKeyFromLink(String link)
	{
		String input = link.trim();
		
		int slash = input.lastIndexOf('/') + 1;
		if (slash == 0 || slash >= input.length())
		{
			return null;
		}
		
		String key = input.substring(slash).split("\\d*[.]jpg")[0];
		
		return key;
	}
	
	private void parseXmlData(
			InputStream content,
			Map<String, AfNoaaTimeLayout> timeLayoutMap,
			AfNoaaDataSet<Float> temperatureData,
			AfNoaaDataSet<Float> humidityData,
			AfNoaaDataSet<Float> precipitationData,
			AfNoaaDataSet<String> weatherIconData
	)
			throws XmlPullParserException,
				   ParseException,
				   NumberFormatException,
				   IOException
	{
		int state = 0;
		
		AfNoaaTimeLayout currentTimeLayout = null;
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(content, null);

		for (int eventType = parser.getEventType();
				 eventType != XmlPullParser.END_DOCUMENT;
				 eventType = parser.next())
		{
			String tagName = parser.getName();

			switch (eventType) {
				case XmlPullParser.TEXT -> {
					String text = parser.getText();
					if (text != null) {
						if (checkFlags(state, PARSE_TIME_LAYOUT_FLAG, PARSE_TIME_START_FLAG)) {
							currentTimeLayout.validTimeStartList.add(mDateFormat.parse(text).getTime());
						} else if (checkFlags(state, PARSE_TIME_LAYOUT_FLAG, PARSE_TIME_END_FLAG)) {
							currentTimeLayout.validTimeEndList.add(mDateFormat.parse(text).getTime());
						} else if (checkFlags(state, PARSE_TIME_LAYOUT_FLAG, PARSE_TIME_LAYOUT_KEY_FLAG)) {
							currentTimeLayout.key = text;
						} else if (checkFlags(state, PARSE_TEMPERATURE_FLAG, PARSE_VALUE_FLAG)) {
							temperatureData.values.add(Float.parseFloat(text));
						} else if (checkFlags(state, PARSE_PRECIPITATION_FLAG, PARSE_VALUE_FLAG)) {
							precipitationData.values.add(Float.parseFloat(text));
						} else if (checkFlags(state, PARSE_HUMIDITY_FLAG, PARSE_VALUE_FLAG)) {
							humidityData.values.add(Float.parseFloat(text));
						} else if (checkFlags(state, PARSE_WEATHER_ICONS_FLAG, PARSE_WEATHER_ICONS_LINK_FLAG)) {
							weatherIconData.values.add(getWeatherIconKeyFromLink(text));
						}
					}
				}
				case XmlPullParser.END_TAG -> {
					if (tagName != null) {
						if (tagName.equals("time-layout")) {
							timeLayoutMap.put(currentTimeLayout.key, currentTimeLayout);
							currentTimeLayout = null;
						}

						Integer flag = mKeyFlagMap.get(tagName);
						if (flag != null) {
							state &= ~flag;
						}
					}
				}
				case XmlPullParser.START_TAG -> {
					if (tagName != null) {
						switch (tagName) {
							case "time-layout" -> currentTimeLayout = new AfNoaaTimeLayout();
							case "temperature" -> {
								String type = parser.getAttributeValue(null, "type");
								if (type == null || !type.equals("hourly")) continue;
								temperatureData.type = type;
								temperatureData.units = parser.getAttributeValue(null, "units");
								temperatureData.timeLayoutKey = parser.getAttributeValue(null, "time-layout");
							}
							case "precipitation" -> {
								String type = parser.getAttributeValue(null, "type");
								if (type == null || !type.equals("liquid")) continue;
								precipitationData.type = type;
								precipitationData.units = parser.getAttributeValue(null, "units");
								precipitationData.timeLayoutKey = parser.getAttributeValue(null, "time-layout");
							}
							case "humidity" -> {
								String type = parser.getAttributeValue(null, "type");
								if (type == null || !type.equals("relative")) continue;
								humidityData.type = type;
								humidityData.units = parser.getAttributeValue(null, "units");
								humidityData.timeLayoutKey = parser.getAttributeValue(null, "time-layout");
							}
							case "conditions-icon" -> {
								String type = parser.getAttributeValue(null, "type");
								if (type == null || !type.equals("forecast-NWS")) continue;
								weatherIconData.type = type;
								weatherIconData.timeLayoutKey = parser.getAttributeValue(null, "time-layout");
							}
						}

						Integer flag = mKeyFlagMap.get(tagName);
						if (flag != null) {
							state |= flag;
						}
					}
				}
			}
		}
	}
	
	private static IntervalData setupIntervalData(Map<MultiKey, IntervalData> intervalDataMap, long timeFrom, long timeTo, long timeAdded)
	{
		MultiKey multiKey = new MultiKey(timeFrom, timeTo);
		IntervalData intervalData = intervalDataMap.get(multiKey);
		
		if (intervalData == null) {
			intervalData = new IntervalData();
			intervalData.timeFrom = timeFrom;
			intervalData.timeTo = timeTo;
			intervalData.timeAdded = timeAdded;
			intervalDataMap.put(multiKey, intervalData);
		}
		
		return intervalData;
	}
	
	private static PointData setupPointData(Map<Long, PointData> pointDataMap, long time, long timeAdded)
	{
		PointData pointData = pointDataMap.get(time);
		
		if (pointData == null) {
			pointData = new PointData();
			pointData.time = time;
			pointData.timeAdded = timeAdded;
			pointDataMap.put(time, pointData);
		}
		
		return pointData;
	}
	
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException
	{
		try
		{
			mAfUpdate.updateWidgetRemoteViews("Getting NWS weather data...", false);
			
			Log.d(TAG, "update(): Started update operation. (aixLocationInfo=" + afLocationInfo + ",currentUtcTime=" + currentUtcTime + ")");
			
			Double latitude = afLocationInfo.getLatitude();
			Double longitude = afLocationInfo.getLongitude();
			
			if (latitude == null || longitude == null)
			{
				throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			long t1 = System.currentTimeMillis();
			
			String buildUrl = String.format(
					Locale.US,
					"https://graphical.weather.gov/xml/sample_products/browser_interface/ndfdXMLclient.php"
							+ "?lat=%.3f&lon=%.3f&product=time-series&temp=temp&rh=rh&icons=icons&qpf=qpf",
					latitude, longitude);

			URL url = new URL(buildUrl);
			HttpURLConnection httpClient = AfUtils.setupHttpClient(url, mContext);

			int code = httpClient.getResponseCode();
			if (code !=  200) {
				if (code == 429) {
					throw new AfDataUpdateException(buildUrl, AfDataUpdateException.Reason.RATE_LIMITED);
				}
				else {
					throw new IOException("Invalid response from server: " + code);
				}
			}

			InputStream content = AfUtils.getGzipInputStream(httpClient);
			
			long t2 = System.currentTimeMillis();
			
			mAfUpdate.updateWidgetRemoteViews("Parsing NWS weather data...", false);
			
			Map<String, AfNoaaTimeLayout> timeLayoutMap = new HashMap<>();
			
			AfNoaaDataSet<Float> humidityData = new AfNoaaDataSet<>();
			AfNoaaDataSet<Float> temperatureData = new AfNoaaDataSet<>();
			AfNoaaDataSet<String> weatherIconData = new AfNoaaDataSet<>();
			AfNoaaDataSet<Float> precipitationData = new AfNoaaDataSet<>();
			
			parseXmlData(content, timeLayoutMap, temperatureData, humidityData, precipitationData, weatherIconData);
			
			ContentValues[] pointDataContentValuesList = buildPointDataContentValues(afLocationInfo, currentUtcTime, timeLayoutMap, temperatureData, humidityData);
			ContentValues[] intervalDataContentValuesList = buildIntervalDataContentValues(afLocationInfo, currentUtcTime, timeLayoutMap, precipitationData, weatherIconData);
			
			long t3 = System.currentTimeMillis();
			
			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AfPointDataForecasts.CONTENT_URI, pointDataContentValuesList);
			resolver.bulkInsert(AfIntervalDataForecasts.CONTENT_URI, intervalDataContentValuesList);
			
			long t4 = System.currentTimeMillis();
			
			Log.d(TAG, String.format("update(): Download (%d ms) + Parse (%d ms) + Insertion (%d ms) = %d ms",
					(t2 - t1), (t3 - t2), (t4 - t3), (t4 - t1)));
			
			// Remove duplicates from weather data
			int numRedundantPointDataEntries = resolver.update(AfPointDataForecasts.CONTENT_URI, null, null, null);
			int numRedundantIntervalDataEntries = resolver.update(AfIntervalDataForecasts.CONTENT_URI, null, null, null);
			
			Log.d(TAG, String.format("update(): %d new PointData entries! %d redundant entries removed.", pointDataContentValuesList.length, numRedundantPointDataEntries));
			Log.d(TAG, String.format("update(): %d new IntervalData entries! %d redundant entries removed.", intervalDataContentValuesList.length, numRedundantIntervalDataEntries));
			
			// Successfully retrieved weather data. Update lastUpdated parameter
			afLocationInfo.setLastForecastUpdate(currentUtcTime);
			afLocationInfo.setForecastValidTo(currentUtcTime + 18 * DateUtils.HOUR_IN_MILLIS);
			afLocationInfo.setNextForecastUpdate(currentUtcTime + 5 * DateUtils.HOUR_IN_MILLIS);
			afLocationInfo.commit(mContext);
		}
		catch (Exception e)
		{
			Log.d(TAG, "update(): Failed to complete update. (" + e.getMessage() + ")");
			e.printStackTrace();
			throw new AfDataUpdateException();
		}
	}

}
