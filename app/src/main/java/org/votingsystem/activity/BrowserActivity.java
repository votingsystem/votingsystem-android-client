package org.votingsystem.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.http.ContentType;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;

import java.lang.reflect.Field;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * <p>
 * http://www.devahead.com/blog/2012/01/preserving-the-state-of-an-android-webview-on-screen-orientation-change/
 */
public class BrowserActivity extends AppCompatActivity {

    public static final String TAG = BrowserActivity.class.getSimpleName();

    private String viewerURL;
    private String jsCommand;
    private App app = null;
    private String broadCastId = BrowserActivity.class.getSimpleName();
    private WebView webView;
    private FrameLayout webViewPlaceholder;
    private OperationDto operation;
    private boolean doubleBackEnabled = true;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean showBrowserAdvice = true;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            OperationType operationType = (OperationType) intent.getSerializableExtra(Constants.OPERATION_TYPE);
            if (operationType == null && responseDto != null)
            if (responseDto.getOperation() != null) {
                if (ContentType.JSON == responseDto.getContentType()) {
                    invokeOperationCallback(responseDto.getMessage(),
                            responseDto.getOperation().getCallerCallback());
                } else {
                    invokeOperationCallback(responseDto.getStatusCode(),
                            responseDto.getNotificationMessage(), responseDto.getOperation().getCallerCallback());
                }
            } else
                MessageDialogFragment.showDialog(responseDto.getStatusCode(), responseDto.getCaption(),
                        responseDto.getNotificationMessage(), getSupportFragmentManager());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browservs);
        app = (App) getApplicationContext();
        viewerURL = getIntent().getStringExtra(Constants.URL_KEY);
        jsCommand = getIntent().getStringExtra(Constants.JS_COMMAND_KEY);
        doubleBackEnabled = getIntent().getBooleanExtra(Constants.DOUBLE_BACK_KEY, true);
        if (savedInstanceState != null) {
            operation = (OperationDto) savedInstanceState.getSerializable(Constants.OPERATION_KEY);
        }
        /*if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/
        initUI();
    }

    protected void initUI() {
        webViewPlaceholder = ((FrameLayout) findViewById(R.id.webViewPlaceholder));
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
                    //webView.loadUrl("javascript:setClientToolConnected()");
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

                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    SslCertificate serverCertificate = error.getCertificate();
                    try {
                        Field f = serverCertificate.getClass().getDeclaredField("mX509Certificate");
                        f.setAccessible(true);
                        X509Certificate x509Certificate = (X509Certificate) f.get(serverCertificate);
                        if (!Constants.IS_DEBUG_SESSION)
                            throw new Exception("server with untrusted cert: " +
                                    x509Certificate.getSubjectDN());
                        handler.proceed();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
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
        if (subject == null) subject = "";
        if (body == null) body = "";
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        outState.putSerializable(Constants.OPERATION_KEY, operation);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.wait_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @JavascriptInterface
    public void setMessage(String appMessage) {
        LOGD(TAG + ".setMessage", "appMessage: " + appMessage);
        try {
            final String decodedStr = Base64.encodeToString(appMessage.getBytes(), Base64.NO_WRAP);
            webView.post(new Runnable() {
                @Override
                public void run() {
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
            operation = null;
            //operation = JSON.readValue(jsonStr, OperationDto.class);
            switch (operation.getOperation()) {
                case PROCESS_URL:
                    if (operation.getEmail() != null) {
                        launchEmailClient(operation.getEmail(), operation.getSubject(),
                                operation.getMessage(), null);
                    }
                    break;
                default:
                    LOGD(TAG, "unknown operation: " + operation.getOperation());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invokeOperationCallback(String dtoStr, String callerCallback) {
        try {
            String base64EncodedMsg = new String(Base64.encode(dtoStr.getBytes("UTF8"), Base64.NO_WRAP));
            final String jsCommand = "javascript:setClientToolMessage('" + callerCallback + "','" +
                    base64EncodedMsg + "')";
            LOGD(TAG, "jsCommand: " + jsCommand);
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(jsCommand);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void invokeOperationCallback(int statusCode, String message, String callerCallback) {
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        String dtoStr = "{\"statusCode\":\"" + statusCode + "\",\"message\":\"" + message + "\"}";
        invokeOperationCallback(dtoStr, callerCallback);
    }

    @Override
    public void onBackPressed() {
        String webUrl = webView.getUrl();
        LOGD(TAG + ".onBackPressed", "webUrl: " + webUrl);
        if (doubleBackToExitPressedOnce || !doubleBackEnabled) {
            super.onBackPressed();
            return;
        } else webView.loadUrl("javascript:try { vs.back() } catch(e) { window.history.back() }");
        this.doubleBackToExitPressedOnce = true;
        if (showBrowserAdvice) {
            Snackbar.make(webViewPlaceholder, getString(R.string.double_back_advice), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.understood_lbl), new View.OnClickListener() {
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

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}