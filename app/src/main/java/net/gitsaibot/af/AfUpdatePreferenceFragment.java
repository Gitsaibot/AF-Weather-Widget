package net.gitsaibot.af;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

public class AfUpdatePreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {

        addPreferencesFromResource(R.xml.update_preferences);

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(getString(R.string.system_settings_title));
    }
}