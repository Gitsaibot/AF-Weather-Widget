package net.gitsaibot.af;

import java.util.Locale;
import java.util.Objects;

import net.gitsaibot.af.util.AfWidgetInfo;

import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

public class AfDeviceProfileActivity extends AppCompatActivity
		implements OnCheckedChangeListener,
				   OnClickListener,
				   OnItemSelectedListener,
				   OnKeyListener
{
	private static final String TAG = "AfDeviceProfileActivity";
	
	private int mNumColumns;
	private int mNumRows;
	
	private int mInvalidEditTextColor;
	
	private boolean mValidPortraitWidth;
	private boolean mValidPortraitHeight;
	private boolean mValidLandscapeWidth;
	private boolean mValidLandscapeHeight;
	
	private AfSettings mAfSettings = null;
	private AfWidgetInfo mAfWidgetInfo = null;
	
	/* UI Elements Begin */

	private TextView mModelTextView;
	private Spinner mOrientationModeSpinner;
	private CheckBox mActivateSpecificDimensionsCheckBox;
	
	private LinearLayout mFocusLayout;
	
	private TextView mPortraitDimensionsLabel;
	private TextView mPortraitWidthLabel, mPortraitHeightLabel;
	private EditText mPortraitWidthEditText, mPortraitHeightEditText;
	private ImageButton mPortraitRevertButton;
	private Button mPortraitCalibrateButton;

	private TextView mLandscapeDimensionsLabel;
	private TextView mLandscapeWidthLabel, mLandscapeHeightLabel;
	private EditText mLandscapeWidthEditText, mLandscapeHeightEditText;
	private ImageButton mLandscapeRevertButton;
	private Button mLandscapeCalibrateButton;

	
	/* UI Elements End */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setResult(RESULT_CANCELED);
		
		Uri widgetUri = getIntent().getData();
		if (widgetUri == null)
		{
			Toast.makeText(this, "Error: Widget URI was null!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Killed AfDeviceProfileActivity: Widget URI was null!");
			finish();
			return;
		}
		
		try {
			mAfWidgetInfo = AfWidgetInfo.build(this, widgetUri);
		} catch (Exception e) {
			Toast.makeText(this, "Error: Failed to get widget information!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Killed AfDeviceProfileActivity: Failed to get widget information! (uri=" + widgetUri +")");
			e.printStackTrace();
			finish();
			return;
		}
		
		mNumColumns = mAfWidgetInfo.getNumColumns();
		mNumRows = mAfWidgetInfo.getNumRows();
		
		mAfSettings = AfSettings.build(this, mAfWidgetInfo);
		
		setContentView(R.layout.activity_device_profiles);
		Objects.requireNonNull(getSupportActionBar()).show();
		setupUIElements();
		initialize();
		updateUIState();
	}

	private void initialize()
	{
		TextView tv = findViewById(R.id.af_device_profile_guide);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(HtmlCompat.fromHtml(getString(R.string.device_profile_guide_link), HtmlCompat.FROM_HTML_MODE_LEGACY));
		
		mInvalidEditTextColor = ContextCompat.getColor(this, R.color.invalid_dimension_color);
		
		mModelTextView.setText(String.format(Locale.US, "%s (%s)", Build.MODEL, Build.PRODUCT));
		
		mValidPortraitWidth = true;
		mValidPortraitHeight = true;
		mValidLandscapeWidth = true;
		mValidLandscapeHeight = true;
		
		// Use Device Specific Dimension Property
		boolean useDeviceSpecificDimensions = mAfSettings.getUseDeviceProfilePreference();
		mActivateSpecificDimensionsCheckBox.setChecked(useDeviceSpecificDimensions);
		mActivateSpecificDimensionsCheckBox.setOnCheckedChangeListener(this);
		
		// Orientation Mode Property
		int orientationMode = mAfSettings.getOrientationModePreference();
		mOrientationModeSpinner.setSelection(orientationMode);
		mOrientationModeSpinner.setOnItemSelectedListener(this);
		
		mPortraitWidthEditText.setOnKeyListener(this);
		mPortraitHeightEditText.setOnKeyListener(this);
		mPortraitRevertButton.setOnClickListener(this);
		mPortraitCalibrateButton.setOnClickListener(this);
		
		mLandscapeWidthEditText.setOnKeyListener(this);
		mLandscapeHeightEditText.setOnKeyListener(this);
		mLandscapeRevertButton.setOnClickListener(this);
		mLandscapeCalibrateButton.setOnClickListener(this);
	}
	
	private void setupUIElements() {
		mModelTextView = findViewById(R.id.af_device_profile_model);
		mOrientationModeSpinner = findViewById(R.id.af_device_profile_orientation);
	
		mFocusLayout = findViewById(R.id.focusLayout);
		
		mActivateSpecificDimensionsCheckBox = findViewById(R.id.checkbox_activate_specific_dimensions);
		
		mPortraitDimensionsLabel = findViewById(R.id.portrait_dimensions_label);
		mPortraitWidthLabel = findViewById(R.id.portrait_width_label);
		mPortraitHeightLabel = findViewById(R.id.portrait_height_label);
		
		mPortraitWidthEditText = findViewById(R.id.af_device_profile_portrait_width);
		mPortraitHeightEditText = findViewById(R.id.af_device_profile_portrait_height);
		mPortraitRevertButton = findViewById(R.id.adp_revert_portrait_button);
		mPortraitCalibrateButton = findViewById(R.id.button_calibrate_portrait);
		
		mLandscapeDimensionsLabel = findViewById(R.id.landscape_dimensions_label);
		mLandscapeWidthLabel = findViewById(R.id.landscape_width_label);
		mLandscapeHeightLabel = findViewById(R.id.landscape_height_label);
		
		mLandscapeWidthEditText = findViewById(R.id.af_device_profile_landscape_width);
		mLandscapeHeightEditText = findViewById(R.id.af_device_profile_landscape_height);
		mLandscapeRevertButton = findViewById(R.id.adp_revert_landscape_button);
		mLandscapeCalibrateButton = findViewById(R.id.button_calibrate_landscape);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView == mActivateSpecificDimensionsCheckBox) {
			mAfSettings.setUseDeviceProfilePreference(isChecked);
			updateUIState();
		}
	}
	
	@Override
	public void onClick(View view) {
		if (view == mPortraitRevertButton) {
			mAfSettings.revertPixelDimensionsPreference(mNumColumns, mNumRows, false);
			updateUIState();
		} else if (view == mLandscapeRevertButton) {
			mAfSettings.revertPixelDimensionsPreference(mNumColumns, mNumRows, true);
			updateUIState();
		} else if (view == mPortraitCalibrateButton || view == mLandscapeCalibrateButton) {
			startCalibration();
		}
	}

	@Override
	public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
		if (parent == mOrientationModeSpinner) {
			if (position >= 0 && position <= 2) {
				mAfSettings.setOrientationModePreference(position);
			}
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) { }
	
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// Avoid evaluating function twice on key events.
		// Handle up + multiple action events.
		if (event.getAction() != KeyEvent.ACTION_DOWN)
		{
			if (v == mPortraitWidthEditText || v == mPortraitHeightEditText) {
				final String widthString = mPortraitWidthEditText.getText().toString();
				final String heightString = mPortraitHeightEditText.getText().toString();
				
				int portraitWidth = mAfSettings.validateStringValue(widthString);
				int portraitHeight = mAfSettings.validateStringValue(heightString);
	
				mValidPortraitWidth = (portraitWidth != -1);
				mValidPortraitHeight = (portraitHeight != -1);
				
				if (mValidPortraitWidth && mValidPortraitHeight)
				{
					mAfSettings.storePixelDimensionsPreference(mNumColumns, mNumRows, false, new Point(portraitWidth, portraitHeight));
				}
				
				updateUIState(false);
			} else if (v == mLandscapeWidthEditText || v == mLandscapeHeightEditText) {
				final String widthString = mLandscapeWidthEditText.getText().toString();
				final String heightString = mLandscapeHeightEditText.getText().toString();
				
				int landscapeWidth = mAfSettings.validateStringValue(widthString);
				int landscapeHeight = mAfSettings.validateStringValue(heightString);
				
				mValidLandscapeWidth = (landscapeWidth != -1);
				mValidLandscapeHeight = (landscapeHeight != -1);
				
				if (mValidLandscapeWidth && mValidLandscapeHeight)
				{
					mAfSettings.storePixelDimensionsPreference(mNumColumns, mNumRows, true, new Point(landscapeWidth, landscapeHeight));
				}
				
				updateUIState(false);
			}
		}
		
		// Do not consume event, or else text will not change.
		return false;
	}

	private void startCalibration() {
		setResult(AfPreferenceFragment.EXIT_CONFIGURATION);
		finish();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mFocusLayout.requestFocus();
	}

	private void updateEditTextBackgroundColors()
	{
		if (mValidPortraitWidth)
		{
			mPortraitWidthEditText.getBackground().clearColorFilter();
		}
		else
		{
			mPortraitWidthEditText.getBackground().setColorFilter(new PorterDuffColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY));
		}
		if (mValidPortraitHeight)
		{
			mPortraitHeightEditText.getBackground().clearColorFilter();
		}
		else
		{
			mPortraitHeightEditText.getBackground().setColorFilter(new PorterDuffColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY));
		}
		if (mValidLandscapeWidth)
		{
			mLandscapeWidthEditText.getBackground().clearColorFilter();
		}
		else
		{
			mLandscapeWidthEditText.getBackground().setColorFilter(new PorterDuffColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY));
		}
		if (mValidLandscapeHeight)
		{
			mLandscapeHeightEditText.getBackground().clearColorFilter();
		}
		else
		{
			mLandscapeHeightEditText.getBackground().setColorFilter(new PorterDuffColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY));
		}
	}
	
	private void updateUIState() {
		updateUIState(true);
	}
	
	private void updateUIState(boolean updateEditTextBoxes)
	{
		boolean activateSpecificDimensions = mActivateSpecificDimensionsCheckBox.isChecked();

		mPortraitDimensionsLabel.setEnabled(activateSpecificDimensions);
		mPortraitWidthLabel.setEnabled(activateSpecificDimensions);
		mPortraitHeightLabel.setEnabled(activateSpecificDimensions);
		
		mPortraitWidthEditText.setEnabled(activateSpecificDimensions);
		mPortraitHeightEditText.setEnabled(activateSpecificDimensions);
		mPortraitCalibrateButton.setEnabled(activateSpecificDimensions);
		mPortraitRevertButton.setEnabled(activateSpecificDimensions);
		
		mLandscapeDimensionsLabel.setEnabled(activateSpecificDimensions);
		mLandscapeWidthLabel.setEnabled(activateSpecificDimensions);
		mLandscapeHeightLabel.setEnabled(activateSpecificDimensions);
		
		mLandscapeWidthEditText.setEnabled(activateSpecificDimensions);
		mLandscapeHeightEditText.setEnabled(activateSpecificDimensions);
		mLandscapeCalibrateButton.setEnabled(activateSpecificDimensions);
		mLandscapeRevertButton.setEnabled(activateSpecificDimensions);
		
		if (updateEditTextBoxes) {
			Point portraitDimensions = mAfSettings.getPixelDimensionsPreferenceOrStandard(mNumColumns, mNumRows, false);
			mPortraitWidthEditText.setText(String.format(Locale.US, "%d", portraitDimensions.x));
			mPortraitHeightEditText.setText(String.format(Locale.US, "%d", portraitDimensions.y));
			
			Point landscapeDimensions = mAfSettings.getPixelDimensionsPreferenceOrStandard(mNumColumns, mNumRows, true);
			mLandscapeWidthEditText.setText(String.format(Locale.US, "%d", landscapeDimensions.x));
			mLandscapeHeightEditText.setText(String.format(Locale.US, "%d", landscapeDimensions.y));
		}
		
		updateEditTextBackgroundColors();
	}
}
