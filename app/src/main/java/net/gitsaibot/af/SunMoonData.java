package net.gitsaibot.af;

import net.gitsaibot.af.AfProvider.AfSunMoonDataColumns;
import android.database.Cursor;

public class SunMoonData {
	
	public long timeAdded;
	public long date;
	public long sunRise, sunSet;
	public long moonRise, moonSet;
	public int moonPhase;
	
	public SunMoonData() {
		
	}
	
	public static SunMoonData buildFromCursor(Cursor c) {
		SunMoonData smd = new SunMoonData();
		smd.timeAdded = c.getLong(AfSunMoonDataColumns.TIME_ADDED_COLUMN);
		smd.date = c.getLong(AfSunMoonDataColumns.DATE_COLUMN);
		smd.sunRise = c.getLong(AfSunMoonDataColumns.SUN_RISE_COLUMN);
		smd.sunSet = c.getLong(AfSunMoonDataColumns.SUN_SET_COLUMN);
		smd.moonRise = c.getLong(AfSunMoonDataColumns.MOON_RISE_COLUMN);
		smd.moonSet = c.getLong(AfSunMoonDataColumns.MOON_SET_COLUMN);
		smd.moonPhase = c.getInt(AfSunMoonDataColumns.MOON_PHASE_COLUMN);
		return smd;
	}
	
}
