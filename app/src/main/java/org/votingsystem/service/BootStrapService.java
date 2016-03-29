package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import org.votingsystem.AppVS;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.voting.AccessControlDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BootStrapService extends IntentService {

    public static final String TAG = BootStrapService.class.getSimpleName();

    private AppVS appVS;
    private String serviceCaller;
    private Handler mHandler;

    public BootStrapService() {
        super(TAG);
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        LOGD(TAG + ".onHandleIntent", "accessControlURL: " + appVS.getAccessControlURL() +
                " - currencyServerURL: " + appVS.getCurrencyServerURL());
        ResponseVS responseVS = null;
        if(!PrefUtils.isDataBootstrapDone()) { }
        appVS.getActor(CurrencyServerDto.class, appVS.getCurrencyServerURL());
        appVS.getActor(AccessControlDto.class, appVS.getAccessControlURL());
        Intent startIntent = new Intent(this, PaymentService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CURRENCY_ACCOUNTS_INFO);
        startService(startIntent);
        /*runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(appVS, getString(R.string.server_connection_error_msg,
                        appVS.getCurrencyServerURL()), Toast.LENGTH_LONG).show();
            }
        });*/
        PrefUtils.markDataBootstrapDone();
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}