package net.gitsaibot.af.util;

import java.util.TimeZone;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfLocationsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class AfLocationInfo {

	private String mTitle = null;
	private String mTitleDetailed = null;
	private String mTimeZone = null;
	private Integer mType = null;
	private Long mTimeOfLastFix = null;
	private Double mLatitude = null;
	private Double mLongitude = null;
	private Long mLastForecastUpdate = null;
	private Long mForecastValidTo = null;
	private Long mNextForecastUpdate = null;
	
	private Uri mLocationUri = null;
	
	public AfLocationInfo() { }
	
	public static AfLocationInfo build(Context context, Uri locationUri)
			throws Exception
	{
		Cursor cursor = null;
		try
		{
			ContentResolver contentResolver = context.getContentResolver();
			cursor = contentResolver.query(locationUri, null, null, null, null);
			
			if (cursor != null && cursor.moveToFirst())
			{
				return AfLocationInfo.buildFromCursor(cursor);
			}
			else
			{
				throw new Exception("Failed to build AfLocationInfo");
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}
	
	private ContentValues buildContentValues()
	{
		ContentValues values = new ContentValues();
		
		if (mTitle != null)
		{
			values.put(AfLocationsColumns.TITLE, mTitle);
		}
		else
		{
			values.putNull(AfLocationsColumns.TITLE);
		}
		
		if (mTitleDetailed != null)
		{
			values.put(AfLocationsColumns.TITLE_DETAILED, mTitleDetailed);
		}
		else
		{
			values.putNull(AfLocationsColumns.TITLE_DETAILED);
		}
		
		if (mTimeZone != null)
		{
			values.put(AfLocationsColumns.TIME_ZONE, mTimeZone);
		}
		else
		{
			values.putNull(AfLocationsColumns.TIME_ZONE);
		}
		
		if (mType != null)
		{
			values.put(AfLocationsColumns.TYPE, mType);
		}
		else
		{
			values.putNull(AfLocationsColumns.TYPE);
		}
		
		if (mTimeOfLastFix != null)
		{
			values.put(AfLocationsColumns.TIME_OF_LAST_FIX, mTimeOfLastFix);
		}
		else
		{
			values.putNull(AfLocationsColumns.TIME_OF_LAST_FIX);
		}
		
		if (mLatitude != null)
		{
			values.put(AfLocationsColumns.LATITUDE, mLatitude);
		}
		else
		{
			values.putNull(AfLocationsColumns.LATITUDE);
		}
		
		if (mLongitude != null)
		{
			values.put(AfLocationsColumns.LONGITUDE, mLongitude);
		}
		else
		{
			values.putNull(AfLocationsColumns.LONGITUDE);
		}
		
		if (mLastForecastUpdate != null)
		{
			values.put(AfLocationsColumns.LAST_FORECAST_UPDATE, mLastForecastUpdate);
		}
		else
		{
			values.putNull(AfLocationsColumns.LAST_FORECAST_UPDATE);
		}
		
		if (mForecastValidTo != null)
		{
			values.put(AfLocationsColumns.FORECAST_VALID_TO, mForecastValidTo);
		}
		else
		{
			values.putNull(AfLocationsColumns.FORECAST_VALID_TO);
		}
		
		if (mNextForecastUpdate != null)
		{
			values.put(AfLocationsColumns.NEXT_FORECAST_UPDATE, mNextForecastUpdate);
		}
		else
		{
			values.putNull(AfLocationsColumns.NEXT_FORECAST_UPDATE);
		}
		
		return values;
	}
	
	public static AfLocationInfo buildFromCursor(Cursor c) {
		AfLocationInfo locationInfo = new AfLocationInfo();
		
		int columnIndex = c.getColumnIndexOrThrow(BaseColumns._ID);
		long locationId = c.getLong(columnIndex);
		locationInfo.mLocationUri = ContentUris.withAppendedId(AfLocations.CONTENT_URI, locationId);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.TITLE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mTitle = c.getString(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.TITLE_DETAILED);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mTitleDetailed = c.getString(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.TIME_ZONE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mTimeZone = c.getString(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.TYPE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mType = c.getInt(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.TIME_OF_LAST_FIX);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mTimeOfLastFix = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.LATITUDE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mLatitude = c.getDouble(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.LONGITUDE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mLongitude = c.getDouble(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.LAST_FORECAST_UPDATE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mLastForecastUpdate = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.FORECAST_VALID_TO);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mForecastValidTo = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AfLocationsColumns.NEXT_FORECAST_UPDATE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) locationInfo.mNextForecastUpdate = c.getLong(columnIndex);
		
		return locationInfo;
	}
	
	public TimeZone buildTimeZone() {
		if (mTimeZone != null)
		{
			return TimeZone.getTimeZone(mTimeZone);
		}
		else
		{
			return null;
		}
	}
	
	public Uri commit(Context context)
	{
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = buildContentValues();
		
		if (mLocationUri != null)
		{
			resolver.update(mLocationUri, values, null, null);
		}
		else
		{
			mLocationUri = resolver.insert(AfLocations.CONTENT_URI, values);
		}
		
		return mLocationUri;
	}
	
	public Long getForecastValidTo() {
		return mForecastValidTo;
	}
	
	public long getId() {
		if (mLocationUri != null)
		{
			return ContentUris.parseId(mLocationUri);
		}
		else
		{
			return -1;
		}
	}
	
	public Long getLastForecastUpdate() {
		return mLastForecastUpdate;
	}
	
	public Double getLatitude() {
		return mLatitude;
	}
	
	public Uri getLocationUri() {
		return mLocationUri;
	}
	
	public Double getLongitude() {
		return mLongitude;
	}
	
	public Long getNextForecastUpdate() {
		return mNextForecastUpdate;
	}
	
	public Long getTimeOfLastFix() {
		return mTimeOfLastFix;
	}
	
	public String getTimeZone() {
		return mTimeZone;
	}
	
	public String getOffset() {
		ZoneId zone = ZoneId.of(mTimeZone);
		String offset = OffsetDateTime.now(zone).getOffset().toString();

		if (offset.equals("Z")) {
			offset = "+00:00";
		}

		return offset;
	}

	public String getTitle() {
		return mTitle;
	}
	
	public String getTitleDetailed() {
		return mTitleDetailed;
	}
	
	public Integer getType() {
		return mType;
	}
	
	public void setForecastValidTo(Long forecastValidTo) {
		mForecastValidTo = forecastValidTo;
	}
	
	public void setLastForecastUpdate(Long lastForecastUpdate) {
		mLastForecastUpdate = lastForecastUpdate;
	}
	
	public void setLatitude(Double latitude) {
		mLatitude = latitude;
	}
	
	public void setLongitude(Double longitude) {
		mLongitude = longitude;
	}
	
	public void setNextForecastUpdate(Long nextForecastUpdate) {
		mNextForecastUpdate = nextForecastUpdate;
	}
	
	public void setTimeOfLastFix(Long timeOfLastFix) {
		mTimeOfLastFix = timeOfLastFix;
	}
	
	public void setTimeZone(String timeZone) {
		mTimeZone = timeZone;
	}
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public void setTitleDetailed(String titleDetailed) {
		mTitleDetailed = titleDetailed;
	}
	
	public void setType(Integer type) {
		mType = type;
	}

	@Override
	public String toString() {
		return "AfLocationInfo(" + mLocationUri + "," + mTitle + "," + mTitleDetailed + ","
				+ mTimeZone + "," + mType + "," + mTimeOfLastFix + ","
				+ mLatitude + "," + mLongitude + ","
				+ mLastForecastUpdate + ","
				+ mForecastValidTo + ","
				+ mNextForecastUpdate + ")";
	}
	
}
