package org.votingsystem.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.fragment.CurrencyAccountsPagerFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.service.BootStrapService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.util.Properties;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IntentFilterActivity extends AppCompatActivity {
	
	public static final String TAG = IntentFilterActivity.class.getSimpleName();

    private ProgressDialog progressDialog = null;
    private OperationVS operationVS;
    private AppVS appVS;
    private String broadCastId = IntentFilterActivity.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            try {
                if(operationVS != null  && ContextVS.State.WITH_CERTIFICATE == appVS.getState()){
                    if(operationVS.getEventVS() != null) {
                        //We don't pass all eventvs data on uri because content can be very large
                        responseVS = HttpHelper.getData(operationVS.getEventVS().getURL(), null);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            EventVSDto selectedEvent = (EventVSDto) responseVS.getMessage(EventVSDto.class);
                            LOGD(TAG + ".onStartCommand", " _ TODO _ Fetch option selected");
                            operationVS.setEventVS(selectedEvent);
                        }
                    } else if(operationVS.getOperation() == TypeVS.FROM_USERVS) {
                        Intent newIntent = new Intent(getBaseContext(), CurrencyAccountsPagerFragment.class);
                        newIntent.putExtra(ContextVS.OPERATIONVS_KEY, JSON.writeValueAsString(operationVS));
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(newIntent);
                    }
                } else {
                    Intent responseIntent = null;
                    switch (appVS.getState()) {
                        case WITHOUT_CSR:
                            String applicationID = PrefUtils.getApplicationId();
                            LOGD(TAG + ".processOperation(.. ) ", "WITHOUT_CSR - applicationID: " +
                                    applicationID);
                            responseIntent = new Intent(getBaseContext(), CertRequestActivity.class);
                            break;
                        case WITH_CSR:
                            responseIntent = new Intent(getBaseContext(), CertResponseActivity.class);
                            break;
                        case WITH_CERTIFICATE:
                            responseIntent = new Intent(getBaseContext(), EventVSMainActivity.class);
                            break;
                    }
                    if(intent != null) {
                        responseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(responseIntent);
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    };


    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            LOGD(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        showProgressDialog(getString(R.string.connecting_caption),
                getString(R.string.loading_data_msg));
        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(responseVS != null) {
            if(progressDialog != null) progressDialog.dismiss();
            showMessage(responseVS.getStatusCode(), responseVS.getCaption(), responseVS.getMessage());
            return;
        }
        Uri uriData = getIntent().getData();
        if(uriData != null) {
            try {
                Properties props = new Properties();
                props.load(getAssets().open("VotingSystem.properties"));
                String currencyServerURL = props.getProperty(ContextVS.CURRENCY_SERVER_URL);
                String accessControlURL = props.getProperty(ContextVS.ACCESS_CONTROL_URL_KEY);
                Intent startIntent = new Intent(getApplicationContext(), BootStrapService.class);
                startIntent.putExtra(ContextVS.URI_KEY, uriData);
                startIntent.putExtra(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL);
                startIntent.putExtra(ContextVS.CURRENCY_SERVER_URL, currencyServerURL);
                startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                startService(startIntent);
                String operationStr = uriData.getQueryParameter("operation");
                if(operationStr != null) {
                    operationVS = new OperationVS(TypeVS.valueOf(operationStr), uriData);
                } else {
                    String encodedMsg = uriData.getQueryParameter("operationvs");
                    if(encodedMsg != null) {
                        operationVS = JSON.readValue(
                                Base64.decode(encodedMsg.getBytes(), Base64.NO_WRAP), OperationVS.class);
                    }
                }
            } catch(Exception ex) {ex.printStackTrace();}
        } else {
            Intent intent = new Intent(this, EventVSMainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showProgressDialog(String title, String dialogMessage) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(title);
        progressDialog.setMessage(dialogMessage);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    };

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}