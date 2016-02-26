package org.votingsystem.activity;

import android.os.Bundle;

import org.votingsystem.android.R;
import org.votingsystem.fragment.CurrencyAccountsPagerFragment;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyMainActivity extends ActivityBase {

    public static final String TAG = CurrencyMainActivity.class.getSimpleName();

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new CurrencyAccountsPagerFragment(), CurrencyAccountsPagerFragment.TAG).commit();
        }
        setMenu(R.menu.drawer_currency);
    }

}