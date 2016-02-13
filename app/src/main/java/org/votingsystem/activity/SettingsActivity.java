package org.votingsystem.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.fragment.UserDataFormFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HelpUtils;
import org.votingsystem.util.PrefUtils;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();


    public static final int USER_DATA          = 0;
    public static final int SELECT_ACCESS_MODE = 1;

    private WeakReference<SettingsFragment> settingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRef = new WeakReference<>(new SettingsFragment());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsRef.get()).commit();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        settingsRef.get().onActivityResult(requestCode, resultCode, data);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private Preference dnieButton;
        private Preference cryptoAccessButton;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            setHasOptionsMenu(true);
            dnieButton =  findPreference("dnieButton");
            dnieButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                    intent.putExtra(ContextVS.FRAGMENT_KEY, UserDataFormFragment.class.getName());
                    getActivity().startActivityForResult(intent, USER_DATA);
                    return true;
                }
            });
            //PreferenceScreen preference_screen = getPreferenceScreen();
            Preference aboutButton = findPreference("aboutAppButton");
            aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override public boolean onPreferenceClick(Preference arg0) {
                    HelpUtils.showAbout(getActivity());
                    return true;
                }
            });
            cryptoAccessButton = findPreference("cryptoAccessButton");
            cryptoAccessButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), CryptoDeviceAccessModeSelectorActivity.class);
                    getActivity().startActivityForResult(intent, SELECT_ACCESS_MODE);
                    return true;
                }
            });
            if(PrefUtils.isDNIeEnabled()) {
                dnieButton.setSummary("CAN: " + PrefUtils.getDNIeCAN());
            }
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                           Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.settings_activity, container, false);
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_vs);
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            toolbar.setTitle(R.string.navdrawer_item_settings);
            return rootView;
        }

        @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
            LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                    resultCode);
            switch (requestCode) {
                case SELECT_ACCESS_MODE:
                    CryptoDeviceAccessMode passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
                    if(passwAccessMode != null) {
                        switch (passwAccessMode.getMode()) {
                            case PATTER_LOCK:
                                cryptoAccessButton.setSummary(getString(R.string.pattern_lock_lbl));
                                break;
                            case PIN:
                                cryptoAccessButton.setSummary(getString(R.string.pin_lbl));
                                break;
                        }
                    }
                    break;
                case USER_DATA:
                    dnieButton.setSummary("CAN: " + PrefUtils.getDNIeCAN());
                    break;
            }
        }
    }

}