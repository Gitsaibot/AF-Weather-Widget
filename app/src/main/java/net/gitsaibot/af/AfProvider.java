package net.gitsaibot.af;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

public class AfProvider extends ContentProvider {

	private static final String TAG = "AfProvider";
	private static final boolean LOGD = false;
	
	public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
	
	public interface AfWidgetsColumns {
		String APPWIDGET_ID = BaseColumns._ID;
		
		/* The size of the widget in the format COLUMNS_ROWS
		 * where TINY=1, SMALL=2, MEDIUM=3, LARGE=4 */
		String SIZE = "size";
		int SIZE_INVALID = 0;
		
		int SIZE_TINY_TINY = 1;
		int SIZE_TINY_SMALL = 2;
		int SIZE_TINY_MEDIUM = 3;
		int SIZE_TINY_LARGE = 4;
		
		int SIZE_SMALL_TINY = 5;
		int SIZE_SMALL_SMALL = 6;
		int SIZE_SMALL_MEDIUM = 7;
		int SIZE_SMALL_LARGE = 8;
		
		int SIZE_MEDIUM_TINY = 9;
		int SIZE_MEDIUM_SMALL = 10;
		int SIZE_MEDIUM_MEDIUM = 11;
		int SIZE_MEDIUM_LARGE = 12;
		
		int SIZE_LARGE_TINY = 13;
		int SIZE_LARGE_SMALL = 14;
		int SIZE_LARGE_MEDIUM = 15;
		int SIZE_LARGE_LARGE = 16;
		
		/* A colon-separated array in string format of the view IDs linked to the widget */
		String VIEWS = "views";
		
		int APPWIDGET_ID_COLUMN = 0;
		int SIZE_COLUMN = 1;
		int VIEWS_COLUMN = 2;

		String[] ALL_COLUMNS = new String[] {
				APPWIDGET_ID, SIZE, VIEWS };
	}
	
	public static class AfWidgets implements BaseColumns, AfWidgetsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixwidget";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixwidget";
		
		public static final String TWIG_SETTINGS = "settings";
	}
	
	public interface AfViewsColumns {
		String VIEW_ID = BaseColumns._ID;
		/* The location row ID in the AixLocations table for the view location */
		String LOCATION = "location";
		
		/* The type of a specific view, e.g. detailed or long-term */
		String TYPE = "type";
		int TYPE_DETAILED = 1;
		
		int VIEW_ID_COLUMN = 0;
		int LOCATION_COLUMN = 1;
		int TYPE_COLUMN = 2;

		String[] ALL_COLUMNS = new String[] {
				VIEW_ID, LOCATION, TYPE };
	}
	
	public static class AfViews implements BaseColumns, AfViewsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviews");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixview";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixview";
		
		public static final String TWIG_SETTINGS = "settings";
		public static final String TWIG_LOCATION = "location";
	}
	
	public interface AfLocationsColumns {
		String TITLE = "title";
		String TITLE_DETAILED = "title_detailed";
		String TIME_ZONE = "timeZone";
		
		/* The type of the location; whether it is set statically or updated dynamically */
		String TYPE = "type";
		int LOCATION_STATIC = 1;
		
		/* The time when the current location fix was set */
		String TIME_OF_LAST_FIX = "time_of_last_fix";
		
		String LATITUDE = "latitude";
		
		String LONGITUDE = "longitude";
		
		/* The time when the forecasts for the location was last updated */
		String LAST_FORECAST_UPDATE = "last_forecast_update";
		
		/* The time when the forecasts for the location is no longer valid */
		String FORECAST_VALID_TO = "forecast_valid_to";
		
		/* The time when the forecasts for the location should be updated next */
		String NEXT_FORECAST_UPDATE = "next_forecast_update";
		
		int LOCATION_ID_COLUMN = 0;
		int TITLE_COLUMN = 1;
		int TITLE_DETAILED_COLUMN = 2;
		int TIME_ZONE_COLUMN = 3;
		int TYPE_COLUMN = 4;
		int TIME_OF_LAST_FIX_COLUMN = 5;
		int LATITUDE_COLUMN = 6;
		int LONGITUDE_COLUMN = 7;
		int LAST_FORECAST_UPDATE_COLUMN = 8;
		int FORECAST_VALID_TO_COLUMN = 9;
		int NEXT_FORECAST_UPDATE_COLUMN = 10;

		String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID,
				TITLE,
				TITLE_DETAILED,
				TIME_ZONE,
				TYPE,
				TIME_OF_LAST_FIX,
				LATITUDE,
				LONGITUDE,
				LAST_FORECAST_UPDATE,
				FORECAST_VALID_TO,
				NEXT_FORECAST_UPDATE };
	}
	
	public static class AfLocations implements BaseColumns, AfLocationsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixlocations");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixlocation";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixlocation";
		
		public static final String TWIG_POINTDATAFORECASTS = "pointdata_forecasts";
		public static final String TWIG_INTERVALDATAFORECASTS = "intervaldata_forecasts";
		public static final String TWIG_SUNMOONDATA = "sunmoondata";
	}
	
	public interface AfPointDataForecastColumns {
		String LOCATION = "location";
		String TIME_ADDED = "timeAdded";
		String TIME = "time";
		String TEMPERATURE = "temperature";
		String HUMIDITY = "humidity";
		String PRESSURE = "pressure";
		
		int LOCATION_COLUMN = 1;
		int TIME_ADDED_COLUMN = 2;
		int TIME_COLUMN = 3;
		int TEMPERATURE_COLUMN = 4;
		int HUMIDITY_COLUMN = 5;
		int PRESSURE_COLUMN = 6;

		String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, LOCATION, TIME_ADDED, TIME, TEMPERATURE, HUMIDITY, PRESSURE };
	}
	
	public static class AfPointDataForecasts implements BaseColumns, AfPointDataForecastColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixpointdataforecasts");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixpointdataforecasts";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixpointdataforecast";
	}
	
	public interface AfIntervalDataForecastColumns {
		String LOCATION = "location";
		String TIME_ADDED = "timeAdded";
		String TIME_FROM = "timeFrom";
		String TIME_TO = "timeTo";
		String RAIN_VALUE = "rainValue";
		String RAIN_MINVAL = "rainLowVal";
		String RAIN_MAXVAL = "rainMaxVal";
		String WEATHER_ICON = "weatherIcon";
		
		int LOCATION_COLUMN = 1;
		int TIME_ADDED_COLUMN = 2;
		int TIME_FROM_COLUMN = 3;
		int TIME_TO_COLUMN = 4;
		int RAIN_VALUE_COLUMN = 5;
		int RAIN_MINVAL_COLUMN = 6;
		int RAIN_MAXVAL_COLUMN = 7;
		int WEATHER_ICON_COLUMN = 8;

		String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, LOCATION, TIME_ADDED, TIME_FROM, TIME_TO, RAIN_VALUE, RAIN_MINVAL, RAIN_MAXVAL, WEATHER_ICON };
	}
	
	public static class AfIntervalDataForecasts implements BaseColumns, AfIntervalDataForecastColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixintervaldataforecasts");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixintervaldataforecasts";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixintervaldataforecast";
	}
	
	public interface AfSunMoonDataColumns {
		String LOCATION = "location";
		String TIME_ADDED = "timeAdded";
		String DATE = "date";
		String SUN_RISE = "sunRise";
		String SUN_SET = "sunSet";
		String MOON_RISE = "moonRise";
		String MOON_SET = "moonSet";
		String MOON_PHASE = "moonPhase";
		
		int NO_MOON_PHASE_DATA = -1;
		int NEW_MOON = 1;
		int WAXING_CRESCENT = 2;
		int FIRST_QUARTER = 3;
		int WAXING_GIBBOUS = 4;
		int FULL_MOON = 5;
		int WANING_GIBBOUS = 6;
		int LAST_QUARTER = 7;
		int WANING_CRESCENT = 8;
		int DARK_MOON = 9;
		
		int LOCATION_COLUMN = 1;
		int TIME_ADDED_COLUMN = 2;
		int DATE_COLUMN = 3;
		int SUN_RISE_COLUMN = 4;
		int SUN_SET_COLUMN = 5;
		int MOON_RISE_COLUMN = 6;
		int MOON_SET_COLUMN = 7;
		int MOON_PHASE_COLUMN = 8;

		String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID,
				LOCATION,
				TIME_ADDED,
				DATE,
				SUN_RISE,
				SUN_SET,
				MOON_RISE,
				MOON_SET,
				MOON_PHASE
		};
	}
	
	public static class AfSunMoonData implements BaseColumns, AfSunMoonDataColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixsunmoondata");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixsunmoondata";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixsunmoondata";
		
		public static final long NEVER_RISE = -1;
		public static final long NEVER_SET = -2;
	}
	
	public interface AfSettingsColumns {
		String ROW_ID = "rowId";
		String KEY = "rowKey";
		String VALUE = "value";
		
		int SETTING_ID_COLUMN = 0;
		int ROW_ID_COLUMN = 1;
		int KEY_COLUMN = 2;
		int VALUE_COLUMN = 3;

		String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, ROW_ID, KEY, VALUE };
	}
	
	public static abstract class AfSettings implements BaseColumns, AfSettingsColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixsetting";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixsetting";
	}
	
	public static class AfWidgetSettingsDatabase extends AfSettings {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgetsettings");
		
//		public static final String TEMPERATURE_UNITS = "temperatureUnits";
		public static final int TEMPERATURE_UNITS_CELSIUS = 1;
		public static final int TEMPERATURE_UNITS_FAHRENHEIT = 2;
//		
//		public static final String PRECIPITATION_UNITS = "precipitationUnits";
		public static final int PRECIPITATION_UNITS_MM = 1;
		public static final int PRECIPITATION_UNITS_INCHES = 2;

//		public static final String PRECIPITATION_SCALE = "precipitationScale";
//		
//		public static final String BACKGROUND_COLOR = "backgroundColor";
//		public static final String TEXT_COLOR = "textColor";
//		public static final String LOCATION_BACKGROUND_COLOR = "locationBackgroundColor";
//		public static final String LOCATION_TEXT_COLOR = "locationTextColor";
//		public static final String GRID_COLOR = "gridColor";
//		public static final String GRID_OUTLINE_COLOR = "gridOutlineColor";
//		public static final String MAX_RAIN_COLOR = "maxRainColor";
//		public static final String MIN_RAIN_COLOR = "minRainColor";
//		public static final String ABOVE_FREEZING_COLOR = "aboveFreezingColor";
//		public static final String BELOW_FREEZING_COLOR = "belowFreezingColor";
	}

	public static class AfViewSettings extends AfSettings {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviewsettings");
	}
	
	private static final String TABLE_AIXWIDGETS = "aixwidgets";
	private static final String TABLE_AIXWIDGETSETTINGS = "aixwidgetsettings";
	private static final String TABLE_AIXVIEWS = "aixviews";
	private static final String TABLE_AIXVIEWSETTINGS = "aixviewsettings";
	private static final String TABLE_AIXLOCATIONS = "aixlocations";
	private static final String TABLE_AIXFORECASTS = "aixforecasts";
	private static final String TABLE_AIXPOINTDATAFORECASTS = "aixpointdataforecasts";
	private static final String TABLE_AIXINTERVALDATAFORECASTS = "aixintervaldataforecasts";
	private static final String TABLE_AIXSUNMOONDATA = "aixsunmoondata";

	
	private DatabaseHelper mOpenHelper;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "aix_database.db";
		private static final int DATABASE_VERSION = 9;
		// 0.1.6 = version 8
		// 0.1.5 = version 7
		// 0.1.4 = version 6
		// 0.1.3 = version 5
		
		private Context mContext;
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			createWidgetViewLocationTables(db);
			createForecastTable(db);
			
			ContentValues values = new ContentValues();
			values.put(AfLocationsColumns.TITLE, "Berlin");
			values.put(AfLocationsColumns.TITLE_DETAILED, "Berlin, Germany");
			values.put(AfLocationsColumns.LATITUDE, 52.5244f);
			values.put(AfLocationsColumns.LONGITUDE, 13.4105f);
			db.insert(TABLE_AIXLOCATIONS, null, values);
			values.clear();
			values.put(AfLocationsColumns.TITLE, "Oslo");
			values.put(AfLocationsColumns.TITLE_DETAILED, "Oslo, Norway");
			values.put(AfLocationsColumns.LATITUDE, 59.949444f);
			values.put(AfLocationsColumns.LONGITUDE, 10.756389f);
			db.insert(TABLE_AIXLOCATIONS, null, values);
		}

		private void createWidgetViewLocationTables(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_AIXWIDGETS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AfWidgetsColumns.SIZE + " INTEGER,"
					+ AfWidgetsColumns.VIEWS + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXWIDGETSETTINGS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AfSettingsColumns.ROW_ID + " INTEGER,"
					+ AfSettingsColumns.KEY + " TEXT,"
					+ AfSettingsColumns.VALUE + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXVIEWS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AfViewsColumns.LOCATION + " INTEGER,"
					+ AfViewsColumns.TYPE + " INTEGER);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXVIEWSETTINGS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AfSettingsColumns.ROW_ID + " INTEGER,"
					+ AfSettingsColumns.KEY + " TEXT,"
					+ AfSettingsColumns.VALUE + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXLOCATIONS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AfLocationsColumns.TITLE + " TEXT,"
					+ AfLocationsColumns.TITLE_DETAILED + " TEXT,"
					+ AfLocationsColumns.TIME_ZONE + " TEXT,"
					+ AfLocationsColumns.TYPE + " INTEGER,"
					+ AfLocationsColumns.TIME_OF_LAST_FIX + " INTEGER,"
					+ AfLocationsColumns.LATITUDE + " REAL,"
					+ AfLocationsColumns.LONGITUDE + " REAL,"
					+ AfLocationsColumns.LAST_FORECAST_UPDATE + " INTEGER,"
					+ AfLocationsColumns.FORECAST_VALID_TO + " INTEGER,"
					+ AfLocationsColumns.NEXT_FORECAST_UPDATE + " INTEGER);");
		}

		private void createForecastTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_AIXPOINTDATAFORECASTS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AfPointDataForecastColumns.LOCATION + " INTEGER,"
					+ AfPointDataForecastColumns.TIME_ADDED + " INTEGER,"
					+ AfPointDataForecastColumns.TIME + " INTEGER,"
					+ AfPointDataForecastColumns.TEMPERATURE + " REAL,"
					+ AfPointDataForecastColumns.HUMIDITY + " REAL,"
					+ AfPointDataForecastColumns.PRESSURE + " REAL);");

			db.execSQL("CREATE TABLE " + TABLE_AIXINTERVALDATAFORECASTS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AfIntervalDataForecastColumns.LOCATION + " INTEGER,"
					+ AfIntervalDataForecastColumns.TIME_ADDED + " INTEGER,"
					+ AfIntervalDataForecastColumns.TIME_FROM + " INTEGER,"
					+ AfIntervalDataForecastColumns.TIME_TO + " INTEGER,"
					+ AfIntervalDataForecastColumns.RAIN_VALUE + " REAL,"
					+ AfIntervalDataForecastColumns.RAIN_MINVAL + " REAL,"
					+ AfIntervalDataForecastColumns.RAIN_MAXVAL + " REAL,"
					+ AfIntervalDataForecastColumns.WEATHER_ICON + " INTEGER);");

			db.execSQL("CREATE TABLE " + TABLE_AIXSUNMOONDATA + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AfSunMoonDataColumns.LOCATION + " INTEGER,"
					+ AfSunMoonDataColumns.TIME_ADDED + " INTEGER,"
					+ AfSunMoonDataColumns.DATE + " INTEGER,"
					+ AfSunMoonDataColumns.SUN_RISE + " INTEGER,"
					+ AfSunMoonDataColumns.SUN_SET + " INTEGER,"
					+ AfSunMoonDataColumns.MOON_RISE + " INTEGER,"
					+ AfSunMoonDataColumns.MOON_SET + " INTEGER,"
					+ AfSunMoonDataColumns.MOON_PHASE + " INTEGER);");

			// Add indexes for faster queries by location
			db.execSQL("CREATE INDEX idx_point_location ON " + TABLE_AIXPOINTDATAFORECASTS + "(" + AfPointDataForecastColumns.LOCATION + ");");
			db.execSQL("CREATE INDEX idx_interval_location ON " + TABLE_AIXINTERVALDATAFORECASTS + "(" + AfIntervalDataForecastColumns.LOCATION + ");");
			db.execSQL("CREATE INDEX idx_sunmoon_location ON " + TABLE_AIXSUNMOONDATA + "(" + AfSunMoonDataColumns.LOCATION + ");");
		}
		
		private void migrateProperty(ContentValues values, Cursor cursor, String id, int column) {
			String color = cursor.getString(column);
			if (color != null) {
				values.put(id, color);
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "onUpdate() oldVersion=" + oldVersion + ",newVersion=" + newVersion);

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXLOCATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETSETTINGS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWSETTINGS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXPOINTDATAFORECASTS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXINTERVALDATAFORECASTS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXSUNMOONDATA);

            onCreate(db);

			// All forecast data has been deleted.
            ContentValues values = new ContentValues();
			values.put(AfLocationsColumns.LAST_FORECAST_UPDATE, 0);
			values.put(AfLocationsColumns.FORECAST_VALID_TO, 0);
			values.put(AfLocationsColumns.NEXT_FORECAST_UPDATE, 0);
			db.update(TABLE_AIXLOCATIONS, values, null, null);
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "delete() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int count = 0;

		switch (sUriMatcher.match(uri)) {
			case AIXWIDGETS -> {
				count = db.delete(TABLE_AIXWIDGETS, null, null);
			}
			case AIXWIDGETS_ID -> {
				count = db.delete(
						TABLE_AIXWIDGETS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXWIDGETS_ID_SETTINGS -> {
				count = db.delete(
						TABLE_AIXWIDGETSETTINGS,
						AfSettingsColumns.ROW_ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXWIDGETSETTINGS -> {
				count = db.delete(TABLE_AIXWIDGETSETTINGS, null, null);
			}
			case AIXWIDGETSETTINGS_ID -> {
				count = db.delete(
						TABLE_AIXWIDGETSETTINGS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWS -> {
				count = db.delete(TABLE_AIXVIEWS, null, null);
			}
			case AIXVIEWS_ID -> {
				count = db.delete(
						TABLE_AIXVIEWS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWS_ID_SETTINGS -> {
				count = db.delete(
						TABLE_AIXVIEWSETTINGS,
						AfSettingsColumns.ROW_ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWSETTINGS -> {
				count = db.delete(TABLE_AIXVIEWSETTINGS, null, null);
			}
			case AIXVIEWSETTINGS_ID -> {
				count = db.delete(
						TABLE_AIXVIEWSETTINGS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXLOCATIONS -> {
				count = db.delete(TABLE_AIXLOCATIONS, null, null);
			}
			case AIXLOCATIONS_ID -> {
				count = db.delete(
						TABLE_AIXLOCATIONS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXLOCATIONS_POINTDATAFORECASTS -> {
				count = db.delete(
						TABLE_AIXPOINTDATAFORECASTS,
						AfPointDataForecastColumns.LOCATION + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXLOCATIONS_INTERVALDATAFORECASTS -> {
				count = db.delete(
						TABLE_AIXINTERVALDATAFORECASTS,
						AfIntervalDataForecastColumns.LOCATION + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXLOCATIONS_SUNMOONDATA -> {
				count = db.delete(
						TABLE_AIXSUNMOONDATA,
						AfSunMoonDataColumns.LOCATION + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXPOINTDATAFORECASTS -> {
				final String before = uri.getQueryParameter("before");
				final String after = uri.getQueryParameter("after");

				if (before != null) {
					count += db.delete(
							TABLE_AIXPOINTDATAFORECASTS,
							AfPointDataForecasts.TIME + "<?",
							new String[]{before});
				} else if (after != null) {
					count += db.delete(
							TABLE_AIXPOINTDATAFORECASTS,
							AfPointDataForecasts.TIME + ">=?",
							new String[]{after});
				} else {
					count = db.delete(TABLE_AIXPOINTDATAFORECASTS, null, null);
				}

			}
			case AIXPOINTDATAFORECASTS_ID -> {
				count = db.delete(
						TABLE_AIXPOINTDATAFORECASTS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXINTERVALDATAFORECASTS -> {
				final String before = uri.getQueryParameter("before");
				final String after = uri.getQueryParameter("after");

				if (before != null) {
					count += db.delete(
							TABLE_AIXINTERVALDATAFORECASTS,
							AfIntervalDataForecasts.TIME_TO + "<?",
							new String[]{before});
				} else if (after != null) {
					count += db.delete(
							TABLE_AIXINTERVALDATAFORECASTS,
							AfIntervalDataForecasts.TIME_FROM + ">=?",
							new String[]{after});
				} else {
					count = db.delete(TABLE_AIXINTERVALDATAFORECASTS, null, null);
				}

			}
			case AIXINTERVALDATAFORECASTS_ID -> {
				count = db.delete(
						TABLE_AIXINTERVALDATAFORECASTS,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXSUNMOONDATA -> {
				final String before = uri.getQueryParameter("before");
				final String after = uri.getQueryParameter("after");

				if (before != null) {
					count += db.delete(
							TABLE_AIXSUNMOONDATA,
							AfSunMoonData.DATE + "<?",
							new String[]{before});
				} else if (after != null) {
					count += db.delete(
							TABLE_AIXSUNMOONDATA,
							AfSunMoonData.DATE + ">=?",
							new String[]{after});
				} else {
					count = db.delete(TABLE_AIXSUNMOONDATA, null, null);
				}

			}
			case AIXSUNMOONDATA_ID -> {
				count = db.delete(
						TABLE_AIXSUNMOONDATA,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
		}
		
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case AIXWIDGETS -> {
				return AfWidgets.CONTENT_TYPE;
			}
			case AIXWIDGETS_ID -> {
				return AfWidgets.CONTENT_ITEM_TYPE;
			}
			case AIXWIDGETS_ID_SETTINGS, AIXWIDGETSETTINGS, AIXVIEWS_ID_SETTINGS, AIXVIEWSETTINGS -> {
				return AfWidgetSettingsDatabase.CONTENT_TYPE;
			}
            case AIXWIDGETSETTINGS_ID, AIXVIEWSETTINGS_ID -> {
				return AfWidgetSettingsDatabase.CONTENT_ITEM_TYPE;
			}
			case AIXVIEWS -> {
				return AfViews.CONTENT_TYPE;
			}
			case AIXVIEWS_ID -> {
				return AfViews.CONTENT_ITEM_TYPE;
			}
            case AIXVIEWS_LOCATION, AIXLOCATIONS_ID -> {
				return AfLocations.CONTENT_ITEM_TYPE;
			}
            case AIXLOCATIONS -> {
				return AfLocations.CONTENT_TYPE;
			}
            case AIXLOCATIONS_POINTDATAFORECASTS, AIXPOINTDATAFORECASTS_ID -> {
				return AfPointDataForecasts.CONTENT_ITEM_TYPE;
			}
			case AIXLOCATIONS_INTERVALDATAFORECASTS, AIXINTERVALDATAFORECASTS_ID -> {
				return AfIntervalDataForecasts.CONTENT_ITEM_TYPE;
			}
			case AIXLOCATIONS_SUNMOONDATA, AIXSUNMOONDATA_ID -> {
				return AfSunMoonData.CONTENT_ITEM_TYPE;
			}
			case AIXPOINTDATAFORECASTS -> {
				return AfPointDataForecasts.CONTENT_TYPE;
			}
            case AIXINTERVALDATAFORECASTS -> {
				return AfIntervalDataForecasts.CONTENT_TYPE;
			}
            case AIXSUNMOONDATA -> {
				return AfSunMoonData.CONTENT_TYPE;
			}
            case AIXRENDER -> {
				return "image/webp";
			}
		}
		throw new IllegalStateException();
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGD) Log.d(TAG, "insert() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		Uri resultUri = null;

		switch (sUriMatcher.match(uri)) {
			case AIXWIDGETS -> {
				long rowId = db.replace(TABLE_AIXWIDGETS, null, values);
				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfWidgets.CONTENT_URI, rowId);
					getContext().getContentResolver().notifyChange(resultUri, null);
				}
			}
			case AIXWIDGETS_ID_SETTINGS -> {
				String widgetId = uri.getPathSegments().get(1);
				boolean success = true;
				for (Entry<String, Object> entry : values.valueSet()) {
					if (addSetting(db, TABLE_AIXWIDGETSETTINGS, widgetId, entry) == -1) {
						success = false;
					}
				}
				if (success) {
					resultUri = AfWidgetSettingsDatabase.CONTENT_URI;
				}
			}
			case AIXWIDGETSETTINGS -> {
				long rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfWidgetSettingsDatabase.CONTENT_URI, rowId);
					getContext().getContentResolver().notifyChange(resultUri, null);
				}
			}
			case AIXVIEWS -> {
				long rowId = db.insert(TABLE_AIXVIEWS, null, values);
				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfViews.CONTENT_URI, rowId);
					getContext().getContentResolver().notifyChange(resultUri, null);
				}
			}
			case AIXVIEWS_ID_SETTINGS -> {
				String viewId = uri.getPathSegments().get(1);
				boolean success = true;
				for (Entry<String, Object> entry : values.valueSet()) {
					if (addSetting(db, TABLE_AIXVIEWSETTINGS, viewId, entry) == -1) {
						success = false;
					}
				}
				if (success) {
					resultUri = AfViewSettings.CONTENT_URI;
				}
			}
			case AIXVIEWSETTINGS -> {
				long rowId = db.insert(TABLE_AIXVIEWSETTINGS, null, values);
				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfViewSettings.CONTENT_URI, rowId);
					getContext().getContentResolver().notifyChange(resultUri, null);
				}
			}
			case AIXLOCATIONS -> {
				long rowId = db.insert(TABLE_AIXLOCATIONS, null, values);
				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfLocations.CONTENT_URI, rowId);
					getContext().getContentResolver().notifyChange(resultUri, null);
				}
			}
			case AIXPOINTDATAFORECASTS -> {
				long rowId = db.insert(TABLE_AIXPOINTDATAFORECASTS, null, values);

				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfPointDataForecasts.CONTENT_URI, rowId);
				}

			}
			case AIXINTERVALDATAFORECASTS -> {
				long rowId = db.insert(TABLE_AIXINTERVALDATAFORECASTS, null, values);

				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfIntervalDataForecasts.CONTENT_URI, rowId);
				}

			}
			case AIXSUNMOONDATA -> {
				long rowId = -1;

				SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
				qb.setStrict(true);
				qb.setTables(TABLE_AIXSUNMOONDATA);

				Cursor cursor = qb.query(db,
						new String[]{BaseColumns._ID},
						AfSunMoonDataColumns.LOCATION + "=? AND " + AfSunMoonDataColumns.DATE + "=?",
						new String[]{
								values.getAsString(AfSunMoonDataColumns.LOCATION),
								values.getAsString(AfSunMoonDataColumns.DATE)
						}, null, null, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
					}
					cursor.close();
				}

				if (rowId != -1) {
					// A record already exists for this time and location. Update its values:
					db.update(TABLE_AIXSUNMOONDATA, values, BaseColumns._ID + "=?", new String[]{Long.toString(rowId)});
				} else {
					// No record exists for this time. Insert new row:
					rowId = db.insert(TABLE_AIXSUNMOONDATA, null, values);
				}

				if (rowId != -1) {
					resultUri = ContentUris.withAppendedId(AfSunMoonData.CONTENT_URI, rowId);
				}

			}
			default -> throw new UnsupportedOperationException();
		}
		
		return resultUri;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
			throws FileNotFoundException
	{
		if (sUriMatcher.match(uri) != AIXRENDER)
		{
			throw new FileNotFoundException("Uri does not follow AfRender format. (uri=" + uri + ")");
		}
		
		List<String> pathSegments = uri.getPathSegments();
		if (pathSegments == null || pathSegments.size() != 4)
		{
			throw new FileNotFoundException();
		}
		
		String appWidgetIdString = pathSegments.get(1);
		String updateTimeString = pathSegments.get(2);
		
		int appWidgetId;
		long updateTime;
		
		try
		{
			appWidgetId = Integer.parseInt(appWidgetIdString);
			updateTime = Long.parseLong(updateTimeString);
			
			if (appWidgetId == -1 || updateTime == -1)
			{
				throw new NumberFormatException();
			}
		}
		catch (NumberFormatException e)
		{
			String errorMessage = String.format(
					"Invalid arguments (appWidgetIdString=%s,updateTimeString=%s)",
					appWidgetIdString, updateTimeString);
			Log.d(TAG, "openFile(): " + errorMessage);
			throw new FileNotFoundException(errorMessage);
		}
		
		String orientation = pathSegments.get(3);
		if (orientation == null || !(orientation.equals("portrait") || orientation.equals("landscape")))
		{
			String errorMessage = "Invalid orientation parameter. (orientation=" + orientation + ")";
			Log.d(TAG, "openFile(): " + errorMessage);
			throw new FileNotFoundException();
		}
		
		Context context = getContext();

		if (context != null) {
			AfUtils.deleteTemporaryFile(context, appWidgetId, updateTime, orientation);
			String fileName = context.getString(R.string.bufferImageFileName, appWidgetId, updateTime, orientation);

			File file = new File(context.getFilesDir(), fileName);
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		} else {
			throw new FileNotFoundException("could not open file: " + uri);
		}
	}
	
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (LOGD) Log.d(TAG, "query() with uri=" + uri);

		String   qbTables        = null;
		String[] qbProjection    = null;
		String   qbSelection     = null;
		String[] qbSelectionArgs = null;
		String   qbSortOrder     = null;

		switch (sUriMatcher.match(uri)) {
			case AIXWIDGETS -> {
				qbTables = TABLE_AIXWIDGETS;
				qbProjection = AfWidgets.ALL_COLUMNS;
			}
			case AIXWIDGETS_ID -> {
				qbTables = TABLE_AIXWIDGETS;
				qbProjection = AfWidgets.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXWIDGETS_ID_SETTINGS -> {
				qbTables = TABLE_AIXWIDGETSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
				qbSelection = AfSettings.ROW_ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXWIDGETSETTINGS -> {
				qbTables = TABLE_AIXWIDGETSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
			}
			case AIXWIDGETSETTINGS_ID -> {
				qbTables = TABLE_AIXWIDGETSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXVIEWS -> {
				qbTables = TABLE_AIXVIEWS;
				qbProjection = AfViews.ALL_COLUMNS;
			}
			case AIXVIEWS_ID -> {
				qbTables = TABLE_AIXVIEWS;
				qbProjection = AfViews.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXVIEWS_ID_SETTINGS -> {
				qbTables = TABLE_AIXVIEWSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
				qbSelection = AfSettings.ROW_ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXVIEWSETTINGS -> {
				qbTables = TABLE_AIXVIEWSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
			}
			case AIXVIEWSETTINGS_ID -> {
				qbTables = TABLE_AIXVIEWSETTINGS;
				qbProjection = AfSettings.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXLOCATIONS -> {
				qbTables = TABLE_AIXLOCATIONS;
				qbProjection = AfLocations.ALL_COLUMNS;
			}
			case AIXLOCATIONS_ID -> {
				qbTables = TABLE_AIXLOCATIONS;
				qbProjection = AfLocations.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXLOCATIONS_POINTDATAFORECASTS -> {
				qbTables = TABLE_AIXPOINTDATAFORECASTS;
				qbProjection = AfPointDataForecasts.ALL_COLUMNS;

				final String locationId = uri.getPathSegments().get(1);
				final String start = uri.getQueryParameter("start");
				final String end = uri.getQueryParameter("end");

				String timeAddedSubQuery = " AND " + AfPointDataForecasts.TIME_ADDED + " = (SELECT MAX(" + AfPointDataForecasts.TIME_ADDED + ") FROM " + TABLE_AIXPOINTDATAFORECASTS + " WHERE " + AfPointDataForecasts.LOCATION + " = ?)";

				if (start != null && end != null) {
					qbSelection = AfPointDataForecasts.LOCATION + "=? AND "
							+ AfPointDataForecasts.TIME + ">=? AND "
							+ AfPointDataForecasts.TIME + "<=?" + timeAddedSubQuery;
					qbSelectionArgs = new String[]{locationId, start, end, locationId};
					qbSortOrder = AfPointDataForecasts.TIME + " ASC";
				} else {
					qbSelection = AfPointDataForecasts.LOCATION + "=?" + timeAddedSubQuery;
					qbSelectionArgs = new String[]{locationId, locationId};
				}

			}
			case AIXLOCATIONS_INTERVALDATAFORECASTS -> {
				qbTables = TABLE_AIXINTERVALDATAFORECASTS;
				qbProjection = AfIntervalDataForecasts.ALL_COLUMNS;

				final String locationId = uri.getPathSegments().get(1);
				final String start = uri.getQueryParameter("start");
				final String end = uri.getQueryParameter("end");

				String timeAddedSubQuery = " AND " + AfIntervalDataForecasts.TIME_ADDED + " = (SELECT MAX(" + AfIntervalDataForecasts.TIME_ADDED + ") FROM " + TABLE_AIXINTERVALDATAFORECASTS + " WHERE " + AfIntervalDataForecasts.LOCATION + " = ?)";

				if (start != null && end != null) {
					qbSelection = AfIntervalDataForecasts.LOCATION + "=? AND "
							+ AfIntervalDataForecasts.TIME_TO + ">? AND "
							+ AfIntervalDataForecasts.TIME_FROM + "<?" + timeAddedSubQuery;
					qbSelectionArgs = new String[]{locationId, start, end, locationId};
					qbSortOrder = '(' + AfIntervalDataForecasts.TIME_TO + '-' +
							AfIntervalDataForecasts.TIME_FROM + ") ASC," +
							AfIntervalDataForecasts.TIME_FROM + " ASC";
				} else {
					qbSelection = AfIntervalDataForecasts.LOCATION + "=?" + timeAddedSubQuery;
					qbSelectionArgs = new String[]{locationId, locationId};
				}

			}
			case AIXLOCATIONS_SUNMOONDATA -> {
				qbTables = TABLE_AIXSUNMOONDATA;
				qbProjection = AfSunMoonData.ALL_COLUMNS;

				final String locationId = uri.getPathSegments().get(1);
				final String start = uri.getQueryParameter("start");
				final String end = uri.getQueryParameter("end");

				if (start != null && end != null) {
					qbSelection = AfSunMoonData.LOCATION + "=? AND "
							+ AfSunMoonData.DATE + ">=? AND "
							+ AfSunMoonData.DATE + "<=?";
					qbSelectionArgs = new String[]{locationId, start, end};
					qbSortOrder = AfSunMoonData.DATE + " ASC";
				} else {
					qbSelection = AfSunMoonData.LOCATION + "=?";
					qbSelectionArgs = new String[]{locationId};
				}

			}
			case AIXPOINTDATAFORECASTS -> {
				qbTables = TABLE_AIXPOINTDATAFORECASTS;
				qbProjection = AfPointDataForecasts.ALL_COLUMNS;
			}
			case AIXPOINTDATAFORECASTS_ID -> {
				qbTables = TABLE_AIXPOINTDATAFORECASTS;
				qbProjection = AfPointDataForecasts.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXINTERVALDATAFORECASTS -> {
				qbTables = TABLE_AIXINTERVALDATAFORECASTS;
				qbProjection = AfIntervalDataForecasts.ALL_COLUMNS;
			}
			case AIXINTERVALDATAFORECASTS_ID -> {
				qbTables = TABLE_AIXINTERVALDATAFORECASTS;
				qbProjection = AfIntervalDataForecasts.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
			case AIXSUNMOONDATA -> {
				qbTables = TABLE_AIXSUNMOONDATA;
				qbProjection = AfSunMoonData.ALL_COLUMNS;
			}
			case AIXSUNMOONDATA_ID -> {
				qbTables = TABLE_AIXSUNMOONDATA;
				qbProjection = AfSunMoonData.ALL_COLUMNS;
				qbSelection = BaseColumns._ID + "=?";
				qbSelectionArgs = new String[]{uri.getPathSegments().get(1)};
			}
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setStrict(true);
		qb.setTables(qbTables);

		return qb.query(db, qbProjection, qbSelection, qbSelectionArgs, null, null, qbSortOrder);
	}


	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "update() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		switch (sUriMatcher.match(uri)) {
			case AIXWIDGETS_ID -> {
				return db.update(
						TABLE_AIXWIDGETS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXWIDGETS_ID_SETTINGS -> {
				return db.update(
						TABLE_AIXWIDGETSETTINGS,
						values,
						AfSettingsColumns.ROW_ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXWIDGETSETTINGS_ID -> {
				return db.update(
						TABLE_AIXWIDGETSETTINGS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWS_ID -> {
				return db.update(
						TABLE_AIXVIEWS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWS_ID_SETTINGS -> {
				return db.update(
						TABLE_AIXVIEWSETTINGS,
						values,
						AfSettingsColumns.ROW_ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXVIEWS_LOCATION -> {
				long locationId = findLocationFromView(db, uri);

				if (locationId != -1) {
					return db.update(
							TABLE_AIXLOCATIONS,
							values,
							BaseColumns._ID + "=?",
							new String[]{Long.toString(locationId)});
				} else {
					if (LOGD)
						Log.d(TAG, "update() with uri=" + uri + " failed. No location in view!");
					return 0; // Could not properly service request, as no location was found.
				}
			}
			case AIXVIEWSETTINGS_ID -> {
				return db.update(
						TABLE_AIXVIEWSETTINGS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXLOCATIONS -> {
				return db.update(TABLE_AIXLOCATIONS, values, null, null);
			}
			case AIXLOCATIONS_ID -> {
				return db.update(
						TABLE_AIXLOCATIONS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXPOINTDATAFORECASTS -> {
				return db.delete(TABLE_AIXPOINTDATAFORECASTS,
						BaseColumns._ID + " NOT IN (" +
								"SELECT MAX(" + BaseColumns._ID + ") FROM " + TABLE_AIXPOINTDATAFORECASTS +
								" GROUP BY " + AfPointDataForecastColumns.LOCATION + ", " + AfPointDataForecastColumns.TIME +
								")",
						null);
			}
			case AIXPOINTDATAFORECASTS_ID -> {
				return db.update(
						TABLE_AIXPOINTDATAFORECASTS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXINTERVALDATAFORECASTS -> {
				return db.delete(TABLE_AIXINTERVALDATAFORECASTS,
						BaseColumns._ID + " NOT IN (" +
								"SELECT MAX(" + BaseColumns._ID + ") FROM " + TABLE_AIXINTERVALDATAFORECASTS +
								" GROUP BY " + AfIntervalDataForecastColumns.LOCATION + ", " + AfIntervalDataForecastColumns.TIME_FROM + ", " + AfIntervalDataForecastColumns.TIME_TO +
								")",
						null);
			}
			case AIXINTERVALDATAFORECASTS_ID -> {
				return db.update(
						TABLE_AIXINTERVALDATAFORECASTS,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			case AIXSUNMOONDATA_ID -> {
				return db.update(
						TABLE_AIXSUNMOONDATA,
						values,
						BaseColumns._ID + "=?",
						new String[]{uri.getPathSegments().get(1)});
			}
			default -> {
				if (LOGD) Log.d(TAG, "update() with uri=" + uri + " not matched");
				return 0;
			}
		}
	}

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
		if (LOGD) Log.d(TAG, "bulkInsert() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		final int match = sUriMatcher.match(uri);
		int numInserted = 0;

		switch (match) {
			case AIXPOINTDATAFORECASTS -> {
				try {
					db.beginTransaction();
					numInserted = bulkInsertPointData(db, values);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			case AIXINTERVALDATAFORECASTS -> {
				try {
					db.beginTransaction();
					numInserted = bulkInsertIntervalData(db, values);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			case AIXWIDGETS_ID_SETTINGS -> {
				String widgetId = uri.getPathSegments().get(1);
				try {
					db.beginTransaction();
					bulkInsertSettings(db, values, widgetId);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			default ->
					throw new UnsupportedOperationException("AfProvider.bulkInsert() Unsupported URI: " + uri);
		}

		return numInserted;
	}
	
	private void bulkInsertSettings(SQLiteDatabase db, ContentValues[] values, String appWidgetId)
	{
		SQLiteStatement updateStatement =
				db.compileStatement("UPDATE " + TABLE_AIXWIDGETSETTINGS
						+ " SET " + AfSettingsColumns.VALUE + "=?"
						+ " WHERE " + AfSettingsColumns.ROW_ID + "=?"
							+ " AND " + AfSettingsColumns.KEY + "=?");
		
		SQLiteStatement insertStatement =
				db.compileStatement("INSERT INTO " + TABLE_AIXWIDGETSETTINGS + "("
						+ AfSettingsColumns.ROW_ID + ","
						+ AfSettingsColumns.KEY + ","
						+ AfSettingsColumns.VALUE + ") "
						+ "VALUES (?,?,?)");

		for (ContentValues value: values)
		{
			String settingKey = value.getAsString(AfWidgetSettingsDatabase.KEY);
			String settingValue = value.getAsString(AfWidgetSettingsDatabase.VALUE);
			
			if (!TextUtils.isEmpty(settingKey))
			{
				long existingRowId = checkForSetting(db, appWidgetId, settingKey);
				
				if (existingRowId != -1)
				{
					// Update existing row
					if (settingValue == null) {
						updateStatement.bindNull(1);
					} else {
						updateStatement.bindString(1, settingValue);
					}
					updateStatement.bindString(2, appWidgetId);
					updateStatement.bindString(3, settingKey);
					updateStatement.execute();
				}
				else
				{
					// Insert new row
					insertStatement.bindString(1, appWidgetId);
					insertStatement.bindString(2, settingKey);
					
					if (settingValue == null) {
						insertStatement.bindNull(3);
					} else {
						insertStatement.bindString(3, settingValue);
					}
					
					insertStatement.execute();
				}
			}
		}
	}
	
	private long checkForSetting(SQLiteDatabase db, String appWidgetId, String key)
	{
		long rowId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(TABLE_AIXWIDGETSETTINGS);
		
		Cursor cursor = qb.query(db,
				new String[] { BaseColumns._ID },
				AfSettingsColumns.ROW_ID + "=? AND " + AfSettingsColumns.KEY + "=?",
				new String[] { appWidgetId, key },
				null, null, null);
		
		if (cursor != null)
		{
			try (cursor) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
				}
			}
		}
		
		return rowId;
	}
	
	private int bulkInsertIntervalData(SQLiteDatabase db, ContentValues[] values)
	{
		int numInserted = 0;
		
		SQLiteStatement insert =
				db.compileStatement("INSERT INTO " + TABLE_AIXINTERVALDATAFORECASTS + "("
						+ AfIntervalDataForecasts.LOCATION + ","
						+ AfIntervalDataForecasts.TIME_ADDED + ","
						+ AfIntervalDataForecasts.TIME_FROM + ","
						+ AfIntervalDataForecasts.TIME_TO + ","
						+ AfIntervalDataForecasts.RAIN_VALUE + ","
						+ AfIntervalDataForecasts.RAIN_MINVAL + ","
						+ AfIntervalDataForecasts.RAIN_MAXVAL + ","
						+ AfIntervalDataForecasts.WEATHER_ICON + ") "
						+ "VALUES (?,?,?,?,?,?,?,?)");
		
		for (ContentValues value : values)
		{
			Long location = value.getAsLong(AfIntervalDataForecasts.LOCATION);
			Long timeAdded = value.getAsLong(AfIntervalDataForecasts.TIME_ADDED);
			Long timeFrom = value.getAsLong(AfIntervalDataForecasts.TIME_FROM);
			Long timeTo = value.getAsLong(AfIntervalDataForecasts.TIME_TO);
			
			if (location != null && timeAdded != null && timeFrom != null && timeTo != null)
			{
				insert.bindLong(1, location);
				insert.bindLong(2, timeAdded);
				insert.bindLong(3, timeFrom);
				insert.bindLong(4, timeTo);
				
				Double rainValue = value.getAsDouble(AfIntervalDataForecasts.RAIN_VALUE);
				if (rainValue != null) {
					insert.bindDouble(5, rainValue);
				} else {
					insert.bindNull(5);
				}
				
				Double rainMinValue = value.getAsDouble(AfIntervalDataForecasts.RAIN_MINVAL);
				if (rainMinValue != null) {
					insert.bindDouble(6, rainMinValue);
				} else {
					insert.bindNull(6);
				}
				
				Double rainMaxValue = value.getAsDouble(AfIntervalDataForecasts.RAIN_MAXVAL);
				if (rainMaxValue != null) {
					insert.bindDouble(7, rainMaxValue);
				} else {
					insert.bindNull(7);
				}
				
				Long weatherIcon = value.getAsLong(AfIntervalDataForecasts.WEATHER_ICON);
				if (weatherIcon != null) {
					insert.bindLong(8, weatherIcon);
				} else {
					insert.bindNull(8);
				}
				
				insert.execute();
				numInserted++;
			}
		}
		
		return numInserted;
	}
	
	private int bulkInsertPointData(SQLiteDatabase db, ContentValues[] values)
	{
		int numInserted = 0;
		
		SQLiteStatement insert =
				db.compileStatement("INSERT INTO " + TABLE_AIXPOINTDATAFORECASTS + "("
						+ AfPointDataForecasts.LOCATION + ","
						+ AfPointDataForecasts.TIME_ADDED + ","
						+ AfPointDataForecasts.TIME + ","
						+ AfPointDataForecasts.TEMPERATURE + ","
						+ AfPointDataForecasts.HUMIDITY + ","
						+ AfPointDataForecasts.PRESSURE + ") "
						+ "VALUES (?,?,?,?,?,?)");
		
		for (ContentValues value : values)
		{
			Long location = value.getAsLong(AfPointDataForecasts.LOCATION);
			Long timeAdded = value.getAsLong(AfPointDataForecasts.TIME_ADDED);
			Long time = value.getAsLong(AfPointDataForecasts.TIME);
			
			if (location != null && timeAdded != null && time != null)
			{
				insert.bindLong(1, location);
				insert.bindLong(2, timeAdded);
				insert.bindLong(3, time);
				
				Double temperature = value.getAsDouble(AfPointDataForecasts.TEMPERATURE);
				if (temperature != null) {
					insert.bindDouble(4, temperature);
				} else {
					insert.bindNull(4);
				}
				
				Double humidity = value.getAsDouble(AfPointDataForecasts.HUMIDITY);
				if (humidity != null) {
					insert.bindDouble(5, humidity);
				} else {
					insert.bindNull(5);
				}
				
				Double pressure = value.getAsDouble(AfPointDataForecasts.PRESSURE);
				if (pressure != null) {
					insert.bindDouble(6, pressure);
				} else {
					insert.bindNull(6);
				}
				
				insert.execute();
				numInserted++;
			}
		}
		
		return numInserted;
	}

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	private static final int AIXWIDGETS = 101;
	private static final int AIXWIDGETS_ID = 102;
	private static final int AIXWIDGETS_ID_SETTINGS = 103;
	
	private static final int AIXWIDGETSETTINGS = 201;
	private static final int AIXWIDGETSETTINGS_ID = 202;
	
	private static final int AIXVIEWS = 301;
	private static final int AIXVIEWS_ID = 302;
	private static final int AIXVIEWS_ID_SETTINGS = 303;
	private static final int AIXVIEWS_LOCATION = 304;
	//private static final int AIXVIEWS_POINTDATAFORECASTS = 305;
	//private static final int AIXVIEWS_INTERVALDATAFORECASTS = 306;
	//private static final int AIXVIEWS_SUNMOONDATA = 307;
	
	private static final int AIXVIEWSETTINGS = 401;
	private static final int AIXVIEWSETTINGS_ID = 402;
	
	private static final int AIXLOCATIONS = 501;
	private static final int AIXLOCATIONS_ID = 502;
	private static final int AIXLOCATIONS_POINTDATAFORECASTS = 503;
	private static final int AIXLOCATIONS_INTERVALDATAFORECASTS = 504;
	private static final int AIXLOCATIONS_SUNMOONDATA = 505;
	
	private static final int AIXPOINTDATAFORECASTS = 601;
	private static final int AIXPOINTDATAFORECASTS_ID = 602;
	
	private static final int AIXINTERVALDATAFORECASTS = 701;
	private static final int AIXINTERVALDATAFORECASTS_ID = 702;
	
	private static final int AIXSUNMOONDATA = 801;
	private static final int AIXSUNMOONDATA_ID = 802;
	
	private static final int AIXRENDER = 999;
	
	static {
		sUriMatcher.addURI(AUTHORITY, "aixwidgets", AIXWIDGETS);
		sUriMatcher.addURI(AUTHORITY, "aixwidgets/#", AIXWIDGETS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixwidgets/#/settings", AIXWIDGETS_ID_SETTINGS);
		
		sUriMatcher.addURI(AUTHORITY, "aixwidgetsettings", AIXWIDGETSETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixwidgetsettings/#", AIXWIDGETSETTINGS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixviews", AIXVIEWS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#", AIXVIEWS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/settings", AIXVIEWS_ID_SETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/location", AIXVIEWS_LOCATION);
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/pointdata_forecasts", AIXVIEWS_POINTDATAFORECASTS);
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/intervaldata_forecasts", AIXVIEWS_INTERVALDATAFORECASTS);
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/sunmoondata", AIXVIEWS_SUNMOONDATA);
		
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings", AIXVIEWSETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings/#", AIXVIEWSETTINGS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixlocations", AIXLOCATIONS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#", AIXLOCATIONS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/pointdata_forecasts", AIXLOCATIONS_POINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/intervaldata_forecasts", AIXLOCATIONS_INTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/sunmoondata", AIXLOCATIONS_SUNMOONDATA);
		
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts", AIXPOINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts/#", AIXPOINTDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts", AIXINTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts/#", AIXINTERVALDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata", AIXSUNMOONDATA);
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata/#", AIXSUNMOONDATA_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixrender/#/#/*", AIXRENDER);
	}
	
	private long findLocationFromView(SQLiteDatabase db, Uri viewUri) {
		long locationId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(TABLE_AIXVIEWS);
		
		Cursor cursor = qb.query(db,
				new String[] { AfViewsColumns.LOCATION },
				BaseColumns._ID + "=?",
				new String[] { viewUri.getPathSegments().get(1) },
				null, null, null);
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				locationId = cursor.getLong(cursor.getColumnIndexOrThrow(AfViewsColumns.LOCATION));
			}
			cursor.close();
		}
		
		return locationId;
	}

	private long addSetting(SQLiteDatabase db, String table, String id, Entry<String, Object> entry) {
		long rowId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(table);

		Cursor cursor = qb.query(db,
				new String[] { BaseColumns._ID },
				AfSettingsColumns.ROW_ID + "=? AND " + AfSettingsColumns.KEY + "=?",
				new String[] { id, entry.getKey() },
				null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
			}
			cursor.close();
		}

		ContentValues values = new ContentValues();
		values.put(AfSettingsColumns.ROW_ID, id);
		values.put(AfSettingsColumns.KEY, entry.getKey());
		values.put(AfSettingsColumns.VALUE, (String) entry.getValue());

		if (rowId != -1) {
			values.put(BaseColumns._ID, rowId);
			rowId = db.replace(TABLE_AIXWIDGETSETTINGS, null, values);
		} else {
			rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
		}

		return rowId;
	}
}
