package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import org.votingsystem.AppVS;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.OperationVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.Wallet;

import java.util.Arrays;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVSService extends IntentService {

    public static final String TAG = OperationVSService.class.getSimpleName();

    private Handler mHandler;

    public OperationVSService() {
        super(TAG);
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        final OperationVS operation = (OperationVS) arguments.getSerializable(ContextVS.OPERATIONVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        if(operation.getState() != OperationVS.State.PENDING) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    Toast.makeText(AppVS.getInstance(), "Noting to check - OperationVS state: " +
                            operation.getState(), Toast.LENGTH_LONG).show();
                }
            });
        }
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation);
            switch(operation.getTypeVS()) {
                case CURRENCY_SEND:
                case CURRENCY_CHANGE:
                    checkCurrencyOperation(operation, serviceCaller);
                    break;
            }
        } catch(Exception ex) {
            AppVS.getInstance().broadcastResponse(ResponseVS.EXCEPTION(ex, AppVS.getInstance())
                    .setServiceCaller(serviceCaller).setTypeVS(operation.getTypeVS()));
            ex.printStackTrace();
        }
    }

    private void checkCurrencyOperation(OperationVS operation, String serviceCaller) throws Exception {
        CurrencyBatchDto requestDto = (CurrencyBatchDto) operation.getData();
        ResponseVS responseVS = null;
        if(requestDto.getLeftOverCurrency() != null) {
            Currency currency = Wallet.getCurrency(requestDto.getLeftOverCurrency()
                    .getHashCertVS());
            if(currency != null) {
                responseVS = ResponseVS.OK().setServiceCaller(serviceCaller)
                        .setMessage("Leftover already in wallet");
            } else {
                CurrencyStateDto currencyStateDto = HttpHelper.getData(CurrencyStateDto.class,
                        AppVS.getInstance().getCurrencyServer().getCurrencyStateServiceURL(
                                requestDto.getLeftOverCurrency().getHashCertVS()), MediaTypeVS.JSON);
                if(Currency.State.OK == currencyStateDto.getState()) {
                    currency = requestDto.getLeftOverCurrency();
                    currency.initSigner(currencyStateDto.getCurrencyCert().getBytes());
                    Wallet.updateWallet(Arrays.asList(currency));
                    responseVS = ResponseVS.OK().setServiceCaller(serviceCaller)
                            .setMessage("wallet updated with: " + currency.getAmount() + " " +
                                    currency.getCurrencyCode() + " - " + currency.getTag());
                }
            }
        } else responseVS = ResponseVS.OK().setServiceCaller(serviceCaller)
                .setMessage("operation without left over currency");
        AppVS.getInstance().broadcastResponse(responseVS.setTypeVS(operation.getTypeVS()));
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }

}