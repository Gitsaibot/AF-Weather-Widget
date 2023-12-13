package net.gitsaibot.af.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.gitsaibot.af.AfProvider.AfSettingsColumns;
import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.R;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

public class AfWidgetSettings {

	private Context mContext;
	private Map<String, Object> mSettings;

	Set<String> mFloatKeys = new HashSet<>();
	Set<String> mIntegerKeys = new HashSet<>();

	private AfWidgetSettings(Context context) {
		mContext = context;
		
		mFloatKeys.add(context.getString(R.string.precipitation_scaling_string));
		mFloatKeys.add(context.getString(R.string.border_thickness_string));
		mFloatKeys.add(context.getString(R.string.border_rounding_string));
		
		mIntegerKeys.add(context.getString(R.string.temperature_units_string));
		mIntegerKeys.add(context.getString(R.string.precipitation_units_string));
		mIntegerKeys.add(context.getString(R.string.top_text_visibility_string));
	}
	
	private boolean addSetting(String key, String value)
	{
		boolean validSetting = true;
		
		if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value))
		{
			if (key.endsWith("bool")) {
				mSettings.put(key, Boolean.parseBoolean(value));
			} else if (key.endsWith("int")) {
				try {
					mSettings.put(key, Integer.parseInt(value));
				} catch (NumberFormatException e) {
					validSetting = false;
				}
			} else if (key.endsWith("string")) {
				validSetting = addSettingString(key, value);
			}
		}
		
		return validSetting;
	}

	private boolean addSettingString(String key, String value) {
		boolean validSetting = true;
		
		if (mFloatKeys.contains(key)) {
			try {
				float floatValue = Float.parseFloat(value);
				mSettings.put(key, floatValue);
			} catch (NumberFormatException e) {
				validSetting = false;
			}
		} else if (mIntegerKeys.contains(key)) {
			try {
				int intValue = Integer.parseInt(value);
				mSettings.put(key, intValue);
			} catch (NumberFormatException e) {
				validSetting = false;
			}
		} else {
			mSettings.put(key, value);
		}
		
		return validSetting;
	}
	
	public static AfWidgetSettings build(Context context, Uri widgetUri) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(context.getString(R.string.temperature_units_string), 1);
		settings.put(context.getString(R.string.precipitation_units_string), 1);
		settings.put(context.getString(R.string.top_text_visibility_string), 4);
		
		settings.put(context.getString(R.string.day_effect_bool), Boolean.TRUE);
		settings.put(context.getString(R.string.border_enabled_bool), Boolean.TRUE);
		
		AfWidgetSettings afWidgetSettings = new AfWidgetSettings(context);
		afWidgetSettings.mSettings = settings;
		
		ContentResolver resolver = context.getContentResolver();
		Cursor widgetSettingsCursor = resolver.query(
				Uri.withAppendedPath(widgetUri, AfWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		if (widgetSettingsCursor != null)
		{
			if (widgetSettingsCursor.moveToFirst()) {
				do {
					String key = widgetSettingsCursor.getString(AfSettingsColumns.KEY_COLUMN);
					String value = widgetSettingsCursor.getString(AfSettingsColumns.VALUE_COLUMN);
					afWidgetSettings.addSetting(key, value);
				} while (widgetSettingsCursor.moveToNext());
			}
			widgetSettingsCursor.close();
		}
	
		afWidgetSettings.setupDefaultIntegerSettings();
		afWidgetSettings.setupDefaultFloatSettings();
		
		return afWidgetSettings;
	}
	
	public Boolean getBooleanSetting(String key) {
		Boolean value = null;
		if (mSettings != null) {
			Object valueObject = mSettings.get(key);
			if (valueObject instanceof Boolean) {
				value = (Boolean)valueObject;
			}
		}
		return value;
	}
	
	public Float getFloatSetting(String key) {
		Float value = null;
		if (mSettings != null) {
			Object valueObject = mSettings.get(key);
			if (valueObject instanceof Float) {
				value = (Float)valueObject;
			}
		}
		return value;
	}
	
	public Integer getIntegerSetting(String key) {
		Integer value = null;
		if (mSettings != null) {
			Object valueObject = mSettings.get(key);
			if (valueObject instanceof Integer) {
				value = (Integer)valueObject;
			}
		}
		return value;
	}
	
	public String getStringSetting(String key) {
		String value = null;
		if (mSettings != null) {
			Object valueObject = mSettings.get(key);
			if (valueObject instanceof String) {
				value = (String)valueObject;
			}
		}
		return value;
	}
	
	public void setupDefaultFloatSettings()
	{
		Resources r = mContext.getResources();
		
		int[][] defaultFloats = {
				{ R.string.border_thickness_string, R.string.border_thickness_default },
				{ R.string.border_rounding_string, R.string.border_rounding_default },
				{ R.string.precipitation_scaling_string,
						useInches() ? R.string.precipitation_scaling_inches_default
									: R.string.precipitation_scaling_mm_default } };

		for (int[] defaultFloat : defaultFloats) {
			String key = r.getString(defaultFloat[0]);

			if (!mSettings.containsKey(key)) {
				String valueString = r.getString(defaultFloat[1]);
				float value = Float.parseFloat(valueString);
				mSettings.put(key, value);
			}
		}
	}
	
	public void setupDefaultIntegerSettings()
	{
		Resources r = mContext.getResources();

		int[][] defaultColors = {
				{ R.string.border_color_int, R.color.border },
				{ R.string.background_color_int, R.color.background },
				{ R.string.text_color_int, R.color.text },
				{ R.string.pattern_color_int, R.color.pattern },
				{ R.string.day_color_int, R.color.day },
				{ R.string.night_color_int, R.color.night },
				{ R.string.grid_color_int, R.color.grid },
				{ R.string.grid_outline_color_int, R.color.grid_outline },
				{ R.string.max_rain_color_int, R.color.maximum_rain },
				{ R.string.min_rain_color_int, R.color.minimum_rain },
				{ R.string.above_freezing_color_int, R.color.above_freezing },
				{ R.string.below_freezing_color_int, R.color.below_freezing } };

		for (int[] defaultColor : defaultColors) {
			String key = r.getString(defaultColor[0]);
			if (!mSettings.containsKey(key)) {
				int value = ContextCompat.getColor(mContext, defaultColor[1]);
				mSettings.put(key, value);
			}
		}
	}
	
	public boolean useFahrenheit() {
		Integer value = getIntegerSetting(mContext.getString(R.string.temperature_units_string));
		if (value != null)
		{
			return value == 2;
		}
		else
		{
			return false;
		}
	}
	
	public boolean useInches() {
		Integer value = getIntegerSetting(mContext.getString(R.string.precipitation_units_string));
		if (value != null)
		{
			return value == 2;
		}
		else
		{
			return false;
		}
	}
	
	public boolean drawTopText(boolean isLandscape) {
		Integer topTextVisibility = getIntegerSetting(mContext.getString(R.string.top_text_visibility_string));
		if (topTextVisibility != null)
		{
			return  (topTextVisibility == 2 && isLandscape) ||
					(topTextVisibility == 3 && !isLandscape) ||
					(topTextVisibility == 4);
		} else {
			return true;
		}
	}
	
	public boolean drawDayLightEffect() {
		Boolean value = getBooleanSetting(mContext.getString(R.string.day_effect_bool));
		if (value == null) value = true;
		return value;
	}
	
	public boolean drawBorder() {
		Boolean value = getBooleanSetting(mContext.getString(R.string.border_enabled_bool));
		if (value == null) value = true;
		return value;
	}
	
	public float getPrecipitationScaling() {
		Float value = getFloatSetting(mContext.getString(R.string.precipitation_scaling_string));
		if (value == null)
		{
			value = useInches() ? 0.05f : 1.0f;
		}
		return value;
	}
	
	public float getBorderThickness() {
		if (drawBorder())
		{
			Float value = getFloatSetting(mContext.getString(R.string.border_thickness_string));
			if (value == null)
			{
				value = 5.0f;
			}
			return value;
		}
		else
		{
			return 0.0f;
		}
	}
	
	public float getBorderRounding() {
		Float value = getFloatSetting(mContext.getString(R.string.border_rounding_string));
		if (value == null) value = 4.0f;
		return value;
	}
	
	public int getBorderColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.border_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.border);
		return color;
	}
	
	public int getBackgroundColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.background_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.background);
		return color;
	}
	
	public int getTextColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.text_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.text);
		return color;
	}
	
	public int getPatternColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.pattern_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.pattern);
		return color;
	}
	
	public int getDayColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.day_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.day);
		return color;
	}
	
	public int getNightColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.night_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.night);
		return color;
	}
	
	public int getGridColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.grid_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.grid);
		return color;
	}
	
	public int getGridOutlineColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.grid_outline_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.grid_outline);
		return color;
	}
	
	public int getMaxRainColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.max_rain_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.maximum_rain);
		return color;
	}
	
	public int getMinRainColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.min_rain_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.minimum_rain);
		return color;
	}
	
	public int getAboveFreezingColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.above_freezing_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.above_freezing);
		return color;
	}
	
	public int getBelowFreezingColor() {
		Integer color = getIntegerSetting(mContext.getString(R.string.below_freezing_color_int));
		if (color == null) color = ContextCompat.getColor(mContext, R.color.below_freezing);
		return color;
	}
	
}
