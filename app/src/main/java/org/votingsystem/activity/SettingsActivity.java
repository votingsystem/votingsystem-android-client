package org.votingsystem.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.votingsystem.android.R;
import org.votingsystem.fragment.CANDialogFragment;
import org.votingsystem.fragment.UserDataFormFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HelpUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final int PATTERN_LOCK = 0;

    private WeakReference<SettingsFragment> settingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefUtils.registerPreferenceChangeListener(this);
        // Display the fragment as the main content.
        settingsRef = new WeakReference<>(new SettingsFragment());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsRef.get()).commit();
    }

    @Override protected void onResume() {
        super.onResume();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(this);
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, ".onSharedPreferenceChanged- key: " + key);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        settingsRef.get().onActivityResult(requestCode, resultCode, data);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private String broadCastId = SettingsFragment.class.getSimpleName();
        private SwitchPreference dnie_switch;
        private Preference changePinButton;
        private SwitchPreference patternLockSwitch;

        private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
                String CAN = null;
                if((CAN = intent.getStringExtra(ContextVS.CAN_KEY)) != null) {
                    LOGD(TAG + ".broadcastReceiver", "CAN:" + CAN);
                    PrefUtils.putDNIeCAN(CAN);
                    dnie_switch.setSummary("CAN: " + CAN);
                    setLockPattern(null);
                } else {
                    dnie_switch.setChecked(false);
                    dnie_switch.setSummary(getString(R.string.pref_description_dnie));
                }
                PrefUtils.putDNIeEnabled(dnie_switch.isChecked());
            }
        };

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.main_settings);
            setHasOptionsMenu(true);
            dnie_switch = (SwitchPreference) findPreference("dnie_switch");
            dnie_switch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if(dnie_switch.isChecked()) CANDialogFragment.showDialog(
                            broadCastId, getFragmentManager());
                    else {
                        PrefUtils.putDNIeEnabled(dnie_switch.isChecked());
                        dnie_switch.setSummary(getString(R.string.pref_description_dnie));
                    }
                    return true;
                }
            });
            //PreferenceScreen preference_screen = getPreferenceScreen();
            Preference userDataButton = findPreference("userDataButton");
            userDataButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                    intent.putExtra(ContextVS.FRAGMENT_KEY, UserDataFormFragment.class.getName());
                    startActivity(intent);
                    return true;
                }
            });
            Preference aboutButton = findPreference("aboutAppButton");
            aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    HelpUtils.showAbout(getActivity());
                    return true;
                }
            });
            changePinButton = findPreference("changePinButton");
            patternLockSwitch = (SwitchPreference)findPreference("patternLockSwitch");
            patternLockSwitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if(patternLockSwitch.isChecked()){
                        Intent intent = new Intent(getActivity(), PatternLockActivity.class);
                        intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.request_pattern_lock_msg));
                        intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_SET_PATTERN);
                        intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                        getActivity().startActivityForResult(intent, PATTERN_LOCK);
                    } else {
                        PrefUtils.putLockPatter(null);
                    }
                    return true;
                }
            });
            changePinButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), MessageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.PIN_CHANGE);
                    startActivity(intent);
                    return true;
                }
            });
            if(PrefUtils.isDNIeEnabled()) {
                dnie_switch.setSummary("CAN: " + PrefUtils.getDNIeCAN());
                dnie_switch.setChecked(true);
            }
            try {
                if(PrefUtils.getLockPatternHash() != null) {
                    patternLockSwitch.setChecked(true);
                } else patternLockSwitch.setChecked(false);
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        private void setLockPattern(String lockPattern) {
            if(lockPattern == null) {
                patternLockSwitch.setChecked(false);
                PrefUtils.putLockPatter(null);
            } else {
                patternLockSwitch.setChecked(true);
            }
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                           Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.settings_activity, container, false);
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_vs);
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            toolbar.setTitle(R.string.navdrawer_item_settings);
            setHasOptionsMenu(true);
            return rootView;
        }

        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                    resultCode);
            switch (requestCode) {
                case PATTERN_LOCK:
                    if(data == null || Activity.RESULT_OK != resultCode) {
                        patternLockSwitch.setChecked(false);
                        return;
                    } else {
                        ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    }
                    break;
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override public void onResume() {
            super.onResume();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    broadcastReceiver, new IntentFilter(broadCastId));
        }

        @Override public void onPause() {
            super.onPause();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        }
    }

}