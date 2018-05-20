package org.votingsystem.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.util.HelpUtils;
import org.votingsystem.util.PrefUtils;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final int RC_USER_DATA = 0;
    public static final int RC_SELECT_ACCESS_MODE = 1;

    private WeakReference<SettingsFragment> settingsRef;

    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        settingsRef = new WeakReference<>(new SettingsFragment());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsRef.get()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.navdrawer_item_settings);
        }
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        settingsRef.get().onActivityResult(requestCode, resultCode, data);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private Preference dnieButton;
        private Preference cryptoAccessButton;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            setHasOptionsMenu(true);
            dnieButton = findPreference("dnieButton");
            dnieButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), DNIeCANFormActivity.class);
                    getActivity().startActivityForResult(intent, RC_USER_DATA);
                    return true;
                }
            });
            //PreferenceScreen preference_screen = getPreferenceScreen();
            Preference aboutButton = findPreference("aboutAppButton");
            aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    HelpUtils.showAbout(getActivity());
                    return true;
                }
            });
            cryptoAccessButton = findPreference("cryptoAccessButton");
            cryptoAccessButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), CryptoDeviceAccessModeSelectorActivity.class);
                    getActivity().startActivityForResult(intent, RC_SELECT_ACCESS_MODE);
                    return true;
                }
            });
            updateView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.settings_activity, container, false);
            return rootView;
        }

        private void updateView() {
            CryptoDeviceAccessMode passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
            if (passwAccessMode != null) {
                switch (passwAccessMode.getMode()) {
                    case PATTER_LOCK:
                        cryptoAccessButton.setSummary(getString(R.string.pattern_lock_lbl));
                        break;
                    case PIN:
                        cryptoAccessButton.setSummary(getString(R.string.pin_lbl));
                        break;
                    case DNIE_PASSW:
                        cryptoAccessButton.setSummary(getString(R.string.id_card_passw_lbl));
                        break;
                }
            } else cryptoAccessButton.setSummary(getString(R.string.id_card_passw_lbl));
            if (PrefUtils.isDNIeEnabled()) {
                dnieButton.setSummary("CAN: " + PrefUtils.getDNIeCAN());
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
            updateView();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

}