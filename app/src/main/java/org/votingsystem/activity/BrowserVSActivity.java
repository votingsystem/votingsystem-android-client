package org.votingsystem.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.util.HashMap;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSActivity extends ActionBarActivity {
	
	public static final String TAG = BrowserVSActivity.class.getSimpleName();

    private String viewerURL = null;
    private TypeVS operationType;
    private AppVS appVS = null;
    private String broadCastId = BrowserVSActivity.class.getSimpleName();
    private WebView webView;
    private OperationVS operationVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(responseVS.getOperation() != null) {
            if(ContentTypeVS.JSON == responseVS.getContentType()) {
                sendMessageToBrowserApp(responseVS.getMessage(),
                        responseVS.getOperation().getCallerCallback());
            } else {
                sendMessageToBrowserApp(responseVS.getStatusCode(),
                        responseVS.getNotificationMessage(), responseVS.getOperation().getCallerCallback());
            }
        } else MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                responseVS.getNotificationMessage(), getSupportFragmentManager());
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        viewerURL = getIntent().getStringExtra(ContextVS.URL_KEY);
        setContentView(R.layout.browservs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        if(savedInstanceState != null) {
            operationType = (TypeVS) savedInstanceState.getSerializable(ContextVS.TYPEVS_KEY);
        }
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        loadUrl(viewerURL, getIntent().getStringExtra(ContextVS.JS_COMMAND_KEY));
    }

    private void loadUrl(String viewerURL, final String jsCommand) {
        LOGD(TAG + ".viewerURL", "viewerURL: " + viewerURL);
        webView = (WebView) findViewById(R.id.browservs_content);
        WebSettings webSettings = webView.getSettings();
        setProgressDialogVisible(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webView.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "clientTool");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if(jsCommand != null) webView.loadUrl(jsCommand);
                setProgressDialogVisible(false);
            }
        });
        webView.loadUrl(viewerURL);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @JavascriptInterface public void setMessage (String appMessage) {
        LOGD(TAG + ".setMessage", "appMessage: " + appMessage);
        try {
            operationVS = JSON.readValue(appMessage, OperationVS.class);
            switch(operationVS.getTypeVS()) {
                default:
                    processSignatureOperation(operationVS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToWebSocketService(TypeVS messageTypeVS, String message) {
        LOGD(TAG + ".sendMessageToWebSocketService", "messageTypeVS: " + messageTypeVS.toString());
        Intent startIntent = new Intent(appVS, WebSocketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, messageTypeVS);
        startIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        runOnUiThread(new Runnable() { @Override public void run() { setProgressDialogVisible(true); } });
        startService(startIntent);
    }

    @Override public void onBackPressed() {
        String webUrl = webView.getUrl();
        LOGD(TAG + ".onBackPressed", "webUrl: " + webUrl);
        if (webView.isFocused() && webView.canGoBack() && webUrl.contains("mode=details")) {
            webView.goBack();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    private void processSignatureOperation(OperationVS operationVS) {
        LOGD(TAG + ".processSignatureOperation", "processSignatureOperation");
        PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                getString(R.string.enter_signature_pin_msg), false, null);
    }

    private void sendResult(int result, String message) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        setResult(result, resultIntent);
        finish();
    }

    public void sendMessageToBrowserApp(int statusCode, String message, String callbackFunction)  {
        LOGD(TAG + ".sendMessageToBrowserApp", "statusCode: " + statusCode + " - message: " +
                message + " - callbackFunction: " + callbackFunction);
        try {
            Map resultMap = new HashMap();
            resultMap.put("statusCode", statusCode);
            resultMap.put("message", message);
            String jsCommand = "javascript:" + callbackFunction + "(" +
                    JSON.writeValueAsString(resultMap) + ")";
            webView.loadUrl(jsCommand);
            setProgressDialogVisible(false);
        } catch (Exception ex) { ex.printStackTrace(); }
    }


    public void sendMessageToBrowserApp(String message, String callbackFunction) {
        LOGD(TAG + ".sendMessageToBrowserApp", "statusCode: " + message +
                " - callbackFunction: " + callbackFunction);
        String jsCommand = "javascript:" + callbackFunction + "(" + message + ")";
        webView.loadUrl(jsCommand);
        setProgressDialogVisible(false);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.URL_KEY, viewerURL);
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}