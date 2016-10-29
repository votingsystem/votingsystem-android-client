package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import org.votingsystem.App;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.dto.voting.AccessControlDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BootStrapService extends IntentService {

    public static final String TAG = BootStrapService.class.getSimpleName();

    private App app;
    private String serviceCaller;
    private Handler mHandler;

    public BootStrapService() {
        super(TAG);
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        app = (App) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        serviceCaller = arguments.getString(Constants.CALLER_KEY);
        LOGD(TAG + ".onHandleIntent", "accessControlURL: " + app.getAccessControlURL() +
                " - currencyServerURL: " + app.getCurrencyServerURL());
        ResponseDto responseDto = null;
        if(!PrefUtils.isDataBootstrapDone()) { }
        app.getActor(CurrencyServerDto.class, app.getCurrencyServerURL());
        app.getActor(AccessControlDto.class, app.getAccessControlURL());
        Intent startIntent = new Intent(this, PaymentService.class);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.CURRENCY_ACCOUNTS_INFO);
        startService(startIntent);
        /*runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(app, getString(R.string.server_connection_error_msg,
                        app.getCurrencyServerURL()), Toast.LENGTH_LONG).show();
            }
        });*/
        PrefUtils.markDataBootstrapDone();
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}