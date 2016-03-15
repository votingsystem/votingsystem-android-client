package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
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
        final String accessControlURL = arguments.getString(ContextVS.ACCESS_CONTROL_URL_KEY);
        final String currencyServerURL = arguments.getString(ContextVS.CURRENCY_SERVER_URL);
        LOGD(TAG + ".onHandleIntent", "accessControlURL: " + accessControlURL +
                " - currencyServerURL: " + currencyServerURL);
        ResponseVS responseVS = null;
        if(!PrefUtils.isDataBootstrapDone()) { }
        if(appVS.getCurrencyServer() == null) {
            responseVS = HttpHelper.getData(ActorDto.getServerInfoURL(currencyServerURL),
                    ContentType.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    CurrencyServerDto currencyServer = (CurrencyServerDto) responseVS.getMessage(
                            CurrencyServerDto.class);
                    appVS.setCurrencyServerDto(currencyServer);
                    Intent startIntent = new Intent(this, PaymentService.class);
                    startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CURRENCY_ACCOUNTS_INFO);
                    startService(startIntent);
                } catch(Exception ex) {ex.printStackTrace();}
            } else {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(appVS, getString(R.string.server_connection_error_msg,
                                currencyServerURL), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        PrefUtils.markDataBootstrapDone();
        if(responseVS == null) responseVS = new ResponseVS();
        responseVS.setServiceCaller(serviceCaller);
        appVS.broadcastResponse(responseVS);
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}