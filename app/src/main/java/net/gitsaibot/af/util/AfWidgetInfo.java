package net.gitsaibot.af.util;

import net.gitsaibot.af.AfProvider.AfViews;
import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.AfProvider.AfWidgetsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AfWidgetInfo {
	
	private int mAppWidgetId, mSize;
	
	private AfViewInfo mAfViewInfo;
	
	private AfWidgetSettings mAfWidgetSettings = null;
	
	public AfWidgetInfo(int appWidgetId, int size, AfViewInfo afViewInfo) {
		mAppWidgetId = appWidgetId;
		mSize = size;
		mAfViewInfo = afViewInfo;
	}
	
	public static AfWidgetInfo build(Context context, Uri widgetUri) throws Exception {
		Cursor cursor = null;
		
		try {
			ContentResolver resolver = context.getContentResolver();
			cursor = resolver.query(widgetUri, null, null, null, null);
			Log.d("AixWidgetInfo", "cursor" + cursor);
			if (cursor != null && cursor.moveToFirst())
			{
				int columnIndex = cursor.getColumnIndexOrThrow(AfWidgetsColumns.APPWIDGET_ID);
				int appWidgetId = cursor.getInt(columnIndex);
				
				int size = -1;
				columnIndex = cursor.getColumnIndex(AfWidgetsColumns.SIZE);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					size = cursor.getInt(columnIndex);
				}
				// Size may be invalid due to old versions. If non-valid, default to 4x1		
				if (!(size >= 1 && size <= 16))
				{
					size = AfWidgetsColumns.SIZE_LARGE_TINY;
				}
				
				AfViewInfo afViewInfo = null;
				columnIndex = cursor.getColumnIndex(AfWidgetsColumns.VIEWS);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					String viewString = cursor.getString(columnIndex);
					Uri viewUri = AfViews.CONTENT_URI.buildUpon().appendPath(viewString).build();
					afViewInfo = AfViewInfo.build(context, viewUri);
				}
				
				return new AfWidgetInfo(appWidgetId, size, afViewInfo);
			}
			else
			{
				throw new Exception("Failed to build AixWidgetInfo");
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
	
	public ContentValues buildContentValues() {
		ContentValues values = new ContentValues();
		values.put(AfWidgetsColumns.APPWIDGET_ID, mAppWidgetId);
		values.put(AfWidgetsColumns.SIZE, mSize);
		
		if (mAfViewInfo != null)
		{
			values.put(AfWidgetsColumns.VIEWS, Long.toString(mAfViewInfo.getId()));
		}
		else
		{
			values.putNull(AfWidgetsColumns.VIEWS);
		}
		
		return values;
	}
	
	public Uri commit(Context context)
	{
		if (mAfViewInfo != null)
		{
			mAfViewInfo.commit(context);
		}
		
		ContentValues values = buildContentValues();
		ContentResolver resolver = context.getContentResolver();
		
		Uri widgetUri = resolver.insert(AfWidgets.CONTENT_URI, values);
		
		return widgetUri;
	}
	
	public int getAppWidgetId() {
		return mAppWidgetId;
	}
	
	public int getNumColumns() {
		return (mSize - 1) / 4 + 1;
	}
	
	public int getNumRows() {
		return (mSize - 1) % 4 + 1;
	}
	
	public AfViewInfo getViewInfo()
	{
		return mAfViewInfo;
	}
	
	public AfWidgetSettings getWidgetSettings()
	{
		return mAfWidgetSettings;
	}
	
	public Uri getWidgetUri() {
		return ContentUris.withAppendedId(AfWidgets.CONTENT_URI, mAppWidgetId);
	}
	
	public void loadSettings(Context context)
	{
		mAfWidgetSettings = AfWidgetSettings.build(context, getWidgetUri());
	}
	
	public void setViewInfo(AfViewInfo viewInfo)
	{
		mAfViewInfo = viewInfo;
	}
	
	public void setViewInfo(AfLocationInfo afLocationInfo, int type)
	{
		if (mAfViewInfo != null)
		{
			mAfViewInfo.setAixLocationInfo(afLocationInfo);
			mAfViewInfo.setType(type);
		}
		else
		{
			mAfViewInfo = new AfViewInfo(null, afLocationInfo, type);
		}
	}
	
	public String toString() {
		return "AixWidgetInfo(" + mAppWidgetId + "," + mSize + "," + mAfViewInfo + ")";
	}
	
}
