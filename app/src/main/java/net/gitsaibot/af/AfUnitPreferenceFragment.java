package net.gitsaibot.af;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class AfUnitPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference mPrecipitationScalingPref;
    private ListPreference mTemperatureUnitPref;
    private Preference mPrecipitationUnitPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {

        addPreferencesFromResource(R.xml.unit_preferences);

        mTemperatureUnitPref = findPreference(getString(R.string.temperature_units_string));
        mPrecipitationUnitPref = findPreference(getString(R.string.precipitation_units_string));
        mPrecipitationScalingPref = findPreference(getString(R.string.precipitation_scaling_string));


        mTemperatureUnitPref.setOnPreferenceChangeListener(this);
        mPrecipitationUnitPref.setOnPreferenceChangeListener(this);
        mPrecipitationScalingPref.setOnPreferenceChangeListener(this);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String scalePref = pref.getString(getString(R.string.precipitation_scaling_string), "1");
        mPrecipitationScalingPref.setSummary(scalePref);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(getString(R.string.unit_settings_title));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTemperatureUnitPref) {

            return true;

        } else if (preference == mPrecipitationUnitPref) {

            return onPrecipitationUnitPreferenceChange(preference, newValue);

        } else if (preference == mPrecipitationScalingPref) {

            return onFloatPreferenceChange(preference, newValue, 0.000001f, 100.0f,
                    R.string.precipitation_units_invalid_number_toast,
                    R.string.precipitation_units_invalid_range_toast);

        }

        return false;
    }

    private boolean onPrecipitationUnitPreferenceChange(
            Preference preference, Object newValue)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        String oldUnit = settings.getString(getString(R.string.precipitation_units_string), "1");
        String newUnit = (String)newValue;

        if (!oldUnit.equals(newUnit)) {
            // When changing the precipitation unit, reset the scaling to default value
            String defaultScalingValue =
                    newUnit.equals("1")
                            ? getString(R.string.precipitation_scaling_mm_default)
                            : getString(R.string.precipitation_scaling_inches_default);

            mPrecipitationScalingPref.setSummary(
                    defaultScalingValue + " " + (newUnit.equals("1") ? "mm" : "in"));
            mPrecipitationScalingPref.setText(defaultScalingValue);
        }

        return true;
    }

    private boolean onFloatPreferenceChange(Preference preference, Object newValue,
                                            float rangeMin, float rangeMax, int invalidNumberString, int invalidRangeString)
    {
        float f;

        try {
            f = Float.parseFloat((String)newValue);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), getString(invalidNumberString), Toast.LENGTH_SHORT).show();
            return false;
        }

        if ((f >= rangeMin) && (f <= rangeMax)) {
            mPrecipitationScalingPref.setSummary(newValue.toString());
            return true;
        } else {
            Toast.makeText(getContext(), getString(invalidRangeString), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

}
