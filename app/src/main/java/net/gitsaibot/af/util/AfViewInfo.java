package net.gitsaibot.af.util;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfViews;
import net.gitsaibot.af.AfProvider.AfViewsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AfViewInfo {
	
	private int mType;
	private AfLocationInfo mAfLocationInfo;
	private Uri mViewUri;
	
	public AfViewInfo(Uri viewUri, AfLocationInfo afLocationInfo, int type)
	{
		mViewUri = viewUri;
		mAfLocationInfo = afLocationInfo;
		mType = type;
	}
	
	public static AfViewInfo build(Context context, Uri viewUri)
			throws Exception
	{
		Cursor cursor = null;
		try
		{
			ContentResolver contentResolver = context.getContentResolver();
			cursor = contentResolver.query(viewUri, null, null, null, null);
			
			if (cursor != null && cursor.moveToFirst())
			{
				AfLocationInfo afLocationInfo = null;
				
				int columnIndex = cursor.getColumnIndex(AfViewsColumns.LOCATION);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					long locationId = cursor.getLong(columnIndex);
					Uri aixLocationUri = ContentUris.withAppendedId(AfLocations.CONTENT_URI, locationId);
					afLocationInfo = AfLocationInfo.build(context, aixLocationUri);
				}
				
				int type = AfViewsColumns.TYPE_DETAILED;
				columnIndex = cursor.getColumnIndex(AfViewsColumns.TYPE);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					type = cursor.getInt(columnIndex);
				}
				
				return new AfViewInfo(viewUri, afLocationInfo, type);
			}
			else
			{
				throw new Exception("Failed to build AfViewInfo");
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
	
	public ContentValues buildContentValues()
	{
		ContentValues values = new ContentValues();
		
		if (mAfLocationInfo != null)
		{
			values.put(AfViewsColumns.LOCATION, mAfLocationInfo.getId());
		}
		else
		{
			values.putNull(AfViewsColumns.LOCATION);
		}
		
		values.put(AfViewsColumns.TYPE, mType);
		
		return values;
	}

	public Uri commit(Context context)
	{
		if (mAfLocationInfo != null)
		{
			mAfLocationInfo.commit(context);
		}
		
		ContentValues values = buildContentValues();
		ContentResolver resolver = context.getContentResolver();
		
		if (mViewUri != null)
		{
			resolver.update(mViewUri, values, null, null);
		}
		else
		{
			mViewUri = resolver.insert(AfViews.CONTENT_URI, values);
		}
		
		return mViewUri;
	}
	
	public long getId() {
		if (mViewUri != null)
		{
			return ContentUris.parseId(mViewUri);
		}
		else
		{
			return -1;
		}
	}
	
	public AfLocationInfo getLocationInfo() {
		return mAfLocationInfo;
	}
	
	public int getType() {
		return mType;
	}

	public void setAixLocationInfo(AfLocationInfo afLocationInfo)
	{
		mAfLocationInfo = afLocationInfo;
	}
	
	public void setType(int type)
	{
		mType = type;
	}
	
	public void setUri(Uri uri)
	{
		mViewUri = uri;
	}
	
	@Override
	public String toString() {
		return "AfViewInfo(" + mViewUri + "," + mType + "," + mAfLocationInfo + ")";
	}
	
}
