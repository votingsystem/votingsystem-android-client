package org.votingsystem.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
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

    private static final int FLAGS = Base64.NO_WRAP | Base64.URL_SAFE;


    private String viewerURL;
    private String jsCommand;
    private AppVS appVS = null;
    private String broadCastId = BrowserVSActivity.class.getSimpleName();
    private WebView webView;
    private FrameLayout webViewPlaceholder;
    private OperationVS operationVS;
    private boolean doubleBackEnabled = true;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean showBrowserAdvice = true;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(responseVS.getOperation() != null) {
            if(ContentTypeVS.JSON == responseVS.getContentType()) {
                invokeOperationCallback(responseVS.getMessage(),
                        responseVS.getOperation().getCallerCallback());
            } else {
                invokeOperationCallback(responseVS.getStatusCode(),
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
        doubleBackEnabled = getIntent().getBooleanExtra(ContextVS.DOUBLE_BACK_KEY, true);
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
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("mailto:")) {
                        MailTo mt = MailTo.parse(url);
                        launchEmailClient(mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
                        view.reload();
                        return true;
                    } else view.loadUrl(url);
                    return true;
                }
            });
            webView.loadUrl(viewerURL);
        }
        webViewPlaceholder.addView(webView);
    }

    private void launchEmailClient(String address, String subject, String body, String cc) {
        /*Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_CC, cc);
        intent.setType("message/rfc822");*/
        if(subject == null) subject = "";
        if(body == null) body = "";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse("mailto:?subject=" + subject + "&to=" + address + "&body=" + body);
        intent.setData(data);
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (webView != null) {
            webViewPlaceholder.removeView(webView);
        }
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.browservs);
        initUI();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        outState.putSerializable(ContextVS.OPERATIONVS_KEY, operationVS);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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
            final String decodedStr = new String(Base64.decode(appMessage.getBytes(), FLAGS), "UTF-8");
            webView.post(new Runnable() {
                @Override public void run() {
                    webView.loadUrl("javascript:clientTool.unescapedMsg(unescape('" + decodedStr + "'))");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void unescapedMsg(String jsonStr) {
        try {
            operationVS = JSON.readValue(jsonStr, OperationVS.class);
            switch(operationVS.getOperation()) {
                case BROWSER_URL:
                    if(operationVS.getEmail() != null) {
                        launchEmailClient(operationVS.getEmail() , operationVS.getSubject(),
                                operationVS.getMessage(), null);
                    }
                    break;
                case REPRESENTATIVE_STATE:
                    RepresentationStateDto representationState = PrefUtils.getRepresentationState();
                    invokeOperationCallback(representationState, operationVS.getCallerCallback());
                    break;
                default:
                    LOGD(TAG, "unknown operation: " + operationVS.getOperation());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void invokeOperationCallback(Object dto, String callerCallback) {
        try {
            String dtoStr = JSON.getMapper().writeValueAsString(dto);
            invokeOperationCallback(dtoStr, callerCallback);
        } catch (JsonProcessingException e) { e.printStackTrace(); }
    }

    public void invokeOperationCallback(String dtoStr, String callerCallback) {
        try {
            String base64EncodedMsg = new String(Base64.encode(dtoStr.getBytes("UTF8"), FLAGS));
            final String jsCommand = "javascript:setClientToolMessage('" + callerCallback + "','" +
                    base64EncodedMsg + "')";
            LOGD(TAG, "jsCommand: " + jsCommand);
            webView.post(new Runnable() {
                @Override public void run() {
                    webView.loadUrl(jsCommand);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace(); }
    }

    public void invokeOperationCallback(int statusCode, String message, String callerCallback) {
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        invokeOperationCallback(resultMap, callerCallback);
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

    @Override
    public void onBackPressed() {
        String webUrl = webView.getUrl();
        LOGD(TAG + ".onBackPressed", "webUrl: " + webUrl);
        if (doubleBackToExitPressedOnce || !doubleBackEnabled) {
            super.onBackPressed();
            return;
        } else webView.loadUrl("javascript:try { app.back() } catch(e) { window.history.back() }");
        this.doubleBackToExitPressedOnce = true;
        if(showBrowserAdvice) {
            Snackbar.make(webViewPlaceholder, getString(R.string.double_back_advice), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.no_more_advice), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showBrowserAdvice = false;
                        }
                    }).show();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 500);
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