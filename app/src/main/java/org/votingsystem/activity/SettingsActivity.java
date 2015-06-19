/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.votingsystem.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.votingsystem.android.R;
import org.votingsystem.dto.AddressVS;
import org.votingsystem.fragment.AddressFormFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HelpUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.TypeVS;

import java.util.HashMap;

import static org.votingsystem.util.LogUtils.LOGD;

public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", " - savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_activity);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SettingsActivity.super.onBackPressed();
            }
        });
        toolbar.setTitle(R.string.navdrawer_item_settings);
        PrefUtils.registerPreferenceChangeListener(this);
        Preference addressButton = (Preference)findPreference("addressButton");
        try {
            AddressVS addressVS = PrefUtils.getAddressVS();
            if(addressVS != null) addressButton.setSummary(addressVS.getName());
        } catch (Exception ex) { ex.printStackTrace();}
        addressButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference arg0) {
                Intent intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, AddressFormFragment.class.getName());
                startActivity(intent);
                return true;
            }
        });

        Preference requestCertButton = (Preference)findPreference("requestCertButton");
        requestCertButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference arg0) {
                Intent intent = new Intent(SettingsActivity.this, CertRequestActivity.class);
                try {
                    intent.putExtra(ContextVS.OPERATIONVS_KEY, JSON.writeValueAsString(new HashMap<>()));
                } catch (Exception e) { e.printStackTrace(); }
                startActivity(intent);
                return true;
            }
        });
        Preference aboutButton = (Preference)findPreference("aboutAppButton");
        aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference arg0) {
                HelpUtils.showAbout(SettingsActivity.this);
                return true;
            }
        });
        Preference changePinButton = (Preference)findPreference("changePinButton");
        changePinButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference arg0) {
                Intent intent = new Intent(SettingsActivity.this, MessageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.PIN_CHANGE);
                startActivity(intent);
                return true;
            }
        });

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
}