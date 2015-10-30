package org.votingsystem.activity;

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
import org.votingsystem.dto.AddressVS;
import org.votingsystem.fragment.AddressFormFragment;
import org.votingsystem.fragment.CANDialogFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HelpUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.TypeVS;

import java.util.HashMap;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefUtils.registerPreferenceChangeListener(this);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override protected void onResume() {
        super.onResume();
        Preference addressButton = (Preference)findPreference("addressButton");
        try {
            AddressVS addressVS = PrefUtils.getAddressVS();
            if(addressVS != null) addressButton.setSummary(addressVS.getName());
        } catch (Exception ex) { ex.printStackTrace();}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(this);
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, ".onSharedPreferenceChanged- key: " + key);
    }


    public static class SettingsFragment extends PreferenceFragment {

        private String broadCastId = SettingsFragment.class.getSimpleName();
        private SwitchPreference dnie_switch;

        private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
                String CAN = null;
                if((CAN = intent.getStringExtra(ContextVS.CAN_KEY)) != null) {
                    LOGD(TAG + ".broadcastReceiver", "CAN:" + CAN);
                    if(dnie_switch.isChecked()) {
                        PrefUtils.putDNIeEnabled(dnie_switch.isChecked());
                        PrefUtils.putDNIeCAN(CAN);
                        dnie_switch.setSummary("CAN: " + CAN);
                    }
                } else {
                    if(dnie_switch.isChecked()) {
                        dnie_switch.setChecked(false);
                        dnie_switch.setSummary(getString(R.string.pref_description_dnie));
                    }
                }
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
                    else dnie_switch.setSummary(getString(R.string.pref_description_dnie));
                    return true;
                }
            });

            Preference addressButton = findPreference("addressButton");
            try {
                AddressVS addressVS = PrefUtils.getAddressVS();
                if(addressVS != null) addressButton.setSummary(addressVS.getName());
            } catch (Exception ex) { ex.printStackTrace();}
            addressButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                    intent.putExtra(ContextVS.FRAGMENT_KEY, AddressFormFragment.class.getName());
                    startActivity(intent);
                    return true;
                }
            });

            Preference requestCertButton = findPreference("requestCertButton");
            requestCertButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), CertRequestActivity.class);
                    try {
                        intent.putExtra(ContextVS.OPERATIONVS_KEY, JSON.writeValueAsString(new HashMap<>()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            Preference changePinButton = findPreference("changePinButton");
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