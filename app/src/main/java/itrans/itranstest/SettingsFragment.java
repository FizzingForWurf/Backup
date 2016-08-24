package itrans.itranstest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener{

    private static final String RINGTONE_PICKER_PREFERENCE_KEY = "ringtoneSelectPrefs";
    private static final int TONE_PICKER = 100;
    private Uri defaultTone;
    private Uri currentTone;
    private Preference myPrefs;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        defaultTone = RingtoneManager.getActualDefaultRingtoneUri(getActivity(), RingtoneManager.TYPE_ALARM);
        myPrefs = findPreference(RINGTONE_PICKER_PREFERENCE_KEY);
        myPrefs.setOnPreferenceClickListener(this);
        String uriRingToneString = prefs.getString("selectedRingTone", defaultTone.toString());
        Uri uri = Uri.parse(uriRingToneString);
        currentTone = uri;
        Ringtone ring = RingtoneManager.getRingtone(getActivity(), uri);
        myPrefs.setSummary(ring.getTitle(getActivity()));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case RINGTONE_PICKER_PREFERENCE_KEY: {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                startActivityForResult(intent, TONE_PICKER);
            }
            break;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TONE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri selectedRingToneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                Ringtone ringTone = RingtoneManager.getRingtone(getActivity(), selectedRingToneUri);
                if (selectedRingToneUri != null) {
                    String NameOfRingTone = ringTone.getTitle(getActivity());
                    myPrefs.setSummary(NameOfRingTone);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("selectedRingTone", selectedRingToneUri.toString());
                    editor.commit();
                }
            }
        }
    }
}
