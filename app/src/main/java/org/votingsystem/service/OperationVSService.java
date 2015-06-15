package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.widget.Toast;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

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
        CurrencyBatchDto requestDto = null;
        if(operation.getState() == OperationVS.State.ERROR || operation.getState() ==
                OperationVS.State.FINISHED) {
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
                    requestDto = (CurrencyBatchDto) operation.getData();
                    if(requestDto.getLeftOver() == null) {

                    }
                    break;
                case CURRENCY_CHANGE:
                    requestDto = (CurrencyBatchDto) operation.getData();
                    break;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
        }
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}