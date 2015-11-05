package org.votingsystem.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

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
 *
 * http://www.devahead.com/blog/2012/01/preserving-the-state-of-an-android-webview-on-screen-orientation-change/
 */
public class BrowserVSActivity extends AppCompatActivity {
	
	public static final String TAG = BrowserVSActivity.class.getSimpleName();

    private String viewerURL;
    private String jsCommand;
    private AppVS appVS = null;
    private String broadCastId = BrowserVSActivity.class.getSimpleName();
    private WebView webView;
    private FrameLayout webViewPlaceholder;
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
        jsCommand = getIntent().getStringExtra(ContextVS.JS_COMMAND_KEY);
        setContentView(R.layout.browservs);
        if(savedInstanceState != null) {
            operationVS = (OperationVS) savedInstanceState.getSerializable(ContextVS.OPERATIONVS_KEY);
        }
        /*if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/
        initUI();
    }

    protected void initUI() {
        webViewPlaceholder = ((FrameLayout)findViewById(R.id.webViewPlaceholder));
        if (webView == null) {
            setProgressDialogVisible(true);
            webView = new WebView(this);
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.setClickable(true);
            webView.getSettings().setSupportZoom(true);
            webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            webView.setScrollbarFadingEnabled(true);
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(this, "clientTool");
            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webView.getSettings().setDomStorageEnabled(true);
            // Load the URLs inside the WebView, not in the external web browser
            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    webView.loadUrl("javascript:setClientToolConnected()");
                    if (jsCommand != null) webView.loadUrl(jsCommand);
                    setProgressDialogVisible(false);
                }
            });
            webView.loadUrl(viewerURL);
        }
        // Attach the WebView to its placeholder
        webViewPlaceholder.addView(webView);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            // Remove the WebView from the old placeholder
            webViewPlaceholder.removeView(webView);
        }
        super.onConfigurationChanged(newConfig);
        // Load the layout resource for the new configuration
        setContentView(R.layout.browservs);
        initUI();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the state of the WebView
        webView.saveState(outState);
        outState.putSerializable(ContextVS.OPERATIONVS_KEY, operationVS);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the state of the WebView
        webView.restoreState(savedInstanceState);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.wait_msg), getSupportFragmentManager());
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressDialogVisible(true);
            }
        });
        startService(startIntent);
    }


    boolean doubleBackToExitPressedOnce = false;
    boolean showBrowserAdvice = true;

    @Override
    public void onBackPressed() {
        String webUrl = webView.getUrl();
        LOGD(TAG + ".onBackPressed", "webUrl: " + webUrl);
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        } else webView.loadUrl("javascript:app.back()");
        this.doubleBackToExitPressedOnce = true;
        if(showBrowserAdvice) {
            Snackbar.make(webViewPlaceholder, getString(R.string.double_back_advice), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.no_more_advice), new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        showBrowserAdvice = false;
                    }
                }).show();
        }
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 700);
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

}