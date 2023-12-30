package net.gitsaibot.af.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfPointDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.BuildConfig;
import net.gitsaibot.af.util.AfLocationInfo;

import org.xmlpull.v1.XmlPullParser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class AfMetWeatherData implements AfDataSource {

	public static final String TAG = "AfMetWeatherData";
	
	private Context mContext;
	private AfUpdate mAfUpdate;
	
	private AfMetWeatherData(Context context, AfUpdate afUpdate)
	{
		mContext = context;
		mAfUpdate = afUpdate;
	}
	
	public static AfMetWeatherData build(Context context, AfUpdate afUpdate)
	{
		return new AfMetWeatherData(context, afUpdate);
	}
	
	private static int mapWeatherIconToOldApi(int id)
	{
		switch (id % 100) // ID + 100 is used to indicate polar night in WeatherIcon 1.1 - Mod 100 to get normal value.
		{
			case 24: // DrizzleThunderSun
			case 25: // RainThunderSun
				return AfUtils.WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN; // LightRainThunderSun
			case 26: // LightSleetThunderSun
			case 27: // HeavySleetThunderSun
				return AfUtils.WEATHER_ICON_DAY_SLEETSUNTHUNDER; // SleetSunThunder
			case 28: // LightSnowThunderSun
			case 29: // HeavySnowThunderSun
				return AfUtils.WEATHER_ICON_DAY_SNOWSUNTHUNDER; // SnowSunThunder
			case 30: // DrizzleThunder
				return AfUtils.WEATHER_ICON_LIGHTRAINTHUNDER; // LightRainThunder
			case 31: // LightSleetThunder
			case 32: // HeavySleetThunder
				return AfUtils.WEATHER_ICON_SLEETTHUNDER; // SleetThunder
			case 33: // LightSnowThunder
			case 34: // HeavySnowThunder
				return AfUtils.WEATHER_ICON_SNOWTHUNDER; // SnowThunder
			case 40: // DrizzleSun
			case 41: // RainSun
				return AfUtils.WEATHER_ICON_DAY_LIGHTRAINSUN; // LightRainSun
			case 42: // LightSleetSun
			case 43: // HeavySleetSun
				return AfUtils.WEATHER_ICON_DAY_POLAR_SLEETSUN; // SleetSun
			case 44: // LightSnowSun
			case 45: // HeavysnowSun
				return AfUtils.WEATHER_ICON_DAY_SNOWSUN; // SnowSun
			case 46: // Drizzle
				return AfUtils.WEATHER_ICON_LIGHTRAIN; // LightRain
			case 47: // LightSleet
			case 48: // HeavySleet
				return AfUtils.WEATHER_ICON_SLEET; // Sleet
			case 49: // LightSnow
			case 50: // HeavySnow
				return AfUtils.WEATHER_ICON_SNOW; // Snow
			default:
				return id;
		}
	}
	
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException
	{
		try {
			Log.d(TAG, "update(): Started update operation. (aixLocationInfo=" + afLocationInfo + ",currentUtcTime=" + currentUtcTime + ")");
			
			mAfUpdate.updateWidgetRemoteViews("Downloading NMI weather data...", false);
			
			Double latitude = afLocationInfo.getLatitude();
			Double longitude = afLocationInfo.getLongitude();
			
			if (latitude == null || longitude == null)
			{
				throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			String buildUrl = String.format(
					Locale.US,
					"https://" + BuildConfig.API_KEY + "/weatherapi/locationforecast/2.0/classic?lat=%.3f&lon=%.3f",
					latitude, longitude);
			
			Log.d(TAG, "Attempting to download weather data from URL=" + buildUrl);

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
			
			mAfUpdate.updateWidgetRemoteViews("Parsing NMI weather data...", false);
			
			TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
			dateFormat.setTimeZone(utcTimeZone);
			
			ArrayList<ContentValues> pointDataValues = new ArrayList<>();
			ArrayList<ContentValues> intervalDataValues = new ArrayList<>();
			ArrayList<ContentValues> currentList = null;
			
			long nextUpdate = -1;
			long forecastValidTo = -1;
			
			ContentValues contentValues = null;
			
			XmlPullParser parser = Xml.newPullParser();
			
			long startTime = System.currentTimeMillis();
			
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
					case XmlPullParser.END_TAG -> {
						if (parser.getName().equals("time") && contentValues != null) {
							currentList.add(contentValues);
						}
					}
					case XmlPullParser.START_TAG -> {
						if (parser.getName().equals("time")) {
							contentValues = new ContentValues();

							String fromString = parser.getAttributeValue(null, "from");
							String toString = parser.getAttributeValue(null, "to");

							try {
								long from = dateFormat.parse(fromString).getTime();
								long to = dateFormat.parse(toString).getTime();

								if (from != to) {
									currentList = intervalDataValues;
									contentValues.put(AfIntervalDataForecastColumns.LOCATION, afLocationInfo.getId());
									contentValues.put(AfIntervalDataForecastColumns.TIME_ADDED, currentUtcTime);
									contentValues.put(AfIntervalDataForecastColumns.TIME_FROM, from);
									contentValues.put(AfIntervalDataForecastColumns.TIME_TO, to);
								} else {
									currentList = pointDataValues;
									contentValues.put(AfPointDataForecastColumns.LOCATION, afLocationInfo.getId());
									contentValues.put(AfPointDataForecastColumns.TIME_ADDED, currentUtcTime);
									contentValues.put(AfPointDataForecastColumns.TIME, from);
								}
							} catch (Exception e) {
								Log.d(TAG, "Error parsing from & to values. from="
										+ fromString + " to=" + toString);
								contentValues = null;
							}
						} else if (parser.getName().equals("temperature")) {
							if (contentValues != null) {
								contentValues.put(AfPointDataForecastColumns.TEMPERATURE,
										Float.parseFloat(parser.getAttributeValue(null, "value")));
							}
						} else if (parser.getName().equals("humidity")) {
							if (contentValues != null) {
								contentValues.put(AfPointDataForecastColumns.HUMIDITY,
										Float.parseFloat(parser.getAttributeValue(null, "value")));
							}
						} else if (parser.getName().equals("pressure")) {
							if (contentValues != null) {
								contentValues.put(AfPointDataForecastColumns.PRESSURE,
										Float.parseFloat(parser.getAttributeValue(null, "value")));
							}
						} else if (parser.getName().equals("symbol")) {
							if (contentValues != null) {
								contentValues.put(AfIntervalDataForecastColumns.WEATHER_ICON,
										mapWeatherIconToOldApi(
												Integer.parseInt(parser.getAttributeValue(null, "number"))));
							}
						} else if (parser.getName().equals("precipitation")) {
							if (contentValues != null) {
								contentValues.put(AfIntervalDataForecastColumns.RAIN_VALUE,
										Float.parseFloat(
												parser.getAttributeValue(null, "value")));
								try {
									contentValues.put(AfIntervalDataForecastColumns.RAIN_MINVAL,
											Float.parseFloat(
													parser.getAttributeValue(null, "minvalue")));
								} catch (Exception e) {
									/* LOW VALUE IS OPTIONAL */
								}
								try {
									contentValues.put(AfIntervalDataForecastColumns.RAIN_MAXVAL,
											Float.parseFloat(
													parser.getAttributeValue(null, "maxvalue")));
								} catch (Exception e) {
									/* HIGH VALUE IS OPTIONAL */
								}
							}
						} else if (parser.getName().equals("model")) {
							String model = parser.getAttributeValue(null, "name");
							if (model.toLowerCase(Locale.US).equals("yr")) {
								try {
									nextUpdate = dateFormat.parse(parser.getAttributeValue(null, "nextrun")).getTime();
								} catch (ParseException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								try {
									forecastValidTo = dateFormat.parse(parser.getAttributeValue(null, "to")).getTime();
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
				eventType = parser.next();
			}
			
			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AfPointDataForecasts.CONTENT_URI, pointDataValues.toArray(new ContentValues[0]));
			resolver.bulkInsert(AfIntervalDataForecasts.CONTENT_URI, intervalDataValues.toArray(new ContentValues[0]));

			// Remove duplicates from weather data
			int numRedundantPointDataEntries = resolver.update(AfPointDataForecasts.CONTENT_URI, null, null, null);
			int numRedundantIntervalDataEntries = resolver.update(AfIntervalDataForecasts.CONTENT_URI, null, null, null);
			
			Log.d(TAG, String.format("update(): %d new PointData entries! %d redundant entries removed.", pointDataValues.size(), numRedundantPointDataEntries));
			Log.d(TAG, String.format("update(): %d new IntervalData entries! %d redundant entries removed.", intervalDataValues.size(), numRedundantIntervalDataEntries));
			
			afLocationInfo.setLastForecastUpdate(currentUtcTime);
			afLocationInfo.setForecastValidTo(forecastValidTo);
			afLocationInfo.setNextForecastUpdate(nextUpdate);
			afLocationInfo.commit(mContext);
			
			long endTime = System.currentTimeMillis();
			
			Log.d(TAG, "Time spent parsing MET data = " + (endTime - startTime) + " ms");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new AfDataUpdateException();
		}
	}
	
}
