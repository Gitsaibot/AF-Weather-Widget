package net.gitsaibot.af.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfPointDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.util.AfLocationInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class AfMetWeatherData implements AfDataSource {

	public static final String TAG = "AfMetWeatherData";

	private final Context mContext;
	private final AfUpdate mAfUpdate;

	private AfMetWeatherData(Context context, AfUpdate afUpdate) {
		mContext = context;
		mAfUpdate = afUpdate;
	}

	public static AfMetWeatherData build(Context context, AfUpdate afUpdate) {
		return new AfMetWeatherData(context, afUpdate);
	}

	private static int mapWeatherIconSymbol(String symbolCode) {
		if (symbolCode == null) return 0; // Return 0 (NA) for null symbol

		String symbol = symbolCode.split("_")[0].toLowerCase(Locale.US);

		return switch (symbol) {
			// Sun and clouds
			case "clearsky" -> AfUtils.WEATHER_ICON_CLEARSKY;
			case "cloudy" -> AfUtils.WEATHER_ICON_CLOUDY;
			case "fair" -> AfUtils.WEATHER_ICON_FAIR;
			case "partlycloudy" -> AfUtils.WEATHER_ICON_PARTLYCLOUD;

			// Fog
			case "fog" -> AfUtils.WEATHER_ICON_FOG;

			// Rain
			case "heavyrain" -> AfUtils.WEATHER_ICON_HEAVYRAIN;
			case "heavyrainandthunder" -> AfUtils.WEATHER_ICON_HEAVYRAINANDTHUNDER;
			case "heavyrainshowers" -> AfUtils.WEATHER_ICON_HEAVYRAINSHOWERS;
			case "heavyrainshowersandthunder" -> AfUtils.WEATHER_ICON_HEAVYRAINSHOWERSANDTHUNDER;
			case "lightrain" -> AfUtils.WEATHER_ICON_LIGHTRAIN;
			case "lightrainandthunder" -> AfUtils.WEATHER_ICON_LIGHTRAINANDTHUNDER;
			case "lightrainshowers" -> AfUtils.WEATHER_ICON_LIGHTRAINSHOWERS;
			case "lightrainshowersandthunder" -> AfUtils.WEATHER_ICON_LIGHTRAINSHOWERSANDTHUNDER;
			case "rain" -> AfUtils.WEATHER_ICON_RAIN;
			case "rainandthunder" -> AfUtils.WEATHER_ICON_RAINANDTHUNDER;
			case "rainshowers" -> AfUtils.WEATHER_ICON_RAINSHOWERS;
			case "rainshowersandthunder" -> AfUtils.WEATHER_ICON_RAINSHOWERANDTHUNDER;

			// Sleet
			case "heavysleet" -> AfUtils.WEATHER_ICON_HEAVYSLEET;
			case "heavysleetandthunder" -> AfUtils.WEATHER_ICON_HEAVYSLEETANDTHUNDER;
			case "heavysleetshowers" -> AfUtils.WEATHER_ICON_HEAVYSLEETSHOWERS;
			case "heavysleetshowersandthunder" -> AfUtils.WEATHER_ICON_HEAVYSLEETSHOWERSANDTHUNDER;
			case "lightsleet" -> AfUtils.WEATHER_ICON_LIGHTSLEET;
			case "lightsleetandthunder" -> AfUtils.WEATHER_ICON_LIGHTSLEETANDTHUNDER;
			case "lightsleetshowers" -> AfUtils.WEATHER_ICON_LIGHTSLEETSHOWERS;
			case "lightsleetshowersandthunder" -> AfUtils.WEATHER_ICON_LIGHTSLEETSHOWERSANDTHUNDER;
			case "sleet" -> AfUtils.WEATHER_ICON_SLEET;
			case "sleetandthunder" -> AfUtils.WEATHER_ICON_SLEETANDTHUNDER;
			case "sleetshowers" -> AfUtils.WEATHER_ICON_SLEETSHOWERS;
			case "sleetshowersandthunder" -> AfUtils.WEATHER_ICON_SLEETSHOWERSANDTHUNDER;

			// Snow
			case "heavysnow" -> AfUtils.WEATHER_ICON_HEAVYSNOW;
			case "heavysnowandthunder" -> AfUtils.WEATHER_ICON_HEAVYSNOWANDTHUNDER;
			case "heavysnowshowers" -> AfUtils.WEATHER_ICON_HEAVYSNOWSHOWERS;
			case "heavysnowshowersandthunder" -> AfUtils.WEATHER_ICON_HEAVYSNOWSHOWERSANDTHUNDER;
			case "lightsnow" -> AfUtils.WEATHER_ICON_LIGHTSNOW;
			case "lightsnowandthunder" -> AfUtils.WEATHER_ICON_LIGHTSNOWANDTHUNDER;
			case "lightsnowshowers" -> AfUtils.WEATHER_ICON_LIGHTSNOWSHOWERS;
			case "lightsnowshowersandthunder" -> AfUtils.WEATHER_ICON_LIGHTSNOWSHOWERSANDTHUNDER;
			case "snow" -> AfUtils.WEATHER_ICON_SNOW;
			case "snowandthunder" -> AfUtils.WEATHER_ICON_SNOWANDTHUNDER;
			case "snowshowers" -> AfUtils.WEATHER_ICON_SNOWSHOWERS;
			case "snowshowersandthunder" -> AfUtils.WEATHER_ICON_SNOWSHOWERSANDTHUNDER;

			default -> 0; // Return 0 (NA) for any symbol not matched
		};
	}


	@Override
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException {
		Log.d(TAG, "update(): Started JSON update operation. (afLocationInfo=" + afLocationInfo + ", currentUtcTime=" + currentUtcTime + ")");

		mAfUpdate.updateWidgetRemoteViews("Downloading weather data...", false);

		Double latitude = afLocationInfo.getLatitude();
		Double longitude = afLocationInfo.getLongitude();

		if (latitude == null || longitude == null) {
			throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
		}

		HttpURLConnection connection = null;
		BufferedReader reader = null;
		long startTime = System.currentTimeMillis();

		try {
			String buildUrl = String.format(
					Locale.US,
					"https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=%.4f&lon=%.4f",
					latitude, longitude);

			Log.d(TAG, "Attempting to download weather data from URL=" + buildUrl);

			URL url = new URL(buildUrl);
			connection = AfUtils.setupHttpClient(url, mContext);
			connection.connect();

			int code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				if (code == 429) { // Too Many Requests
					throw new AfDataUpdateException(buildUrl, AfDataUpdateException.Reason.RATE_LIMITED);
				} else {
					throw new IOException("Invalid response from server: " + code);
				}
			}

			InputStream content = AfUtils.getGzipInputStream(connection);

			StringBuilder buffer = new StringBuilder();
			reader = new BufferedReader(new InputStreamReader(content));
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line).append("\n");
			}
			String jsonResponse = buffer.toString();

			if (jsonResponse.isEmpty()) {
				throw new AfDataUpdateException("API response was empty.");
			}

			mAfUpdate.updateWidgetRemoteViews("Parsing weather data...", false);

			Gson gson = new Gson();
			MetJsonData weatherData = gson.fromJson(jsonResponse, MetJsonData.class);

			if (weatherData == null || weatherData.properties == null || weatherData.properties.timeSeries == null) {
				throw new AfDataUpdateException("Failed to parse JSON or response was incomplete.");
			}

			TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
			dateFormat.setTimeZone(utcTimeZone);

			ArrayList<ContentValues> pointDataValues = new ArrayList<>();
			ArrayList<ContentValues> intervalDataValues = new ArrayList<>();

			long forecastValidTo = -1;
			if (weatherData.properties.meta != null && weatherData.properties.meta.expires != null) {
				forecastValidTo = dateFormat.parse(weatherData.properties.meta.expires).getTime();
			}

			for (TimeSeries ts : weatherData.properties.timeSeries) {
				long from = dateFormat.parse(ts.time).getTime();

				if (ts.data != null && ts.data.instant != null && ts.data.instant.details != null) {
					ContentValues cvPoint = new ContentValues();
					cvPoint.put(AfPointDataForecastColumns.LOCATION, afLocationInfo.getId());
					cvPoint.put(AfPointDataForecastColumns.TIME_ADDED, currentUtcTime);
					cvPoint.put(AfPointDataForecastColumns.TIME, from);

					if (ts.data.instant.details.air_temperature != null) {
						cvPoint.put(AfPointDataForecastColumns.TEMPERATURE, ts.data.instant.details.air_temperature);
					}
					if (ts.data.instant.details.relative_humidity != null) {
						cvPoint.put(AfPointDataForecastColumns.HUMIDITY, ts.data.instant.details.relative_humidity);
					}
					if (ts.data.instant.details.air_pressure_at_sea_level != null) {
						cvPoint.put(AfPointDataForecastColumns.PRESSURE, ts.data.instant.details.air_pressure_at_sea_level);
					}
					pointDataValues.add(cvPoint);
				}

				if (ts.data != null && ts.data.next1Hours != null) {
					ContentValues cvInterval = new ContentValues();
					cvInterval.put(AfIntervalDataForecastColumns.LOCATION, afLocationInfo.getId());
					cvInterval.put(AfIntervalDataForecastColumns.TIME_ADDED, currentUtcTime);
					cvInterval.put(AfIntervalDataForecastColumns.TIME_FROM, from);
					cvInterval.put(AfIntervalDataForecastColumns.TIME_TO, from + (60 * 60 * 1000));

					float precipitationAmount = 0.0f;
					Float precipitationMin = null;
					Float precipitationMax = null;

					if (ts.data.next1Hours.details != null) {
						if (ts.data.next1Hours.details.precipitation_amount != null) {
							precipitationAmount = ts.data.next1Hours.details.precipitation_amount;
						}
						precipitationMin = ts.data.next1Hours.details.precipitation_amount_min;
						precipitationMax = ts.data.next1Hours.details.precipitation_amount_max;
					}

					cvInterval.put(AfIntervalDataForecastColumns.RAIN_VALUE, precipitationAmount);

					float rainMinVal = (precipitationMin != null) ? precipitationMin : precipitationAmount;
					cvInterval.put(AfIntervalDataForecastColumns.RAIN_MINVAL, rainMinVal);

					float rainMaxVal = (precipitationMax != null && precipitationMax > rainMinVal) ? precipitationMax : rainMinVal;
					cvInterval.put(AfIntervalDataForecastColumns.RAIN_MAXVAL, rainMaxVal);

					if (ts.data.next1Hours.summary != null && ts.data.next1Hours.summary.symbol_code != null) {
						cvInterval.put(AfIntervalDataForecastColumns.WEATHER_ICON, mapWeatherIconSymbol(ts.data.next1Hours.summary.symbol_code));
					}
					intervalDataValues.add(cvInterval);
				}
			}

			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AfPointDataForecasts.CONTENT_URI, pointDataValues.toArray(new ContentValues[0]));
			resolver.bulkInsert(AfIntervalDataForecasts.CONTENT_URI, intervalDataValues.toArray(new ContentValues[0]));

			int numRedundantPointDataEntries = resolver.update(AfPointDataForecasts.CONTENT_URI, null, null, null);
			int numRedundantIntervalDataEntries = resolver.update(AfIntervalDataForecasts.CONTENT_URI, null, null, null);

			Log.d(TAG, String.format("update(): %d new PointData entries! %d redundant entries removed.", pointDataValues.size(), numRedundantPointDataEntries));
			Log.d(TAG, String.format("update(): %d new IntervalData entries! %d redundant entries removed.", intervalDataValues.size(), numRedundantIntervalDataEntries));

			afLocationInfo.setLastForecastUpdate(currentUtcTime);
			afLocationInfo.setForecastValidTo(forecastValidTo);
			afLocationInfo.setNextForecastUpdate(forecastValidTo);
			afLocationInfo.commit(mContext);

			long endTime = System.currentTimeMillis();
			Log.d(TAG, "Time spent parsing MET JSON data = " + (endTime - startTime) + " ms");

		} catch (Exception e) {
			Log.e(TAG, "update failed", e);
			throw new AfDataUpdateException(e.getMessage());
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) { /* ignore */ }
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	// DATA MODELS

	private static class MetJsonData {
		@SerializedName("properties")
		public WeatherProperties properties;
	}

	private static class WeatherProperties {
		@SerializedName("meta")
		public Meta meta;
		@SerializedName("timeseries")
		public ArrayList<TimeSeries> timeSeries;
	}

	private static class Meta {
		@SerializedName("updated_at")
		public String updatedAt;
		@SerializedName("units")
		public Units units;
		@SerializedName("expires")
		public String expires;
	}

	private static class Units {
	}

	private static class TimeSeries {
		@SerializedName("time")
		public String time;
		@SerializedName("data")
		public TimeData data;
	}

	private static class TimeData {
		@SerializedName("instant")
		public InstantData instant;
		@SerializedName("next_1_hours")
		public NextHoursData next1Hours;
	}

	private static class InstantData {
		@SerializedName("details")
		public InstantDetails details;
	}

	private static class InstantDetails {
		@SerializedName("air_temperature")
		public Float air_temperature;
		@SerializedName("relative_humidity")
		public Float relative_humidity;
		@SerializedName("air_pressure_at_sea_level")
		public Float air_pressure_at_sea_level;
	}

	private static class NextHoursData {
		@SerializedName("summary")
		public Summary summary;
		@SerializedName("details")
		public Details details;
	}

	private static class Summary {
		@SerializedName("symbol_code")
		public String symbol_code;
	}

	private static class Details {
		@SerializedName("precipitation_amount")
		public Float precipitation_amount;
		@SerializedName("precipitation_amount_min")
		public Float precipitation_amount_min;
		@SerializedName("precipitation_amount_max")
		public Float precipitation_amount_max;
	}
}
