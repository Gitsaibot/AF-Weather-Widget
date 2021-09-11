package net.gitsaibot.af;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.gitsaibot.af.util.AixLocationInfo;
import net.gitsaibot.af.util.AixWidgetInfo;

public class AfPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "AfPrefFragment";

    private final static int SELECT_LOCATION = 1;
    private final static int DEVICE_PROFILES = 2;

    public final static int EXIT_CONFIGURATION = 77;

    private AixWidgetInfo mAixWidgetInfo = null;
    private AixSettings mAixSettings = null;

    private Button mAddWidgetButton;

    private EditTextPreference mBorderThicknessPref;
    private EditTextPreference mBorderRoundingPref;
    private EditTextPreference mPrecipitationScalingPref;

    private Preference mUiPref;
    private Preference mUpdatePref;
    private Preference mUnitPref;
    private Preference mDeviceProfilePref;
    private Preference mLocationPref;
    private Preference mPrecipitationUnitPref;
    private Preference mProviderPref;
    private Preference mTemperatureUnitPref;
    private Preference mTopTextVisibilityPref;
    private Preference mButton;
    //private Preference mUpdateRatePref;

    private boolean mActionEdit = false;
    private boolean mActivateCalibrationMode = false;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.preferences);

        mUnitPref = findPreference("unit_preference");
        mUiPref = findPreference("ui_preference");
        mUpdatePref = findPreference("update_preference");
        mButton = findPreference("Button");

        mBorderThicknessPref = findPreference(getString(R.string.border_thickness_string));
        mBorderRoundingPref = findPreference(getString(R.string.border_rounding_string));
        mPrecipitationScalingPref = findPreference(getString(R.string.precipitation_scaling_string));

        mDeviceProfilePref = findPreference(getString(R.string.device_profiles_key));
        mLocationPref = findPreference(getString(R.string.location_settings_key));
        mPrecipitationUnitPref = findPreference(getString(R.string.precipitation_units_string));
        mProviderPref = findPreference(getString(R.string.preference_provider_string));
        mTemperatureUnitPref = findPreference(getString(R.string.temperature_units_string));
        mTopTextVisibilityPref = findPreference(getString(R.string.top_text_visibility_string));

        setupListeners();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setResult(Activity.RESULT_CANCELED);

        if (getActivity().getIntent() == null || getActivity().getIntent().getAction() == null) {
            Toast.makeText(getContext(), "Could not start configuration activity: Intent or action was null.", Toast.LENGTH_SHORT).show();
            return;
        }

        mActionEdit = getActivity().getIntent().getAction().equals(Intent.ACTION_EDIT);

        if (mActionEdit) {
            Uri widgetUri = getActivity().getIntent().getData();

            if (widgetUri == null) {
                Toast.makeText(getContext(), "Could not start configuration activity: Data was null. Remove and recreate the widget.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                mAixWidgetInfo = AixWidgetInfo.build(getContext(), widgetUri);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to get widget information from database. Try removing the widget and creating a new one.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }
        } else {
            int appWidgetId = getActivity().getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Toast.makeText(getContext(), "Could not start configuration activity: Missing AppWidgetId.", Toast.LENGTH_SHORT).show();
                return;
            }

            AixUtils.deleteWidget(getContext(), appWidgetId);

            mAixWidgetInfo = new AixWidgetInfo(appWidgetId, AixProvider.AixWidgets.SIZE_LARGE_TINY, null);

            Log.d(TAG, "Commit=" + mAixWidgetInfo.commit(getContext()));
        }

        Log.d(TAG, "onCreate(): " + mAixWidgetInfo.toString());

        mAixSettings = AixSettings.build(getContext(), mAixWidgetInfo);

        if (mActionEdit) {
            mAixSettings.initializePreferencesExistingWidget();
        } else {
            mAixSettings.initializePreferencesNewWidget();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

        mAddWidgetButton = new Button(getActivity().getApplicationContext());
        mAddWidgetButton.setText(mActionEdit ? getString(R.string.apply_changes) : getString(R.string.add_widget));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.BOTTOM;
        params.topMargin = 50;

        v.addView(mAddWidgetButton);
        mAddWidgetButton.setLayoutParams(params);
        mAddWidgetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Context mContext = getActivity().getApplicationContext();

                if (mAixWidgetInfo.getViewInfo() == null || mAixWidgetInfo.getViewInfo().getLocationInfo() == null) {
                    Toast.makeText(mContext, getString(R.string.must_select_location), Toast.LENGTH_SHORT).show();

                }

                Log.d(TAG, "onClick(): " + mAixWidgetInfo.toString());
                mAixWidgetInfo.commit(mContext);
                Log.d(TAG, "onClick(): Committed=" + mAixWidgetInfo.toString());

                boolean isProviderModified = mAixSettings.isProviderPreferenceModified();
                boolean globalSettingModified = mAixSettings.saveAllPreferences(mActivateCalibrationMode);

                PendingIntent configurationIntent = AixUtils.buildConfigurationIntent(mContext, mAixWidgetInfo.getWidgetUri());
                AixUtils.updateWidgetRemoteViews(mContext, mAixWidgetInfo.getAppWidgetId(), getString(R.string.widget_loading), true, configurationIntent);

                Uri widgetUri = mAixWidgetInfo.getWidgetUri();

                if (isProviderModified || globalSettingModified) {
                    AixService.enqueueWork(
                            mContext,
                            new Intent(isProviderModified
                                    ? AixService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
                                    : AixService.ACTION_UPDATE_ALL,
                                    widgetUri, mContext, AixService.class));
                } else {
                    AixService.enqueueWork(
                            mContext,
                            new Intent(AixService.ACTION_UPDATE_WIDGET, widgetUri, mContext, AixService.class));
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAixWidgetInfo.getAppWidgetId());
                getActivity().setResult(Activity.RESULT_OK, resultIntent);

                if(getActivity() != null & !(mAixWidgetInfo.getViewInfo() == null || mAixWidgetInfo.getViewInfo().getLocationInfo() == null)) {
                    getActivity().finish();
                }

            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(getString(R.string.title_configure));
    }

    /**
     * Sets up all the preference change listeners to use the specified listener.
     */
    private void setupListeners() {

        mLocationPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_LOCATION: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri locationUri = Uri.parse(data.getStringExtra("location"));
                    try {
                        AixLocationInfo aixLocationInfo = AixLocationInfo.build(getActivity().getApplicationContext(), locationUri);
                        mAixWidgetInfo.setViewInfo(aixLocationInfo, AixProvider.AixViews.TYPE_DETAILED);
                        Log.d(TAG, "onActivityResult(): locationInfo=" + aixLocationInfo);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Failed to set up location info", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onActivityResult(): Failed to get location data.");
                        e.printStackTrace();
                    }
                }
                break;
            }
            case DEVICE_PROFILES: {
                if (resultCode == EXIT_CONFIGURATION) {
                    mActivateCalibrationMode = true;

                    boolean editMode = Intent.ACTION_EDIT.equals(getActivity().getIntent().getAction());

                    if (editMode) {
                        int activeCalibrationTarget = mAixSettings.getCalibrationTarget();

                        boolean isProviderModified = mAixSettings.isProviderPreferenceModified();
                        boolean globalSettingModified = mAixSettings.saveAllPreferences(mActivateCalibrationMode);

                        if (activeCalibrationTarget != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            // Redraw the currently active calibration widget
                            AixService.enqueueWork(getContext(), new Intent(
                                    AixService.ACTION_UPDATE_WIDGET,
                                    ContentUris.withAppendedId(AixProvider.AixWidgets.CONTENT_URI, activeCalibrationTarget),
                                    getContext(), AixService.class));
                        }

                        Uri widgetUri = mAixWidgetInfo.getWidgetUri();

                        if (isProviderModified || globalSettingModified) {
                            AixService.enqueueWork(
                                    getActivity().getApplicationContext(),
                                    new Intent(isProviderModified
                                            ? AixService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
                                            : AixService.ACTION_UPDATE_ALL,
                                            widgetUri, getContext(), AixService.class));
                        } else {
                            AixService.enqueueWork(
                                    getActivity().getApplicationContext(),
                                    new Intent(AixService.ACTION_UPDATE_WIDGET,
                                            widgetUri, getContext(), AixService.class));
                        }

                    }
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        if (preference == mUnitPref) {
            return super.onPreferenceTreeClick(preference);

        } else if (preference == mUiPref) {
            return super.onPreferenceTreeClick(preference);

        } else if (preference == mUpdatePref) {
            return super.onPreferenceTreeClick(preference);

        } else if (preference == mDeviceProfilePref) {
            Intent intent = new Intent(getContext(), AfDeviceProfileActivity.class);
            intent.setAction(getActivity().getIntent().getAction());
            intent.setData(mAixWidgetInfo.getWidgetUri());
            startActivityForResult(intent, DEVICE_PROFILES);
            return true;

        } else if (preference == mLocationPref) {
            Intent intent = new Intent(getContext(), AixLocationSelectionActivity.class);
            startActivityForResult(intent, SELECT_LOCATION);
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String locationName = null;

        if (mAixWidgetInfo.getViewInfo() != null && mAixWidgetInfo.getViewInfo().getLocationInfo() != null) {
            AixLocationInfo locationInfo = mAixWidgetInfo.getViewInfo().getLocationInfo();
            if (locationInfo != null && locationInfo.getTitle() != null) {
                locationName = locationInfo.getTitle();
            }
        }
        mLocationPref.setSummary(locationName);
    }
}