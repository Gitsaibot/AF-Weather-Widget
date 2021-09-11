package net.gitsaibot.af;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class AfUiPreferenceFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private EditTextPreference mBorderThicknessPreference;
    private EditTextPreference mBorderRoundingPreference;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.ui_preferences);

        mBorderThicknessPreference = findPreference(getString(R.string.border_thickness_string));
        mBorderRoundingPreference = findPreference(getString(R.string.border_rounding_string));


        mBorderThicknessPreference.setOnPreferenceChangeListener(this);
        mBorderRoundingPreference.setOnPreferenceChangeListener(this);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String bts = pref.getString(getString(R.string.border_thickness_string), "5");
        mBorderThicknessPreference.setSummary(bts + "px");
        String brs = pref.getString(getString(R.string.border_rounding_string), "4");
        mBorderRoundingPreference.setSummary(brs + "px");

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(getString(R.string.ui_settings_title));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference == mBorderThicknessPreference) {

            mBorderThicknessPreference.setSummary(newValue + "px");
            return true;

        } else if (preference == mBorderRoundingPreference) {

            mBorderRoundingPreference.setSummary(newValue + "px");
            return true;
        }

        return false;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            DialogFragment dialogFragment = ColorPreferenceFragment.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), null);
        } else super.onDisplayPreferenceDialog(preference);
    }
}
