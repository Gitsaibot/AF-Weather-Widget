package net.gitsaibot.af.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.IntervalData;
import net.gitsaibot.af.PointData;
import net.gitsaibot.af.util.AfLocationInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class AfNoaaWeatherData implements AfDataSource {

	public static final String TAG = "AfNoaaWeatherData";

	private final Context mContext;
	private final AfUpdate mAfUpdate;
	private final Gson mGson;
	private final SimpleDateFormat mNwsDateFormat;
	private final SimpleDateFormat mMetDateFormat;

	private static final Map<String, Integer> mMetWeatherIconMap = new HashMap<>();
	static {
		mMetWeatherIconMap.put("clearsky", AfUtils.WEATHER_ICON_CLEARSKY);
		mMetWeatherIconMap.put("cloudy", AfUtils.WEATHER_ICON_CLOUDY);
		mMetWeatherIconMap.put("fair", AfUtils.WEATHER_ICON_FAIR);
		mMetWeatherIconMap.put("partlycloudy", AfUtils.WEATHER_ICON_PARTLYCLOUD);
		mMetWeatherIconMap.put("fog", AfUtils.WEATHER_ICON_FOG);
		mMetWeatherIconMap.put("heavyrain", AfUtils.WEATHER_ICON_HEAVYRAIN);
		mMetWeatherIconMap.put("heavyrainandthunder", AfUtils.WEATHER_ICON_HEAVYRAINANDTHUNDER);
		mMetWeatherIconMap.put("heavyrainshowers", AfUtils.WEATHER_ICON_HEAVYRAINSHOWERS);
		mMetWeatherIconMap.put("heavyrainshowersandthunder", AfUtils.WEATHER_ICON_HEAVYRAINSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("lightrain", AfUtils.WEATHER_ICON_LIGHTRAIN);
		mMetWeatherIconMap.put("lightrainandthunder", AfUtils.WEATHER_ICON_LIGHTRAINANDTHUNDER);
		mMetWeatherIconMap.put("lightrainshowers", AfUtils.WEATHER_ICON_LIGHTRAINSHOWERS);
		mMetWeatherIconMap.put("lightrainshowersandthunder", AfUtils.WEATHER_ICON_LIGHTRAINSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("rain", AfUtils.WEATHER_ICON_RAIN);
		mMetWeatherIconMap.put("rainandthunder", AfUtils.WEATHER_ICON_RAINANDTHUNDER);
		mMetWeatherIconMap.put("rainshowers", AfUtils.WEATHER_ICON_RAINSHOWERS);
		mMetWeatherIconMap.put("rainshowersandthunder", AfUtils.WEATHER_ICON_RAINSHOWERANDTHUNDER);
		mMetWeatherIconMap.put("heavysleet", AfUtils.WEATHER_ICON_HEAVYSLEET);
		mMetWeatherIconMap.put("heavysleetandthunder", AfUtils.WEATHER_ICON_HEAVYSLEETANDTHUNDER);
		mMetWeatherIconMap.put("heavysleetshowers", AfUtils.WEATHER_ICON_HEAVYSLEETSHOWERS);
		mMetWeatherIconMap.put("heavysleetshowersandthunder", AfUtils.WEATHER_ICON_HEAVYSLEETSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("lightsleet", AfUtils.WEATHER_ICON_LIGHTSLEET);
		mMetWeatherIconMap.put("lightsleetandthunder", AfUtils.WEATHER_ICON_LIGHTSLEETANDTHUNDER);
		mMetWeatherIconMap.put("lightsleetshowers", AfUtils.WEATHER_ICON_LIGHTSLEETSHOWERS);
		mMetWeatherIconMap.put("lightsleetshowersandthunder", AfUtils.WEATHER_ICON_LIGHTSLEETSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("sleet", AfUtils.WEATHER_ICON_SLEET);
		mMetWeatherIconMap.put("sleetandthunder", AfUtils.WEATHER_ICON_SLEETANDTHUNDER);
		mMetWeatherIconMap.put("sleetshowers", AfUtils.WEATHER_ICON_SLEETSHOWERS);
		mMetWeatherIconMap.put("sleetshowersandthunder", AfUtils.WEATHER_ICON_SLEETSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("heavysnow", AfUtils.WEATHER_ICON_HEAVYSNOW);
		mMetWeatherIconMap.put("heavysnowandthunder", AfUtils.WEATHER_ICON_HEAVYSNOWANDTHUNDER);
		mMetWeatherIconMap.put("heavysnowshowers", AfUtils.WEATHER_ICON_HEAVYSNOWSHOWERS);
		mMetWeatherIconMap.put("heavysnowshowersandthunder", AfUtils.WEATHER_ICON_HEAVYSNOWSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("lightsnow", AfUtils.WEATHER_ICON_LIGHTSNOW);
		mMetWeatherIconMap.put("lightsnowandthunder", AfUtils.WEATHER_ICON_LIGHTSNOWANDTHUNDER);
		mMetWeatherIconMap.put("lightsnowshowers", AfUtils.WEATHER_ICON_LIGHTSNOWSHOWERS);
		mMetWeatherIconMap.put("lightsnowshowersandthunder", AfUtils.WEATHER_ICON_LIGHTSNOWSHOWERSANDTHUNDER);
		mMetWeatherIconMap.put("snow", AfUtils.WEATHER_ICON_SNOW);
		mMetWeatherIconMap.put("snowandthunder", AfUtils.WEATHER_ICON_SNOWANDTHUNDER);
		mMetWeatherIconMap.put("snowshowers", AfUtils.WEATHER_ICON_SNOWSHOWERS);
		mMetWeatherIconMap.put("snowshowersandthunder", AfUtils.WEATHER_ICON_SNOWSHOWERSANDTHUNDER);
	}

	public AfNoaaWeatherData(Context context, AfUpdate afUpdate) {
		mContext = context;
		mAfUpdate = afUpdate;
		mGson = new GsonBuilder().create();
		mNwsDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
		mNwsDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		mMetDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		mMetDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static AfNoaaWeatherData build(Context context, AfUpdate afUpdate) {
		return new AfNoaaWeatherData(context, afUpdate);
	}

	@Override
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException {
		try {
			mAfUpdate.updateWidgetRemoteViews("Getting weather data...", false);
			Log.d(TAG, "Hybrid update started for location: " + afLocationInfo);

			Double latitude = afLocationInfo.getLatitude();
			Double longitude = afLocationInfo.getLongitude();
			if (latitude == null || longitude == null) {
				throw new AfDataUpdateException("Missing location information.");
			}

			boolean isUsLocation = afLocationInfo.getTimeZone() != null && afLocationInfo.getTimeZone().startsWith("America/");
			Map<Long, CombinedData> combinedDataMap;

			if (isUsLocation) {
				try {
					mAfUpdate.updateWidgetRemoteViews("Getting local NWS data...", false);
					combinedDataMap = fetchNwsFirstData(latitude, longitude);
					Log.d(TAG, "Successfully fetched and enhanced forecast with NWS data.");
				} catch (Exception e) {
					Log.w(TAG, "NWS data fetch failed, falling back to MET.no", e);
					mAfUpdate.updateWidgetRemoteViews("Using MET.no data...", false);
					combinedDataMap = fetchMetNoDataFull(latitude, longitude);
				}
			} else {
				combinedDataMap = fetchMetNoDataFull(latitude, longitude);
			}

			List<ContentValues> pointDataList = new ArrayList<>();
			List<ContentValues> intervalDataList = new ArrayList<>();

			for (CombinedData data : combinedDataMap.values()) {
				PointData pointData = new PointData();
				pointData.time = data.startTime;
				pointData.timeAdded = currentUtcTime;
				if (data.temperature != null) pointData.temperature = data.temperature;
				if (data.humidity != null) pointData.humidity = data.humidity;
				if (data.pressure != null) pointData.pressure = data.pressure;
				pointDataList.add(pointData.buildContentValues(afLocationInfo.getId()));

				IntervalData intervalData = new IntervalData();
				intervalData.timeFrom = data.startTime;
				intervalData.timeTo = data.startTime + DateUtils.HOUR_IN_MILLIS;
				intervalData.timeAdded = currentUtcTime;
				intervalData.rainValue = data.rainValue;
				intervalData.rainMinValue = data.rainMinValue;
				intervalData.rainMaxValue = data.rainMaxValue;
				if (data.icon != null) intervalData.weatherIcon = data.icon;
				intervalDataList.add(intervalData.buildContentValues(afLocationInfo.getId()));
			}

			if (pointDataList.isEmpty()) {
				throw new AfDataUpdateException("No weather data could be processed.");
			}

			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AfPointDataForecasts.CONTENT_URI, pointDataList.toArray(new ContentValues[0]));
			resolver.bulkInsert(AfIntervalDataForecasts.CONTENT_URI, intervalDataList.toArray(new ContentValues[0]));

			// Use the provider's update mechanism to clear old data, matching the pattern in AfMetWeatherData
			int numRedundantPointDataEntries = resolver.update(AfPointDataForecasts.CONTENT_URI, null, null, null);
			int numRedundantIntervalDataEntries = resolver.update(AfIntervalDataForecasts.CONTENT_URI, null, null, null);

			Log.d(TAG, String.format("update(): %d new PointData entries! %d redundant entries removed.", pointDataList.size(), numRedundantPointDataEntries));
			Log.d(TAG, String.format("update(): %d new IntervalData entries! %d redundant entries removed.", intervalDataList.size(), numRedundantIntervalDataEntries));

			afLocationInfo.setLastForecastUpdate(currentUtcTime);
			afLocationInfo.setForecastValidTo(currentUtcTime + 48 * DateUtils.HOUR_IN_MILLIS);
			afLocationInfo.setNextForecastUpdate(currentUtcTime + 2 * DateUtils.HOUR_IN_MILLIS);
			afLocationInfo.commit(mContext);

		} catch (Exception e) {
			Log.e(TAG, "Failed to complete hybrid update.", e);
			throw new AfDataUpdateException("Failed during hybrid update: " + e.getMessage());
		}
	}

	private Map<Long, CombinedData> fetchNwsFirstData(double latitude, double longitude) throws Exception {
		Map<Long, CombinedData> dataMap = new TreeMap<>();

		String gridPointUrl = String.format(Locale.US, "https://api.weather.gov/points/%.4f,%.4f", latitude, longitude);
		NwsGridPoint gridPoint = getJsonFromUrl(gridPointUrl, NwsGridPoint.class, "application/geo+json");

		if (gridPoint == null || gridPoint.properties == null || gridPoint.properties.forecastHourly == null) {
			throw new AfDataUpdateException("NWS initial response did not contain hourly forecast URL.");
		}

		NwsForecast forecast = getJsonFromUrl(gridPoint.properties.forecastHourly, NwsForecast.class, "application/geo+json");
		if (forecast == null || forecast.properties == null || forecast.properties.periods == null || forecast.properties.periods.isEmpty()) {
			throw new AfDataUpdateException("Could not retrieve NWS hourly forecast for temp/humidity.");
		}

		for (NwsForecast.Period period : forecast.properties.periods) {
			long startTime = mNwsDateFormat.parse(period.startTime).getTime();
			CombinedData data = dataMap.computeIfAbsent(startTime, k -> new CombinedData(startTime));
			data.temperature = convertTemperature(period.temperature, period.temperatureUnit);
			if (period.relativeHumidity != null && period.relativeHumidity.value != null) {
				data.humidity = period.relativeHumidity.value;
			}
		}

		try {
			MetJsonData metData = getJsonFromUrl(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=%.4f&lon=%.4f", latitude, longitude), MetJsonData.class, "application/json");
			if (metData != null && metData.properties != null && metData.properties.timeSeries != null) {
				for (MetJsonData.TimeSeries ts : metData.properties.timeSeries) {
					long startTime = mMetDateFormat.parse(ts.time).getTime();
					CombinedData data = dataMap.get(startTime);
					if (data != null && ts.data != null) {
						if (ts.data.instant != null && ts.data.instant.details != null && ts.data.instant.details.air_pressure_at_sea_level != null) {
							data.pressure = ts.data.instant.details.air_pressure_at_sea_level;
						}
						if (ts.data.next1Hours != null) {
							if (ts.data.next1Hours.summary != null) {
								data.icon = mapWeatherIconSymbol(ts.data.next1Hours.summary.symbol_code);
							}
							if (ts.data.next1Hours.details != null) {
								data.rainValue = ts.data.next1Hours.details.precipitation_amount != null ? ts.data.next1Hours.details.precipitation_amount : 0.0f;
								data.rainMinValue = ts.data.next1Hours.details.precipitation_amount_min != null ? ts.data.next1Hours.details.precipitation_amount_min : data.rainValue;
								data.rainMaxValue = ts.data.next1Hours.details.precipitation_amount_max != null ? ts.data.next1Hours.details.precipitation_amount_max : data.rainValue;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "Failed to enhance with MET.no rain/icon/pressure data", e);
		}

		interpolateMissingValues(new ArrayList<>(dataMap.values()));

		return dataMap;
	}

	private Map<Long, CombinedData> fetchMetNoDataFull(double latitude, double longitude) throws Exception {
		Map<Long, CombinedData> dataMap = new TreeMap<>();
		String urlString = String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=%.4f&lon=%.4f", latitude, longitude);
		MetJsonData weatherData = getJsonFromUrl(urlString, MetJsonData.class, "application/json");

		if (weatherData == null || weatherData.properties == null || weatherData.properties.timeSeries == null) {
			throw new AfDataUpdateException("MET.no JSON response was incomplete.");
		}

		for (MetJsonData.TimeSeries ts : weatherData.properties.timeSeries) {
			long startTime = mMetDateFormat.parse(ts.time).getTime();
			CombinedData data = dataMap.computeIfAbsent(startTime, k -> new CombinedData(startTime));

			if (ts.data != null) {
				if (ts.data.instant != null && ts.data.instant.details != null) {
					data.temperature = ts.data.instant.details.air_temperature;
					data.humidity = ts.data.instant.details.relative_humidity;
					data.pressure = ts.data.instant.details.air_pressure_at_sea_level;
				}
				if (ts.data.next1Hours != null) {
					if (ts.data.next1Hours.summary != null) {
						data.icon = mapWeatherIconSymbol(ts.data.next1Hours.summary.symbol_code);
					}
					if (ts.data.next1Hours.details != null) {
						data.rainValue = ts.data.next1Hours.details.precipitation_amount != null ? ts.data.next1Hours.details.precipitation_amount : 0.0f;
						data.rainMinValue = ts.data.next1Hours.details.precipitation_amount_min != null ? ts.data.next1Hours.details.precipitation_amount_min : data.rainValue;
						data.rainMaxValue = ts.data.next1Hours.details.precipitation_amount_max != null ? ts.data.next1Hours.details.precipitation_amount_max : data.rainValue;
					}
				}
			}
		}
		return dataMap;
	}

	private static int mapWeatherIconSymbol(String symbolCode) {
		if (symbolCode == null) return 0;
		String symbol = symbolCode.split("_")[0].toLowerCase(Locale.US);
		return mMetWeatherIconMap.getOrDefault(symbol, 0);
	}

	private void interpolateMissingValues(List<CombinedData> dataList) {
		interpolateFloatValue(dataList, (data) -> data.temperature, (data, val) -> data.temperature = val);
		interpolateFloatValue(dataList, (data) -> data.humidity, (data, val) -> data.humidity = val);
		interpolateFloatValue(dataList, (data) -> data.pressure, (data, val) -> data.pressure = val);
	}

	private void interpolateFloatValue(List<CombinedData> dataList, java.util.function.Function<CombinedData, Float> getter, java.util.function.BiConsumer<CombinedData, Float> setter) {
		for (int i = 0; i < dataList.size(); i++) {
			if (getter.apply(dataList.get(i)) == null) {
				CombinedData prevData = null;
				for (int j = i - 1; j >= 0; j--) {
					if (getter.apply(dataList.get(j)) != null) {
						prevData = dataList.get(j);
						break;
					}
				}

				CombinedData nextData = null;
				for (int j = i + 1; j < dataList.size(); j++) {
					if (getter.apply(dataList.get(j)) != null) {
						nextData = dataList.get(j);
						break;
					}
				}

				if (prevData != null && nextData != null) {
					long timeDiff = nextData.startTime - prevData.startTime;
					if (timeDiff > 0) {
						float valueDiff = getter.apply(nextData) - getter.apply(prevData);
						float timeRatio = (float) (dataList.get(i).startTime - prevData.startTime) / timeDiff;
						setter.accept(dataList.get(i), getter.apply(prevData) + (valueDiff * timeRatio));
					}
				} else if (prevData != null) {
					setter.accept(dataList.get(i), getter.apply(prevData));
				} else if (nextData != null) {
					setter.accept(dataList.get(i), getter.apply(nextData));
				}
			}
		}
	}

	private <T> T getJsonFromUrl(String urlString, Class<T> classOfT, String acceptHeader) throws Exception {
		HttpURLConnection connection = null;
		InputStreamReader reader = null;
		try {
			URL url = new URL(urlString);
			connection = AfUtils.setupHttpClient(url, mContext);
			connection.setRequestProperty("User-Agent", AfUtils.getUserAgent(mContext));
			connection.setRequestProperty("Accept", acceptHeader);

			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				Log.w(TAG, "HTTP error " + responseCode + " for URL: " + urlString);
				return null;
			}

			InputStream inputStream = AfUtils.getGzipInputStream(connection);
			reader = new InputStreamReader(inputStream);
			return mGson.fromJson(reader, classOfT);
		} finally {
			if (reader != null) try { reader.close(); } catch (Exception e) { /* ignore */ }
			if (connection != null) connection.disconnect();
		}
	}

	private float convertTemperature(float value, String unit) {
		if (unit != null && unit.equalsIgnoreCase("F")) {
			return (value - 32.0f) * 5.0f / 9.0f;
		}
		return value;
	}

	// --- DATA MODELS ---
	private static class CombinedData {
		long startTime;
		Float temperature = null;
		Float humidity = null;
		Float pressure = null;
		float rainValue = 0.0f;
		float rainMinValue = 0.0f;
		float rainMaxValue = 0.0f;
		Integer icon = null;

		CombinedData(long startTime) {
			this.startTime = startTime;
		}
	}

	private static class NwsGridPoint {
		@SerializedName("properties") public GridPointProperties properties;
		private static class GridPointProperties {
			@SerializedName("forecastHourly") public String forecastHourly;
			@SerializedName("forecastGridData") public String forecastGridData;
		}
	}

	private static class NwsForecast {
		@SerializedName("properties") public ForecastProperties properties;
		private static class ForecastProperties {
			@SerializedName("periods") public List<Period> periods;
		}
		private static class Period {
			@SerializedName("startTime") public String startTime;
			@SerializedName("endTime") public String endTime;
			@SerializedName("temperature") public float temperature;
			@SerializedName("temperatureUnit") public String temperatureUnit;
			@SerializedName("relativeHumidity") public ValueItem relativeHumidity;
		}
	}

	private static class ValueItem {
		@SerializedName("value") public Float value;
	}

	private static class MetJsonData {
		@SerializedName("properties") public WeatherProperties properties;
		private static class WeatherProperties {
			@SerializedName("timeseries") public ArrayList<TimeSeries> timeSeries;
		}
		private static class TimeSeries {
			@SerializedName("time") public String time;
			@SerializedName("data") public TimeData data;
		}
		private static class TimeData {
			@SerializedName("instant") public InstantData instant;
			@SerializedName("next_1_hours") public NextHoursData next1Hours;
		}
		private static class InstantData {
			@SerializedName("details") public InstantDetails details;
			private static class InstantDetails {
				@SerializedName("air_temperature") public Float air_temperature;
				@SerializedName("relative_humidity") public Float relative_humidity;
				@SerializedName("air_pressure_at_sea_level") public Float air_pressure_at_sea_level;
			}
		}
		private static class NextHoursData {
			@SerializedName("summary") public Summary summary;
			@SerializedName("details") public Details details;
			private static class Summary {
				@SerializedName("symbol_code") public String symbol_code;
			}
			private static class Details {
				@SerializedName("precipitation_amount") public Float precipitation_amount;
				@SerializedName("precipitation_amount_min") public Float precipitation_amount_min;
				@SerializedName("precipitation_amount_max") public Float precipitation_amount_max;
			}
		}
	}
}
