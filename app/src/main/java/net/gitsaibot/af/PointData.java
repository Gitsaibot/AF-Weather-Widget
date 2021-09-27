package net.gitsaibot.af;

import net.gitsaibot.af.AfProvider.AfPointDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import android.content.ContentValues;
import android.database.Cursor;

public class PointData {

	public Long timeAdded = null;
	public Long time = null;
	
	public Float temperature = null;
	public Float humidity = null;
	public Float pressure = null;
	
	public PointData() { }
	
	public ContentValues buildContentValues(long locationId)
	{
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(AfPointDataForecasts.LOCATION, locationId);
		
		if (timeAdded != null) contentValues.put(AfPointDataForecasts.TIME_ADDED, timeAdded);
		if (time != null) contentValues.put(AfPointDataForecasts.TIME, time);
		if (temperature != null) contentValues.put(AfPointDataForecastColumns.TEMPERATURE, temperature);
		if (humidity != null) contentValues.put(AfPointDataForecastColumns.HUMIDITY, humidity);
		if (pressure != null) contentValues.put(AfPointDataForecastColumns.PRESSURE, pressure);
		
		return contentValues;
	}
	
	public static PointData buildFromCursor(Cursor c) {
		PointData pointData = new PointData();
		
		int columnIndex = c.getColumnIndex(AfPointDataForecastColumns.TIME_ADDED);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.timeAdded = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AfPointDataForecastColumns.TIME);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.time = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AfPointDataForecastColumns.TEMPERATURE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.temperature = c.getFloat(columnIndex);
		
		columnIndex = c.getColumnIndex(AfPointDataForecastColumns.HUMIDITY);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.humidity = c.getFloat(columnIndex);
		
		columnIndex = c.getColumnIndex(AfPointDataForecastColumns.PRESSURE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.pressure = c.getFloat(columnIndex);

		return pointData;
	}
	
}
