/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.philliphsu.clock2.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;

import com.philliphsu.clock2.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;
    private SwitchPreference geolocationEnabled;
    private EditTextPreference manualLocation;

    public SettingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        preferences = getPreferenceScreen().getSharedPreferences();
        geolocationEnabled = (SwitchPreference) findPreference(getString(R.string.pref_geolocation_enabled_key));
        manualLocation = (EditTextPreference) findPreference(getString(R.string.pref_manual_location_key));

        bindPreferenceSummaryToValue(manualLocation);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_temperature_unit_key)));

        // Set ringtone summary
        setSummary(preferences, getString(R.string.key_timer_ringtone));
        findPreference(getString(R.string.key_alarm_volume))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                        am.adjustStreamVolume(
                                AudioManager.STREAM_ALARM,
                                AudioManager.ADJUST_SAME, // no adjustment
                                AudioManager.FLAG_SHOW_UI); // show the volume toast
                        return true;
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(sharedPreferences, key);
    }

    private void setSummary(SharedPreferences prefs, String key) {
        Preference pref = findPreference(key);
        // Setting a ListPreference's summary value to "%s" in XML automatically updates the
        // preference's summary to display the selected value.
        if (pref instanceof RingtonePreference) {
            Uri ringtoneUri = Uri.parse(prefs.getString(key, ""));
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
            pref.setSummary(ringtone.getTitle(getActivity()));
        }

        manualLocation.setEnabled(!geolocationEnabled.isChecked());
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, preferences.getString(preference.getKey(), null));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(stringValue);
        }
        return true;
    }
}
