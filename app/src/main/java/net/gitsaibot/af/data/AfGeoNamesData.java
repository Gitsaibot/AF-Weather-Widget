package net.gitsaibot.af.data;

import java.io.InputStream;
import java.util.Locale;

import net.gitsaibot.af.AfSettings;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.BuildConfig;
import net.gitsaibot.af.util.AfLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class AfGeoNamesData implements AfDataSource {

	public final static String TAG = "AfGeoNamesData";
	
	private Context mContext;
	private AfSettings mAfSettings;
	private AfUpdate mAfUpdate;
	
	private AfGeoNamesData(Context context, AfUpdate afUpdate, AfSettings afSettings)
	{
		mContext = context;
		mAfUpdate = afUpdate;
		mAfSettings = afSettings;
	}
	
	public static AfGeoNamesData build(Context context, AfUpdate afUpdate, AfSettings afSettings)
	{
		return new AfGeoNamesData(context, afUpdate, afSettings);
	}
	
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime) throws AfDataUpdateException
	{
		String timeZone = afLocationInfo.getTimeZone();
		String countryCode = mAfSettings.getLocationCountryCode(afLocationInfo.getId());
		
		mAfUpdate.updateWidgetRemoteViews("Getting timezone data...", false);
		
		Double latitude = afLocationInfo.getLatitude();
		Double longitude = afLocationInfo.getLongitude();
		
		if (latitude == null || longitude == null)
		{
			throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
		}
		
		if (timeZone != null) timeZone = timeZone.trim();
		if (countryCode != null) countryCode = countryCode.trim();
		
		if (timeZone == null || timeZone.length() == 0 || timeZone.equalsIgnoreCase("null") ||
		    countryCode == null || countryCode.length() == 0 || countryCode.equalsIgnoreCase("null"))
		{
			String url = String.format(
					Locale.US,
					"https://secure.geonames.org/timezoneJSON?lat=%.5f&lng=%.5f&username=" + BuildConfig.USER_GEONAMES,
					latitude, longitude);
			
			Log.d(TAG, "Retrieving timezone data from URL=" + url);
			
			try
			{
				HttpClient httpClient = AfUtils.setupHttpClient(mContext);
				HttpGet httpGet = new HttpGet(url);
				HttpResponse response = httpClient.execute(httpGet);

				if (response.getStatusLine().getStatusCode() == 429)
				{
					throw new AfDataUpdateException(url, AfDataUpdateException.Reason.RATE_LIMITED);
				}

				InputStream content = response.getEntity().getContent();
				
				String input = AfUtils.convertStreamToString(content);
				
				JSONObject jObject = new JSONObject(input);
				
				timeZone = jObject.getString("timezoneId");
				countryCode = jObject.getString("countryCode");
				
				Log.d(TAG, "Parsed TimeZone='" + timeZone + "' CountryCode='" + countryCode + "'");
				
				mAfSettings.setLocationCountryCode(afLocationInfo.getId(), countryCode);
				
				afLocationInfo.setTimeZone(timeZone);
				afLocationInfo.commit(mContext);
			}
			catch (Exception e)
			{
				Log.d(TAG, "Failed to retrieve timezone data. (" + e.getMessage() + ")");
				throw new AfDataUpdateException();
			}
		}
	}
	
}
