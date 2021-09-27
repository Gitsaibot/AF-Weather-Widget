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

import net.gitsaibot.af.util.AfLocationInfo;
import net.gitsaibot.af.util.AfWidgetInfo;

public class AfPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "AfPrefFragment";

    private final static int SELECT_LOCATION = 1;
    private final static int DEVICE_PROFILES = 2;

    public final static int EXIT_CONFIGURATION = 77;

    private AfWidgetInfo mAfWidgetInfo = null;
    private AfSettings mAfSettings = null;

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
                mAfWidgetInfo = AfWidgetInfo.build(getContext(), widgetUri);
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

            AfUtils.deleteWidget(getContext(), appWidgetId);

            mAfWidgetInfo = new AfWidgetInfo(appWidgetId, AfProvider.AfWidgets.SIZE_LARGE_TINY, null);

            Log.d(TAG, "Commit=" + mAfWidgetInfo.commit(getContext()));
        }

        Log.d(TAG, "onCreate(): " + mAfWidgetInfo.toString());

        mAfSettings = AfSettings.build(getContext(), mAfWidgetInfo);

        if (mActionEdit) {
            mAfSettings.initializePreferencesExistingWidget();
        } else {
            mAfSettings.initializePreferencesNewWidget();
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

                if (mAfWidgetInfo.getViewInfo() == null || mAfWidgetInfo.getViewInfo().getLocationInfo() == null) {
                    Toast.makeText(mContext, getString(R.string.must_select_location), Toast.LENGTH_SHORT).show();

                }

                Log.d(TAG, "onClick(): " + mAfWidgetInfo.toString());
                mAfWidgetInfo.commit(mContext);
                Log.d(TAG, "onClick(): Committed=" + mAfWidgetInfo.toString());

                boolean isProviderModified = mAfSettings.isProviderPreferenceModified();
                boolean globalSettingModified = mAfSettings.saveAllPreferences(mActivateCalibrationMode);

                PendingIntent configurationIntent = AfUtils.buildConfigurationIntent(mContext, mAfWidgetInfo.getWidgetUri());
                AfUtils.updateWidgetRemoteViews(mContext, mAfWidgetInfo.getAppWidgetId(), getString(R.string.widget_loading), true, configurationIntent);

                Uri widgetUri = mAfWidgetInfo.getWidgetUri();

                if (isProviderModified || globalSettingModified) {
                    AfService.enqueueWork(
                            mContext,
                            new Intent(isProviderModified
                                    ? AfService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
                                    : AfService.ACTION_UPDATE_ALL,
                                    widgetUri, mContext, AfService.class));
                } else {
                    AfService.enqueueWork(
                            mContext,
                            new Intent(AfService.ACTION_UPDATE_WIDGET, widgetUri, mContext, AfService.class));
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAfWidgetInfo.getAppWidgetId());
                getActivity().setResult(Activity.RESULT_OK, resultIntent);

                if(getActivity() != null & !(mAfWidgetInfo.getViewInfo() == null || mAfWidgetInfo.getViewInfo().getLocationInfo() == null)) {
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
                        AfLocationInfo afLocationInfo = AfLocationInfo.build(getActivity().getApplicationContext(), locationUri);
                        mAfWidgetInfo.setViewInfo(afLocationInfo, AfProvider.AfViews.TYPE_DETAILED);
                        Log.d(TAG, "onActivityResult(): locationInfo=" + afLocationInfo);
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
                        int activeCalibrationTarget = mAfSettings.getCalibrationTarget();

                        boolean isProviderModified = mAfSettings.isProviderPreferenceModified();
                        boolean globalSettingModified = mAfSettings.saveAllPreferences(mActivateCalibrationMode);

                        if (activeCalibrationTarget != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            // Redraw the currently active calibration widget
                            AfService.enqueueWork(getContext(), new Intent(
                                    AfService.ACTION_UPDATE_WIDGET,
                                    ContentUris.withAppendedId(AfProvider.AfWidgets.CONTENT_URI, activeCalibrationTarget),
                                    getContext(), AfService.class));
                        }

                        Uri widgetUri = mAfWidgetInfo.getWidgetUri();

                        if (isProviderModified || globalSettingModified) {
                            AfService.enqueueWork(
                                    getActivity().getApplicationContext(),
                                    new Intent(isProviderModified
                                            ? AfService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
                                            : AfService.ACTION_UPDATE_ALL,
                                            widgetUri, getContext(), AfService.class));
                        } else {
                            AfService.enqueueWork(
                                    getActivity().getApplicationContext(),
                                    new Intent(AfService.ACTION_UPDATE_WIDGET,
                                            widgetUri, getContext(), AfService.class));
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
            intent.setData(mAfWidgetInfo.getWidgetUri());
            startActivityForResult(intent, DEVICE_PROFILES);
            return true;

        } else if (preference == mLocationPref) {
            Intent intent = new Intent(getContext(), AfLocationSelectionActivity.class);
            startActivityForResult(intent, SELECT_LOCATION);
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String locationName = null;

        if (mAfWidgetInfo.getViewInfo() != null && mAfWidgetInfo.getViewInfo().getLocationInfo() != null) {
            AfLocationInfo locationInfo = mAfWidgetInfo.getViewInfo().getLocationInfo();
            if (locationInfo != null && locationInfo.getTitle() != null) {
                locationName = locationInfo.getTitle();
            }
        }
        mLocationPref.setSummary(locationName);
    }
}