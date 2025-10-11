package net.gitsaibot.af.widget;

import static net.gitsaibot.af.AfUtils.WEATHER_ICONS_DAY;
import static net.gitsaibot.af.AfUtils.WEATHER_ICONS_NIGHT;
import static net.gitsaibot.af.AfUtils.WEATHER_ICONS_POLAR;
import static net.gitsaibot.af.AfUtils.hcap;
import static net.gitsaibot.af.AfUtils.isPrime;
import static net.gitsaibot.af.AfUtils.lcap;
import static net.gitsaibot.af.AfUtils.truncateDay;
import static net.gitsaibot.af.AfUtils.truncateHour;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfSunMoonData;
import net.gitsaibot.af.IntervalData;
import net.gitsaibot.af.PointData;
import net.gitsaibot.af.R;
import net.gitsaibot.af.SunMoonData;
import net.gitsaibot.af.util.AfLocationInfo;
import net.gitsaibot.af.util.AfWidgetInfo;
import net.gitsaibot.af.util.AfWidgetSettings;
import net.gitsaibot.af.util.CatmullRomSpline;
import net.gitsaibot.af.util.Cubic;
import net.gitsaibot.af.util.Cubic.CubicResult;
import net.gitsaibot.af.util.DayState;
import net.gitsaibot.af.util.Pair;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

public class AfDetailedWidget {
	
	private static final String TAG = "AfDetailedWidget";
	
	/* Initial Properties */
	
	private final int mNumHours;
	private final int mNumWeatherDataBufferHours;
	
	private final Context mContext;
	
	private final AfLocationInfo mAfLocationInfo;
	private final AfWidgetSettings mWidgetSettings;
	
	/* Common Properties */
	
	private float mDP;
	
	private float mIconHeight;
	private float mIconSpacingY;
	private float mIconWidth;
	private float mLabelTextSize;
	private float mTextSize;
	
	private int mNumHoursBetweenSamples;
	
	private long mTimeFrom;
	private long mTimeNow;
	private long mTimeTo;	
	
	private ArrayList<IntervalData> mIntervalData;
	private ArrayList<PointData> mPointData;
	private ArrayList<SunMoonData> mSunMoonData;
	private ArrayList<Pair<Date, DayState>> mSunMoonTransitions;
	
	private ContentResolver mResolver;
	
	private Paint mAboveFreezingTemperaturePaint;
	private Paint mBackgroundPaint;
	private Paint mBelowFreezingTemperaturePaint;
	private Paint mBorderPaint;
	private Paint mGridOutlinePaint;
	private Paint mGridPaint;
	private Paint mLabelPaint;
	private Paint mPatternPaint;
	private Paint mTextPaint;
	
	private Paint mMinRainPaint, mMaxRainPaint;
	
	private TimeZone mUtcTimeZone = TimeZone.getTimeZone("UTC");
	
	/* Render Properties */

	private double mTemperatureValueMax, mTemperatureValueMin;
	private double mTemperatureRangeMax, mTemperatureRangeMin;
	
	private int mNumHorizontalCells, mNumVerticalCells;
	private double mCellSizeX, mCellSizeY;
	
	private String[] mTemperatureLabels;

	private Rect mGraphRect = new Rect();
	private Rect mWidgetBounds = new Rect();
	
	private int mWidgetHeight;
	private int mWidgetWidth;
	
	private RectF mBackgroundRect = new RectF();
	private RectF mBorderRect = new RectF();
	
	private AfDetailedWidget(final Context context, AfWidgetInfo widgetInfo, AfLocationInfo locationInfo) {
		mContext = context;
		
		mNumHours = 24;
		mNumWeatherDataBufferHours = 6;
		
		mAfLocationInfo = locationInfo;
		
		mWidgetSettings = widgetInfo.getWidgetSettings();
	}
	
	public static AfDetailedWidget build(final Context context, AfWidgetInfo widgetInfo, AfLocationInfo locationInfo) throws AfWidgetDrawException, AfWidgetDataException {
		AfDetailedWidget widget = new AfDetailedWidget(context, widgetInfo, locationInfo);
		return widget.initialize();
	}
	
	private AfDetailedWidget initialize() throws AfWidgetDrawException, AfWidgetDataException {
		mResolver = mContext.getContentResolver();

		setupTimesAndPointData();
		setupIntervalData();
		setupSampleTimes();
		setupEpochAndTimes();
		validatePointData();
		setupSunMoonData();
		setupSunMoonTransitions();
		setupPaints();
		
		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		mDP = dm.density;
		
		mTextSize = 10.0f * mDP;
		mLabelTextSize = 9.0f * mDP;
		mIconHeight = 19.0f * mDP;
		mIconWidth = 19.0f * mDP;
		mIconSpacingY = 2.0f * mDP;
		
		setupPaintDimensions();
		
		return this;
	}
	
	public Bitmap render(int width, int height, boolean isLandscape) throws AfWidgetDrawException {
		setupWidgetDimensions(width, height);
		Bitmap bitmap = Bitmap.createBitmap(mWidgetWidth, mWidgetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		float borderPadding = 1.0f;
		mBorderRect.set(
				Math.round(mWidgetBounds.left + borderPadding),
				Math.round(mWidgetBounds.top + borderPadding),
				Math.round(mWidgetBounds.right - borderPadding),
				Math.round(mWidgetBounds.bottom - borderPadding));
		
		float borderRounding = mWidgetSettings.getBorderRounding();
		float borderThickness = mWidgetSettings.getBorderThickness();
		
		float widgetBorder = Math.round(borderPadding + borderRounding + (borderThickness - borderRounding));
		mBackgroundRect.set(
				Math.round(mWidgetBounds.left + widgetBorder),
				Math.round(mWidgetBounds.top + widgetBorder),
				Math.round(mWidgetBounds.right - widgetBorder),
				Math.round(mWidgetBounds.bottom - widgetBorder));
		
		boolean drawTopText = mWidgetSettings.drawTopText(isLandscape);
		
		double minimumCellWidth = 8.0 * mDP;
		double minimumCellHeight = 8.0 * mDP;
		
		double reservedSpaceAboveGraph = mIconHeight + mIconSpacingY;
		double reservedSpaceBelowGraph = 2.0f * mDP; // 0.5 * minimumCellHeight;
		
		calculateDimensions(isLandscape, drawTopText, minimumCellWidth, minimumCellHeight, reservedSpaceAboveGraph, reservedSpaceBelowGraph);
		
		drawBackground(canvas);
		drawGrid(canvas);

		if (mWidgetSettings.drawDayLightEffect()) {
			drawDayAndNight(canvas);
		}
		
		Path minRainPath = new Path();
		Path maxRainPath = new Path();
		buildRainPaths(minRainPath, maxRainPath);
		drawRainPaths(canvas, minRainPath, maxRainPath);
		
		PointF[] temperaturePointArray = buildTemperaturePointArray(mPointData, mTimeFrom, mTimeTo, (float)mTemperatureRangeMax, (float)mTemperatureRangeMin);
		Path temperaturePath = CatmullRomSpline.buildPath(temperaturePointArray);
		
		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(mGraphRect.width(), mGraphRect.height());
		temperaturePath.transform(scaleMatrix);
		temperaturePath.offset(mGraphRect.left, mGraphRect.top);
		drawTemperature(canvas, temperaturePath);
		
		drawGridOutline(canvas);
		drawTemperatureLabels(canvas);
		drawHourLabels(canvas);
		drawWeatherIcons(canvas);
		
		if (drawTopText) {
			Float pressure = null, humidity = null, temperature = null;
			
			long error = Long.MAX_VALUE;
			
			for (PointData p : mPointData) {
				if (p.time != null)
				{
					long e = p.time - mTimeNow;
					
					if ((e >= 0) && (e < error) &&
						(e <= mNumHoursBetweenSamples * DateUtils.HOUR_IN_MILLIS))
					{
						pressure = p.pressure;
						humidity = p.humidity;
						temperature = p.temperature;
						error = e;
					}
				}
			}
			
			drawInfoText(canvas, pressure, humidity, temperature);
		}
		
		return bitmap;
	}

	private void buildRainPaths(Path minRainPath, Path maxRainPath) {
		Float[] rainValues = new Float[mNumHorizontalCells];
		Float[] rainMinValues = new Float[mNumHorizontalCells];
		Float[] rainMaxValues = new Float[mNumHorizontalCells];
		
		int[] pointers = new int[mNumHorizontalCells];
		int[] precision = new int[mNumHorizontalCells];
		
		Arrays.fill(pointers, -1);
		
		for (int dataIndex = 0; dataIndex < mIntervalData.size(); dataIndex++) {
			IntervalData d = mIntervalData.get(dataIndex);
			if (d.timeFrom.equals(d.timeTo)) continue;
			
			int startCell = (int)Math.floor((float)mNumHorizontalCells *
					(float)(d.timeFrom - mTimeFrom) / (float)(mTimeTo - mTimeFrom));
			
			if (startCell >= mNumHorizontalCells) continue;
			
			float endCellPos = (float)mNumHorizontalCells *
					(float)(d.timeTo - mTimeFrom) / (float)(mTimeTo - mTimeFrom);
			int endCell = (endCellPos == Math.round(endCellPos))
					? (int)endCellPos - 1 : (int)Math.ceil(endCellPos);
			
			startCell = lcap(startCell, 0);
			endCell = hcap(endCell, mNumHorizontalCells - 1);
			
			for (int cellIndex = startCell; cellIndex <= endCell; cellIndex++) {
				if (d.rainValue != null)
				{
					if ((pointers[cellIndex] == -1) ||
						(d.getLengthInHours() < precision[cellIndex]))
					{
						rainValues[cellIndex] = d.rainValue;
						rainMinValues[cellIndex] = d.rainMinValue;
						rainMaxValues[cellIndex] = d.rainMaxValue;
						
						precision[cellIndex] = d.getLengthInHours();
						pointers[cellIndex] = dataIndex;
					}
				}
			}
		}
		
		RectF rainRect = new RectF();
		
		float precipitationScale = mWidgetSettings.getPrecipitationScaling();
		
		for (int cellIndex = 0; cellIndex < mNumHorizontalCells;)
		{
			Float lowVal, highVal = null;
			
			if (rainMinValues[cellIndex] != null && rainMaxValues[cellIndex] != null)
			{
				lowVal = rainMinValues[cellIndex];
				highVal = rainMaxValues[cellIndex];
			}
			else if (rainValues[cellIndex] != null)
			{
				lowVal = rainValues[cellIndex];
			}
			else
			{
				cellIndex++;
				continue;
			}
			
			lowVal = hcap(lowVal / precipitationScale, mNumVerticalCells);
			
			int endCellIndex = cellIndex + 1;
			while (	(endCellIndex < mNumHorizontalCells) &&
					(pointers[cellIndex] == pointers[endCellIndex]) &&
					(pointers[endCellIndex] != -1))
			{
				endCellIndex++;
			}
			
			rainRect.set(
					(float)(mGraphRect.left + Math.round(cellIndex * mCellSizeX) + 1.0),
					(float)(mGraphRect.bottom - lowVal * mCellSizeY),
					(float)(mGraphRect.left + Math.round(endCellIndex * mCellSizeX)),
					(float)(mGraphRect.bottom));
			minRainPath.addRect(rainRect, Path.Direction.CCW);
			
			if (highVal != null)
			{
				highVal = hcap(highVal / precipitationScale, mNumVerticalCells);
				
				if (highVal > lowVal && highVal < mNumVerticalCells)
				{
					rainRect.bottom = rainRect.top;
					rainRect.top = (float)(mGraphRect.bottom - highVal * mCellSizeY);
					maxRainPath.addRect(rainRect, Path.Direction.CCW);
				}
			}
			
			cellIndex = endCellIndex;
		}
	}
	
	private PointF[] buildTemperaturePointArray(
			ArrayList<PointData> pointData,
			long timeIntervalStart, long timeIntervalEnd,
			float graphRangeMax, float graphRangeMin)
	{
		float graphRange = graphRangeMax - graphRangeMin;
		long timeInterval = timeIntervalEnd - timeIntervalStart;
		
		PointF[] points = new PointF[pointData.size()];
		
		for (int i = 0; i < pointData.size(); i++) {
			PointData p = pointData.get(i);
			
			float x = (float)(p.time - timeIntervalStart) / timeInterval;
			float y = 1.0f - (p.temperature - graphRangeMin) / graphRange;
			
			points[i] = new PointF(x, y);
		}
		
		return points;
	}
	
	private int calculateNumberOfHorizontalCells(int numMaxCells, double minimumCellSize, double availableCellSpace) throws AfWidgetDrawException {
		int numCells = 0;
		
		if (availableCellSpace < minimumCellSize) {
			throw new AfWidgetDrawException("Not enough horizontal graph space");
		}
		
		for (int i = 1; i <= numMaxCells; i++) {
			if (numMaxCells % i == 0) {
				double cellSize = availableCellSpace / (double)i;
				if (cellSize >= minimumCellSize) {
					numCells = i;
				} else {
					break;
				}
			}
		}
		
		return numCells;
	}
	
	private int calculateNumberOfVerticalCells(
			double availableCellSpace,
			double reservedSpaceAbove,
			double reservedSpaceBelow,
			double minimumCellSize,
			double maxDataValue,
			double minDataValue,
			double numUnitsPerCell) throws AfWidgetDrawException
	{
		if (availableCellSpace < reservedSpaceAbove + reservedSpaceBelow) {
			throw new AfWidgetDrawException("Not enough reserved vertical graph space");
		}
		
		if (availableCellSpace < minimumCellSize) {
			throw new AfWidgetDrawException("Not enough vertical graph space");
		}
		
		// Start by calculating the minimum number of vertical cells required to
		// fit the graph alone. Will always be at least 1 cell.
		int numGraphCellsRequired = Math.max(1,
				  ((int) Math.ceil(maxDataValue / numUnitsPerCell))
				- ((int) Math.floor(minDataValue / numUnitsPerCell)));
		
		for (int numVerticalCells = numGraphCellsRequired ;; numVerticalCells++) {
			double cellHeight = availableCellSpace / (double)numVerticalCells;
			
			if (cellHeight < minimumCellSize) {
				return 0;
			}
			
			int numRequiredCells = Math.max(1,
					  ((int) Math.ceil(maxDataValue / numUnitsPerCell + reservedSpaceAbove / cellHeight))
					- ((int) Math.floor(minDataValue / numUnitsPerCell - reservedSpaceBelow / cellHeight)));
			
			if (numVerticalCells >= numRequiredCells) {
				// If there is not enough space to label each vertical interval;
				// Ensure that there is a non-prime number of vertical cells such that
				// labels can be separated symmetrically.
				boolean isLabelSpaceConstrained = (double)mGraphRect.height() / (double)mNumVerticalCells < mLabelPaint.getTextSize();
				
				if (isLabelSpaceConstrained && isPrime(mNumVerticalCells)) {
					continue;
				}
				
				return numVerticalCells;
			}
		}
	}
	
	private double createVerticalGraphLabels(int numVerticalCells, double graphRangeMin, double numUnitsPerCell, Paint labelPaint) {
		double maxLabelWidth = 0.0;
		boolean isAllLabelsBelowTen = true;
		
		String formatting = Math.round(numUnitsPerCell) != numUnitsPerCell ? "%.1f\u00B0" : "%.0f\u00B0";
		
		mTemperatureLabels = new String[numVerticalCells + 1];
		
		for (int i = 0; i <= numVerticalCells; i++) {
			double value = graphRangeMin + numUnitsPerCell * i;
			
			if (Math.abs(value) >= 10.0) {
				isAllLabelsBelowTen = false;
			}
			
			String label = String.format(formatting, value);
			maxLabelWidth = Math.max(maxLabelWidth, labelPaint.measureText(label));
			
			mTemperatureLabels[i] = label;
		}
		
		if (isAllLabelsBelowTen) {
			maxLabelWidth += mDP;
		}
		
		return maxLabelWidth;
	}
	
	private int calculateStartCellOffset(int numVerticalCells, int numRequiredVerticalCells, double graphRangeMax, double numUnitsPerCell, double reservedSpaceAbove, double cellHeight) {
		// Calculate the space left in the cell containing the max-value of the graph
		double graphTopCellSpace = Math.ceil(graphRangeMax / numUnitsPerCell) - graphRangeMax / numUnitsPerCell;
		
		// Center range as far as possible (need to fix proper centering)
		int startCell = numVerticalCells - numRequiredVerticalCells;
		
		// Adjust the startCell offset to vertically center the graph
		while (startCell > 0) {
			double cellsAboveGraph = numVerticalCells - numRequiredVerticalCells - startCell + graphTopCellSpace;						
			
			if (cellsAboveGraph * cellHeight < reservedSpaceAbove) {
				startCell--;
			} else {
				break;
			}
		}
		
		return startCell;
	}
	
	private Rect calculateGraphRect(double verticalLabelWidth, double horizontalLabelHeight, boolean drawTopText, RectF parentRect) throws AfWidgetDrawException {
		Rect graphRect = new Rect();
		
		double topSpacing = drawTopText
				? horizontalLabelHeight + 4.0 * (double)mDP
				: horizontalLabelHeight / 2.0 + 2.0 * (double)mDP;
		
		graphRect.left = (int) Math.round((double)parentRect.left + verticalLabelWidth + 5.0 * (double)mDP);
		graphRect.top = (int) Math.round((double)parentRect.top + topSpacing);
		graphRect.right = (int) Math.round((double)parentRect.right - 5.0 * (double)mDP);
		graphRect.bottom = (int) Math.round((double)parentRect.bottom - horizontalLabelHeight - 6.0 * (double)mDP);
		
		// Sanity check
		if ((graphRect.left >= graphRect.right) || (graphRect.top >= graphRect.bottom)) {
			throw new AfWidgetDrawException("Failed to fit basic graph elements");
		}
		
		return graphRect;
	}
	
	private void calculateDimensions(
			final boolean isLandscape,
			final boolean drawTopText,
			final double minimumCellWidth,
			final double minimumCellHeight,
			final double reservedSpaceAboveGraph,
			final double reservedSpaceBelowGraph
	)
		throws AfWidgetDrawException
	{
		double[] degreesPerCellOptions = { 0.5, 1.0, 2.0, 2.5, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0 };
		double textLabelWidth = 0.0;
		
		// timeoutCounter is used to ensure that the loop will not run forever
		
		for (	int degreesPerCellIndex = 0, timeoutCounter = 0;
				degreesPerCellIndex < degreesPerCellOptions.length && timeoutCounter < 3;
				timeoutCounter++)
		{
			double degreesPerCell = degreesPerCellOptions[degreesPerCellIndex];
			
			mGraphRect = calculateGraphRect(textLabelWidth, mLabelTextSize, drawTopText, mBackgroundRect);
			mNumHorizontalCells = calculateNumberOfHorizontalCells(mNumHours, minimumCellWidth, mGraphRect.width());
			mCellSizeX = (double)mGraphRect.width() / (double)mNumHorizontalCells;
			
			mNumVerticalCells = calculateNumberOfVerticalCells(mGraphRect.height(), reservedSpaceAboveGraph, reservedSpaceBelowGraph, minimumCellHeight, mTemperatureValueMax, mTemperatureValueMin, degreesPerCell);
			
			if (mNumVerticalCells == 0) {
				timeoutCounter = 0;
				textLabelWidth = 0.0;
				degreesPerCellIndex++;
				continue;
			}
			
			mCellSizeY = (float)mGraphRect.height() / (float) mNumVerticalCells;
			
			int numRequiredVerticalCells = Math.max(1,
					  ((int) Math.ceil(mTemperatureValueMax / degreesPerCell))
					- ((int) Math.floor(mTemperatureValueMin / degreesPerCell)));
			
			int startCell = calculateStartCellOffset(mNumVerticalCells, numRequiredVerticalCells, mTemperatureValueMax, degreesPerCell, reservedSpaceAboveGraph, mCellSizeY);

			mTemperatureRangeMin = degreesPerCell * (float)(Math.floor(mTemperatureValueMin / degreesPerCell) - startCell);
			mTemperatureRangeMax = mTemperatureRangeMin + degreesPerCell * mNumVerticalCells;
			
			double tempMaxLabelWidth = createVerticalGraphLabels(mNumVerticalCells, mTemperatureRangeMin, degreesPerCell, mLabelPaint);
			
			if (tempMaxLabelWidth <= textLabelWidth) {
				return;
			}
			
			textLabelWidth = tempMaxLabelWidth;
		}
		
		throw new AfWidgetDrawException("Failed to calculate graph dimensions");
	}
	
	private void drawBackground(Canvas canvas)
	{
		float borderThickness = mWidgetSettings.getBorderThickness();
		float borderRounding = mWidgetSettings.getBorderRounding();
		
		if (borderThickness > 0.0f) {
			canvas.save();
			canvas.clipOutRect(mBackgroundRect);
			canvas.drawRoundRect(mBorderRect, borderRounding, borderRounding, mBorderPaint);
			canvas.restore();
			
			canvas.drawRect(mBackgroundRect, mBackgroundPaint);
			canvas.drawRect(mBackgroundRect, mPatternPaint);
		} else {
			canvas.drawRoundRect(mBackgroundRect, borderRounding, borderRounding, mBackgroundPaint);
			canvas.drawRoundRect(mBackgroundRect, borderRounding, borderRounding, mPatternPaint);
		}
	}
	
	private void drawDayAndNight(Canvas canvas)
	{
		if (mSunMoonTransitions == null || mSunMoonTransitions.size() < 2) return;
		
		float timeRange = mTimeTo - mTimeFrom;
		float transitionWidthDefault = (float)DateUtils.HOUR_IN_MILLIS / timeRange;
		
		canvas.save();
		canvas.clipRect(mGraphRect.left + 1, mGraphRect.top, mGraphRect.right, mGraphRect.bottom);
		
		Matrix matrix = new Matrix();
		matrix.setScale(mGraphRect.width(), mGraphRect.height());
		matrix.postTranslate(mGraphRect.left + 1, mGraphRect.top);
		canvas.setMatrix(matrix);
		
		Paint paint = new Paint();
		paint.setStyle(Style.FILL);

		final int dayColor = mWidgetSettings.getDayColor();
		final int nightColor = mWidgetSettings.getNightColor();

		float marker = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < mSunMoonTransitions.size() - 1; i++)
		{
			Pair<Date, DayState> previous = mSunMoonTransitions.get(i);
			Pair<Date, DayState> current = mSunMoonTransitions.get(i + 1);
			Pair<Date, DayState> next = (i < mSunMoonTransitions.size() - 2)
				? mSunMoonTransitions.get(i + 2) : null;

			float transitionWidth = hcap(
				transitionWidthDefault,
				(float)(current.first.getTime() - previous.first.getTime()) / timeRange);

			if (next != null) {
				transitionWidth = hcap(transitionWidth,
					(float)(next.first.getTime() - current.first.getTime()) / timeRange);
			}

			float transitionPosition = (float)(current.first.getTime() - mTimeFrom) / timeRange;
			float transitionStart = transitionPosition - transitionWidth;
			float transitionEnd = transitionPosition + transitionWidth;

			paint.setShader(new LinearGradient(transitionStart, 0.0f, transitionEnd, 0.0f,
				previous.second == DayState.DAY ? dayColor : nightColor,
				current.second == DayState.DAY ? dayColor : nightColor,
				Shader.TileMode.CLAMP));

			if (marker == Float.NEGATIVE_INFINITY) {
				marker = (float)(previous.first.getTime() - mTimeFrom) / timeRange;
			}

			canvas.drawRect(marker, 0.0f, transitionEnd, 1.0f, paint);

			marker = transitionEnd;
		}
		
		canvas.restore();
	}
	
	private void drawGrid(Canvas canvas) {
		Path gridPath = new Path();
		
		for (int i = 1; i <= mNumHorizontalCells; i++) {
			float xPos = mGraphRect.left + Math.round(i * mCellSizeX);
			gridPath.moveTo(xPos, mGraphRect.bottom);
			gridPath.lineTo(xPos, mGraphRect.top);
		}

		for (int i = 1; i <= mNumVerticalCells; i++) {
			float yPos = mGraphRect.bottom - Math.round(i * mCellSizeY);
			gridPath.moveTo(mGraphRect.left, yPos);
			gridPath.lineTo(mGraphRect.right, yPos);
		}
		
		boolean drawDayLightEffect = mWidgetSettings.drawDayLightEffect();
		
		int rightOffset = drawDayLightEffect ? 0 : 1;
		
		canvas.save();
		canvas.clipRect(
				mGraphRect.left + 1,
				mGraphRect.top,
				mGraphRect.right + rightOffset,
				mGraphRect.bottom);
		canvas.drawPath(gridPath, mGridPaint);
		canvas.restore();
	}
	
	private void drawGridOutline(Canvas canvas) {
		Path gridOutline = new Path();
		gridOutline.moveTo(mGraphRect.left, mGraphRect.top);
		gridOutline.lineTo(mGraphRect.left, mGraphRect.bottom);
		gridOutline.lineTo(mGraphRect.right + 1, mGraphRect.bottom);
		
		if (mWidgetSettings.drawDayLightEffect()) {
			gridOutline.moveTo(mGraphRect.right, mGraphRect.top);
			gridOutline.lineTo(mGraphRect.right, mGraphRect.bottom);
		}
		
		canvas.drawPath(gridOutline, mGridOutlinePaint);
	}
	
	private void drawHourLabels(Canvas canvas) throws AfWidgetDrawException {
		// Draw time stamp labels and horizontal notches
		float notchHeight = 3.5f * mDP;
		
		mLabelPaint.setTextAlign(Paint.Align.CENTER);
		
		Calendar calendar = Calendar.getInstance(mAfLocationInfo.buildTimeZone());
		calendar.setTimeInMillis(mTimeFrom);
		
		int startHour = calendar.get(Calendar.HOUR_OF_DAY);

		float hoursPerCell = (float)mNumHours / (float)mNumHorizontalCells;
		int numCellsBetweenHorizontalLabels = mNumHoursBetweenSamples < hoursPerCell
				? 1 : (int)Math.floor((float)mNumHoursBetweenSamples / hoursPerCell);
		
		// HARDCODED LIMIT
		if (mNumHours == 24) {
			numCellsBetweenHorizontalLabels = lcap(numCellsBetweenHorizontalLabels, 2);
		}
		
		boolean useShortLabel;
		boolean use24hours = DateFormat.is24HourFormat(mContext);
		
		float longLabelWidth = mLabelPaint.measureText(use24hours ? "24" : "12 pm");
		float shortLabelWidth = mLabelPaint.measureText("12p");
		
		while (true) {
			/* Ensure that labels can be evenly spaced, given the number of cells, and make
			 * sure that each label step is a full number of hours.
			 */
			while ((mNumHorizontalCells % numCellsBetweenHorizontalLabels != 0) ||
					((hoursPerCell * numCellsBetweenHorizontalLabels) !=
							Math.round(hoursPerCell * numCellsBetweenHorizontalLabels)))
			{
				if (numCellsBetweenHorizontalLabels > mNumHorizontalCells)
				{
					throw new AfWidgetDrawException("Failed to space horizontal cells");
				}
				else
				{
					numCellsBetweenHorizontalLabels++;
				}
			}
			
			double spaceBetweenLabels = numCellsBetweenHorizontalLabels * mCellSizeX;
			if (spaceBetweenLabels > longLabelWidth * 1.25f) {
				useShortLabel = false;
				break;
			} else if (!use24hours && spaceBetweenLabels > shortLabelWidth * 1.25f) {
				useShortLabel = true;
				break;
			}
			
			numCellsBetweenHorizontalLabels++;
		}
		
		for (int i = numCellsBetweenHorizontalLabels;
				 i < mNumHorizontalCells;
				 i+= numCellsBetweenHorizontalLabels)
		{
			float notchX = mGraphRect.left + Math.round(i * mCellSizeX);
			canvas.drawLine(notchX, (float)mGraphRect.bottom - notchHeight / 2.0f,
					notchX, (float)mGraphRect.bottom + notchHeight / 2.0f, mGridOutlinePaint);
			
			int hour = startHour + (int)(hoursPerCell * i);
			
			String hourLabel;
			if (use24hours) {
				hourLabel = String.format(Locale.US, "%02d", hour % 24);
			} else {
				int hour12 = hour % 12;
				if (0 == hour12) hour12 = 12;
				boolean am = (hour % 24) < 12;
				if (useShortLabel) {
					hourLabel = String.format(Locale.US, "%2d%s", hour12, am ? "a" : "p" );
				} else {
					hourLabel = String.format(Locale.US, "%2d %s", hour12, am ? "am" : "pm" );
				}
			}
			canvas.drawText(hourLabel, notchX, (float)Math.floor(((float)mGraphRect.bottom + mBackgroundRect.bottom) / 2.0f + mLabelPaint.getTextSize() / 2.0f), mLabelPaint);
		}
	}

	private void drawInfoText(Canvas canvas, Float pressure, Float humidity, Float temperature)
	{
		float topTextSidePadding = mDP;
		
		String locationName = mAfLocationInfo.getTitle();
		
		String pressureString = pressure != null ? mContext.getString(R.string.pressure_top, pressure) : "";
		String humidityString = humidity != null ? mContext.getString(R.string.humidity_top, humidity) : "";
		
		float precipitationScale = mWidgetSettings.getPrecipitationScaling();
		boolean useInches = mWidgetSettings.useInches();
		boolean useFahrenheit = mWidgetSettings.useFahrenheit();
		
		DecimalFormat df = new DecimalFormat(mContext.getString(R.string.rain_scale_format));
		String rainScaleFormatted = df.format(precipitationScale);

		String rainScaleUnit = mContext.getString(useInches ? R.string.rain_scale_unit_inches
																 : R.string.rain_scale_unit_mm);
		
		String rainScaleString = mContext.getString(R.string.rain_scale_top, rainScaleFormatted, rainScaleUnit);
		
		String temperatureString = "";
		if (temperature != null)
		{
			df = new DecimalFormat(mContext.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = mContext.getString(
					useFahrenheit ? R.string.temperature_unit_fahrenheit
								   : R.string.temperature_unit_celsius);
			
			temperatureString = ' ' + mContext.getString(
					R.string.temperature_top, temperatureFormatted, temperatureUnit);
		}
		
		float topTextSpace = mGraphRect.width() - topTextSidePadding * 2.0f;
		
		String ellipsis = "...";
		String spacing = "       ";
		
		boolean isMeasuring = true;
		int measureState = 0;
		
		while ( isMeasuring &&
				topTextSpace < mTextPaint.measureText(pressureString) +
							   mTextPaint.measureText(humidityString) +
							   mTextPaint.measureText(rainScaleString) +
							   mTextPaint.measureText(locationName) +
							   mTextPaint.measureText(temperatureString) +
							   mTextPaint.measureText(spacing)) // Spacing between strings
		{
			switch (measureState) {
			case 0:
				if (pressure != null)
				{
					pressureString = mContext.getString(R.string.pressure_top_short, pressure);
				}
				measureState++;
				break;
			case 1:
				if (humidity != null)
				{
					humidityString = mContext.getString(R.string.humidity_top_short, humidity);
				}
				measureState++;
				break;
			case 2:
				if (locationName.isEmpty()) {
					isMeasuring = false;
					break;
				}
				spacing = spacing + ellipsis;
				measureState++;
			default:
				locationName = locationName.substring(0, locationName.length() - 1);
				if (measureState > locationName.length() + 2) isMeasuring = false;
				break;
			}
		}
		
		if (measureState == 3) {
			locationName = locationName + ellipsis;
		}

		StringBuilder sb = new StringBuilder();

		if (!pressureString.isEmpty()) {
			sb.append(pressureString);
			sb.append("  ");
		}
		
		if (!humidityString.isEmpty()) {
			sb.append(humidityString);
			sb.append("  ");
		}
		
		if (!rainScaleString.isEmpty()) {
			sb.append(rainScaleString);
		}
		
		mTextPaint.setTextAlign(Align.LEFT);
		canvas.drawText(sb.toString(),
				mGraphRect.left + topTextSidePadding,
				mBackgroundRect.top + mTextPaint.getTextSize(),
				mTextPaint);
		
		sb.setLength(0);
		sb.append(locationName);

		if (temperature != null) {
			df = new DecimalFormat(mContext.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = mContext.getString(
					useFahrenheit ? R.string.temperature_unit_fahrenheit
								   : R.string.temperature_unit_celsius);
			
			sb.append(' ');
			sb.append(mContext.getString(
					R.string.temperature_top,
					temperatureFormatted,
					temperatureUnit));
		}

		mTextPaint.setTextAlign(Align.RIGHT);
		canvas.drawText(sb.toString(),
				mGraphRect.right - topTextSidePadding,
				mBackgroundRect.top + mTextPaint.getTextSize(),
				mTextPaint);
	}
	
	private void drawRainPaths(Canvas canvas, Path minRainPath, Path maxRainPath)
	{
		canvas.drawPath(minRainPath, mMinRainPaint);
		canvas.save();
		canvas.clipPath(maxRainPath);
		
		float dimensions = (float)Math.sin(Math.toRadians(45)) *
						   (mGraphRect.height() + mGraphRect.width());
		float dimensions2 = dimensions / 2.0f;
		float ggx = mGraphRect.left + mGraphRect.width() / 2.0f;
		float ggy = mGraphRect.top + mGraphRect.height() / 2.0f;
	
		Matrix transform = new Matrix();
		transform.setRotate(-45.0f, ggx, ggy);
		canvas.setMatrix(transform);
		
		float ypos = mGraphRect.top - (dimensions - mGraphRect.height()) / 2.0f;
		float ytest = mGraphRect.bottom + (dimensions - mGraphRect.height()) / 2.0f;
		while (ypos < ytest) {
			canvas.drawLine(ggx - dimensions2, ypos, ggx + dimensions2, ypos, mMaxRainPaint);
			ypos += 2.0f * mDP;
		}
		canvas.restore();
	}

	private void drawTemperature(Canvas canvas, Path temperaturePath)
	{
		float freezingTemperature = mWidgetSettings.useFahrenheit() ? 32.0f : 0.0f;

		// This is the y-coordinate on the graph that corresponds to the freezing temperature.
		float freezingPosY = mGraphRect.bottom - (float)((freezingTemperature - mTemperatureRangeMin) / (mTemperatureRangeMax - mTemperatureRangeMin) * mGraphRect.height());

		// Ensure the position is within the graph bounds
		freezingPosY = Math.max(mGraphRect.top, Math.min(mGraphRect.bottom, freezingPosY));

		RectF graphRectInner = new RectF(mGraphRect.left + 1, mGraphRect.top, mGraphRect.right, mGraphRect.bottom);

		canvas.save();
		if (mTemperatureRangeMin >= freezingTemperature) {
			// All above freezing
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
		} else if (mTemperatureRangeMax <= freezingTemperature) {
			// All below freezing
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		} else {
			// Path is both above and below freezing, split the drawing
			canvas.clipRect(graphRectInner.left, graphRectInner.top, graphRectInner.right, freezingPosY);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
			canvas.restore(); // Restore from the first clip

			canvas.save();
			canvas.clipRect(graphRectInner.left, freezingPosY, graphRectInner.right, graphRectInner.bottom);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		}
		canvas.restore();
	}
	
	private void drawTemperatureLabels(Canvas canvas) {
		float labelTextPaddingX = 2.5f * mDP;
		float notchWidth = 3.5f * mDP;
		
		// Find the minimum number of cells between each label
		int numCellsBetweenVerticalLabels = 1;
		while (numCellsBetweenVerticalLabels * mCellSizeY < mLabelPaint.getTextSize()) {
			numCellsBetweenVerticalLabels++;
		}
		// Ensure that labels can be evenly spaced, given the number of cells
		while (mNumVerticalCells % numCellsBetweenVerticalLabels != 0) {
			numCellsBetweenVerticalLabels++;
		}
		
		if (mTemperatureLabels.length < mNumVerticalCells) return;
		
		mLabelPaint.setTextAlign(Paint.Align.RIGHT);
		
		Rect bounds = new Rect();
		
		for (int i = 0; i <= mNumVerticalCells; i += numCellsBetweenVerticalLabels) {
			float notchY = mGraphRect.bottom - Math.round(i * mCellSizeY);
			canvas.drawLine(
					mGraphRect.left - notchWidth / 2, notchY,
					mGraphRect.left + notchWidth / 2, notchY,
					mGridOutlinePaint);
			
			mLabelPaint.getTextBounds(mTemperatureLabels[i], 0, mTemperatureLabels[i].length(), bounds);
			
			canvas.drawText(
					mTemperatureLabels[i],
					mGraphRect.left - labelTextPaddingX,
					mGraphRect.bottom - bounds.centerY()
							- (float)(i * mGraphRect.height()) / (float)mNumVerticalCells,
					mLabelPaint);
		}
	}

	// DELETE the current drawWeatherIcons method and REPLACE it with this one.
	private void drawWeatherIcons(Canvas canvas) {
		int hoursPerIcon = mNumHoursBetweenSamples; // 2 hours
		int numIcons = mNumHours / hoursPerIcon;   // 12 icons

		Calendar calendar = Calendar.getInstance(mUtcTimeZone);

		// Loop through each of the 12 icon slots
		for (int i = 0; i < numIcons; i++) {

			long slotStartTime = mTimeFrom + (i * hoursPerIcon * DateUtils.HOUR_IN_MILLIS);
			long secondHourTime = slotStartTime + DateUtils.HOUR_IN_MILLIS;
			long slotCenterTime = slotStartTime + (hoursPerIcon * DateUtils.HOUR_IN_MILLIS / 2);

			// For a 2-hour slot, we look for data for the two 1-hour intervals within it.
			// We prioritize the data for the second hour as it's more representative.
			IntervalData firstHourData = null;
			IntervalData secondHourData = null;

			for (IntervalData dataPoint : mIntervalData) {
				// Basic validation for the data point
				if (dataPoint.weatherIcon == null || dataPoint.weatherIcon < 1 || dataPoint.weatherIcon > 50) {
					continue;
				}
				// Check if this data point matches the first hour of slot
				if (dataPoint.timeFrom == slotStartTime) {
					firstHourData = dataPoint;
				}
				// Check if this data point matches the second hour of slot
				if (dataPoint.timeFrom == secondHourTime) {
					secondHourData = dataPoint;
				}
				// If both are found, stop searching for this slot
				if (firstHourData != null && secondHourData != null) {
					break;
				}
			}

			IntervalData dataForIcon;
			// Prioritize the second hour's data. If it doesn't exist, use the first hour's.
			if (secondHourData != null) {
				dataForIcon = secondHourData;
			} else {
				dataForIcon = firstHourData;
			}

			if (dataForIcon == null) {
				continue; // No icon found for this slot, move to the next one
			}

			// --- From here, the rest of the method is for positioning and drawing ---
			// Find the HIGHEST temperature on the graph line underneath the icon's width.
			long iconTimeWidth = (long) (mIconWidth / (float) mGraphRect.width() * (mTimeTo - mTimeFrom));
			long leftTime = slotCenterTime - (iconTimeWidth / 2);
			long centerTime = slotCenterTime;
			long rightTime = slotCenterTime + (iconTimeWidth / 2);

			PointF leftTempPoint = interpolateTemperature(leftTime);
			PointF centerTempPoint = interpolateTemperature(centerTime);
			PointF rightTempPoint = interpolateTemperature(rightTime);

			float highestTemperatureValue = Float.NEGATIVE_INFINITY;
			if (leftTempPoint != null) {
				highestTemperatureValue = Math.max(highestTemperatureValue, leftTempPoint.y);
			}
			if (centerTempPoint != null) {
				highestTemperatureValue = Math.max(highestTemperatureValue, centerTempPoint.y);
			}
			if (rightTempPoint != null) {
				highestTemperatureValue = Math.max(highestTemperatureValue, rightTempPoint.y);
			}

			if (highestTemperatureValue == Float.NEGATIVE_INFINITY) {
				continue;
			}

			// Calculate icon position
			double verticalPositionOnGraph = mGraphRect.height() * (highestTemperatureValue - mTemperatureRangeMin) / (mTemperatureRangeMax - mTemperatureRangeMin);
			double horizontalPosition = ((double) (slotCenterTime - mTimeFrom) / (double) (mTimeTo - mTimeFrom)) * mGraphRect.width();
			int iconX = (int) Math.round(mGraphRect.left + horizontalPosition - (double) mIconWidth / 2.0);
			int iconY = (int) Math.round((mGraphRect.bottom - verticalPositionOnGraph) - mIconHeight - mIconSpacingY);

			iconY = lcap(iconY, mGraphRect.top);
			iconY = hcap(iconY, mGraphRect.bottom - (int) Math.ceil(mIconHeight));
			Rect dest = new Rect(iconX, iconY, Math.round(iconX + mIconWidth), Math.round(iconY + mIconHeight));

			// Determine Day/Night/Polar icons
			calendar.setTimeInMillis(slotCenterTime);
			truncateDay(calendar);
			long iconDate = calendar.getTimeInMillis();
			int[] weatherIcons = WEATHER_ICONS_NIGHT; // Default to Night

			SunMoonData relevantSmd = null;
			for (SunMoonData smd : mSunMoonData) {
				if (smd.date == iconDate) {
					relevantSmd = smd;
					break;
				}
			}

			if (relevantSmd != null) {
				if (relevantSmd.sunRise == AfSunMoonData.NEVER_RISE) {
					weatherIcons = WEATHER_ICONS_POLAR; // Polar Night
				} else if (relevantSmd.sunSet == AfSunMoonData.NEVER_SET) {
					weatherIcons = WEATHER_ICONS_DAY; // Polar Day
				} else if (slotCenterTime >= relevantSmd.sunRise && slotCenterTime < relevantSmd.sunSet) {
					weatherIcons = WEATHER_ICONS_DAY; // Standard Day
				}
			}

			// This check ensures we don't try to access an index that doesn't exist
			// or one that is a placeholder in the array.
			int iconResourceId = weatherIcons[dataForIcon.weatherIcon - 1];
			if (iconResourceId == 0) {
				continue;
			}

			// Draw the final icon
			Bitmap weatherIcon = ((BitmapDrawable) Objects.requireNonNull(ResourcesCompat.getDrawable(
					mContext.getResources(), iconResourceId, null))).getBitmap();
			canvas.drawBitmap(weatherIcon, null, dest, null);
		}
	}

	private PointF interpolateTemperature(long time) {
		PointData before = null;
		PointData after = null;

		// Find the closest data points before and after the requested time
		for (PointData p : mPointData) {
			if (p.time != null && p.temperature != null) {
				if (p.time <= time && (before == null || p.time > before.time)) {
					before = p;
				}
				if (p.time >= time && (after == null || p.time < after.time)) {
					after = p;
				}
			}
		}

		// If we can't find points, we can't interpolate.
		if (before == null && after == null) {
			return null;
		}
		// If time is outside the known data range, clamp to the nearest point.
		if (before == null) {
			return new PointF(0, after.temperature);
		}
		if (after == null) {
			return new PointF(0, before.temperature);
		}

		// If the points are the same, just return that point
		if (before.time.equals(after.time)) {
			return new PointF(0, before.temperature);
		}

		// Perform linear interpolation to find the exact temperature at the given time.
		double t = (double) (time - before.time) / (double) (after.time - before.time);
		float interpolatedTemp = (float) (before.temperature * (1 - t) + after.temperature * t);

		// The 'x' coordinate in the returned PointF is not used by the new drawWeatherIcons,
		// but we return the real temperature in 'y'.
		return new PointF(0, interpolatedTemp);
	}

	private void setupIntervalData() {
		ArrayList<IntervalData> intervalData = new ArrayList<>();

		boolean useInches = mWidgetSettings.useInches();

		final Uri uri = mAfLocationInfo.getLocationUri().buildUpon()
				.appendPath(AfLocations.TWIG_INTERVALDATAFORECASTS)
				.appendQueryParameter("start", Long.toString(mTimeFrom))
				.appendQueryParameter("end", Long.toString(mTimeTo)).build();
		
		Cursor cursor = mResolver.query(uri, null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					try {
						IntervalData d = IntervalData.buildFromCursor(cursor);
						
						if (useInches)
						{
							if (d.rainValue != null) d.rainValue = d.rainValue / 25.4f;
							if (d.rainMinValue != null) d.rainMinValue = d.rainMinValue / 25.4f;
							if (d.rainMaxValue != null) d.rainMaxValue = d.rainMaxValue / 25.4f;
						}
						
						intervalData.add(d);
					} catch (Exception e) { Log.d(TAG, "setupIntervalData(): Adding IntervalData from cursor failed: " + e.getMessage()); }
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		
		mIntervalData = intervalData;
	}

	
	private void setupPaintDimensions()
	{
		mTextPaint.setTextSize(mTextSize);
		mLabelPaint.setTextSize(mLabelTextSize);
		mAboveFreezingTemperaturePaint.setStrokeWidth(2.0f * mDP);
		mBelowFreezingTemperaturePaint.setStrokeWidth(2.0f * mDP);
		mMaxRainPaint.setStrokeWidth(mDP);
	}
	
	private void setupPaints() {
		mBorderPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getBorderColor());
			setStyle(Paint.Style.FILL);
		}};
		mBackgroundPaint = new Paint() {{
			setColor(mWidgetSettings.getBackgroundColor());
			setStyle(Paint.Style.FILL);
		}};
		final Bitmap bgPattern = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pattern);
		mPatternPaint = new Paint() {{
			setShader(new BitmapShader(bgPattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
			setColorFilter(new PorterDuffColorFilter(mWidgetSettings.getPatternColor(), PorterDuff.Mode.SRC_IN));
		}};
		
		mTextPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getTextColor());
		}};
		mLabelPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getTextColor());
		}};
		
		mGridPaint = new Paint() {{
			setColor(mWidgetSettings.getGridColor());
			setStyle(Paint.Style.STROKE);
		}};
		mGridOutlinePaint = new Paint() {{
			setColor(mWidgetSettings.getGridOutlineColor());
			setStyle(Paint.Style.STROKE);
		}};
		
		mAboveFreezingTemperaturePaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getAboveFreezingColor());
			setStrokeCap(Paint.Cap.ROUND);
			setStyle(Paint.Style.STROKE);
		}};
		mBelowFreezingTemperaturePaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getBelowFreezingColor());
			setStrokeCap(Paint.Cap.ROUND);
			setStyle(Paint.Style.STROKE);
		}};
		
		mMinRainPaint = new Paint() {{
			setAntiAlias(false);
			setColor(mWidgetSettings.getMinRainColor());
			setStyle(Paint.Style.FILL);
		}};
		mMaxRainPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mWidgetSettings.getMaxRainColor());
			setStrokeCap(Paint.Cap.SQUARE);
			setStyle(Paint.Style.STROKE);
		}};
	}
	
	private void setupSunMoonData() {
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(mTimeFrom);
		truncateDay(calendar);
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		long dateFrom = calendar.getTimeInMillis();
		
		calendar.setTimeInMillis(mTimeTo);
		truncateDay(calendar);
		calendar.add(Calendar.DAY_OF_YEAR, +1);
		long dateTo = calendar.getTimeInMillis();
		
		ArrayList<SunMoonData> sunMoonData = new ArrayList<>();

		final Uri uri = mAfLocationInfo.getLocationUri().buildUpon()
				.appendPath(AfLocations.TWIG_SUNMOONDATA)
				.appendQueryParameter("start", Long.toString(dateFrom))
				.appendQueryParameter("end", Long.toString(dateTo)).build();

		Cursor c = mResolver.query(uri, null, null, null, null);

		if (c != null) {
			if (c.moveToFirst()) {
				do {
					sunMoonData.add(SunMoonData.buildFromCursor(c));
				} while (c.moveToNext());
			}
			c.close();
		}
		
		mSunMoonData = sunMoonData;
	}

	private void setupSunMoonTransitions() {
		ArrayList<Pair<Date, DayState>> transitions = new ArrayList<>();

		for (SunMoonData s : mSunMoonData) {
			if (s.sunRise > 0) {
				transitions.add(Pair.create(new Date(s.sunRise), DayState.DAY));
			}
			if (s.sunSet > 0) {
				transitions.add(Pair.create(new Date(s.sunSet), DayState.NIGHT));
			}
		}

		transitions.sort(new Comparator<>() {
			@Override
			public int compare(final Pair<Date, DayState> t1, final Pair<Date, DayState> t2) {
				int dateComparison = t1.first.compareTo(t2.first);
				if (dateComparison != 0) {
					return dateComparison;
				}
				return t1.second == DayState.DAY ? 1 : -1;
			}
		});

		if (!mSunMoonData.isEmpty()) {
			SunMoonData firstSunMoonData = mSunMoonData.get(0);
			Pair<Date, DayState> firstTransition = !transitions.isEmpty() ? transitions.get(0) : null;

			if (firstTransition == null || firstSunMoonData.date < firstTransition.first.getTime()) {
				if (firstSunMoonData.sunRise == AfSunMoonData.NEVER_RISE) {
					// Polar night
					transitions.add(0, Pair.create(new Date(firstSunMoonData.date), DayState.NIGHT));
				} else if (firstSunMoonData.sunRise == 0) {
					// Polar day
					transitions.add(0, Pair.create(new Date(firstSunMoonData.date), DayState.DAY));
				} else if (firstTransition != null) {
					transitions.add(0, Pair.create(
						new Date(firstSunMoonData.date),
						firstTransition.second == DayState.DAY ? DayState.NIGHT : DayState.DAY));
				}
			}
		}

		// Remove non-transitions
		for (int i = 0; i < transitions.size() - 1;) {
			if (transitions.get(i).second == transitions.get(i + 1).second) {
				transitions.remove(i + 1);
			}
			else {
				i += 1;
			}
		}

		if (!mSunMoonData.isEmpty() && !transitions.isEmpty()) {
			SunMoonData lastSunMoonData = mSunMoonData.get(mSunMoonData.size() - 1);
			Pair<Date, DayState> lastTransition = transitions.get(transitions.size() - 1);

			if (lastSunMoonData.date + DateUtils.DAY_IN_MILLIS > lastTransition.first.getTime()) {
				transitions.add(Pair.create(
					new Date(lastSunMoonData.date + DateUtils.DAY_IN_MILLIS),
					lastTransition.second == DayState.DAY ? DayState.NIGHT : DayState.DAY));
			}
		}

		mSunMoonTransitions = transitions;
	}

	private void setupEpochAndTimes() {
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		calendar.setTimeInMillis(mTimeNow);
		truncateHour(calendar);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		long epoch = calendar.getTimeInMillis();
		calendar.setTimeInMillis(epoch);
		mTimeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, mNumHours);
		mTimeTo = calendar.getTimeInMillis();

		SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.US);
		sdf.setTimeZone(mAfLocationInfo.buildTimeZone());

		Log.d(TAG, "New graph time range. From: " + sdf.format(new Date(mTimeFrom)) + ", To: " + sdf.format(new Date(mTimeTo)));
	}

	private void setupSampleTimes() throws AfWidgetDataException {
		long sampleResolutionHrs = 2;
		Log.d(TAG, "Forcing sample resolution to " + sampleResolutionHrs + "hrs for correct icon and label layout.");

		if ((sampleResolutionHrs < 1) || (sampleResolutionHrs > mNumHours / 2)) {
			throw new AfWidgetDataException("Invalid sample resolution");
		}

		mNumHoursBetweenSamples = (int)sampleResolutionHrs;
	}

	private void setupTimesAndPointData() {
		// Set up time variables
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		mTimeNow = calendar.getTimeInMillis();
		truncateHour(calendar);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.add(Calendar.HOUR_OF_DAY, -mNumWeatherDataBufferHours);
		mTimeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, mNumWeatherDataBufferHours * 2 + mNumHours);
		mTimeTo = calendar.getTimeInMillis();
		
		// Get temperature values
		ArrayList<PointData> pointData = new ArrayList<>();

		boolean isFahrenheit = mWidgetSettings.useFahrenheit();
		
		Cursor cursor = null;
		
		try {
			final Uri uri = mAfLocationInfo.getLocationUri().buildUpon()
					.appendPath(AfLocations.TWIG_POINTDATAFORECASTS)
					.appendQueryParameter("start", Long.toString(mTimeFrom))
					.appendQueryParameter("end", Long.toString(mTimeTo)).build();

			cursor = mResolver.query(uri, null, null, null, null);
			
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						PointData p = PointData.buildFromCursor(cursor);
						
						if (p.time != null && p.timeAdded != null && p.temperature != null)
						{
							if (isFahrenheit) p.temperature = p.temperature * 9.0f / 5.0f + 32.0f;
							pointData.add(p);
						}
					} while (cursor.moveToNext());
				}
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}

		mPointData = pointData;
	}
	
	private void setupWidgetDimensions(int width, int height) {
		mWidgetWidth = width;
		mWidgetHeight = height;
		
		boolean isWidgetWidthOdd = mWidgetWidth % 2 == 1;
		boolean isWidgetHeightOdd = mWidgetHeight % 2 == 1;
		
		mWidgetBounds.set(
				isWidgetWidthOdd ? 1 : 0,
				isWidgetHeightOdd ? 1 : 0,
				mWidgetWidth, mWidgetHeight);
	}
	
	private void validatePointData() throws AfWidgetDataException {
		float maxTemperature = Float.NEGATIVE_INFINITY;
		float minTemperature = Float.POSITIVE_INFINITY;
		
		int validPointDataSamples = 0;
		// Find maximum, minimum and range of temperatures
		for (PointData p : mPointData) {
			// Check if temperature is within viewable range
			if (p.temperature != null && p.time >= mTimeFrom && p.time <= mTimeTo)
			{
				if (p.temperature > maxTemperature) maxTemperature = p.temperature;
				if (p.temperature < minTemperature) minTemperature = p.temperature;
				validPointDataSamples++;
			}
		}
		
		// Ensure that there are enough point data samples within the time period
		if (validPointDataSamples < 2) {
			throw new AfWidgetDataException("Too few temperature samples");
		}
		
		mTemperatureValueMax = maxTemperature;
		mTemperatureValueMin = minTemperature;
	}
	
}
