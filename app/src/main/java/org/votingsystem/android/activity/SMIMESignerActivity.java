package org.votingsystem.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.SerializationFeature;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.dto.SocketMessageDto;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.ProgressDialogFragment;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DeviceUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignerActivity extends ActionBarActivity {
	
	public static final String TAG = SMIMESignerActivity.class.getSimpleName();

    private AppContextVS contextVS = null;
    private String broadCastId = SMIMESignerActivity.class.getSimpleName();
    private WebView webView;
    private SocketMessageDto socketMessage;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        SocketMessageDto socketMessageResponse = null;
        try {
            String dtoStr = intent.getStringExtra(ContextVS.WEBSOCKET_MSG_KEY);
            if(dtoStr != null) {
                socketMessageResponse = JSON.getMapper().readValue(dtoStr, SocketMessageDto.class);
            }
        } catch (Exception ex) { ex.printStackTrace();}
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) sendSocketMessage(socketMessage);
        else {
            setProgressDialogVisible(false, null, null);
            if(socketMessageResponse != null) {
                if(TypeVS.MESSAGEVS_SIGN_RESPONSE == socketMessageResponse.getOperation()) {
                    if(ResponseVS.SC_WS_MESSAGE_SEND_OK == socketMessageResponse.getStatusCode()) {
                        UIUtils.launchMessageActivity(socketMessageResponse.getNotificationResponse(
                                SMIMESignerActivity.this), SMIMESignerActivity.this);
                        SMIMESignerActivity.this.finish();
                    } else showMessage(socketMessageResponse.getStatusCode(),
                            getString(R.string.sign_document_lbl), socketMessageResponse.getMessage());
                }
            }
        }
    }};

    private void sendSocketMessage(SocketMessageDto socketMessage) {
        LOGD(TAG + ".sendSocketMessage() ", "sendSocketMessage");
        try {
            Intent startIntent = new Intent(this, WebSocketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, socketMessage.getOperation());
            startIntent.putExtra(ContextVS.MESSAGE_KEY,
                    JSON.getMapper().writeValueAsString(socketMessage));
            setProgressDialogVisible(true,
                    getString(R.string.wait_msg), getString(R.string.signing_document_lbl));
            startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.smime_signer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        webView = (WebView) findViewById(R.id.smime_signed_content);
        contextVS = (AppContextVS) getApplicationContext();
        String dtoStr = getIntent().getStringExtra(ContextVS.WEBSOCKET_MSG_KEY);
        try {
            socketMessage = JSON.getMapper().readValue(dtoStr, SocketMessageDto.class);
            String signatureContent = JSON.getMapper().configure(SerializationFeature.INDENT_OUTPUT,
                    true).writeValueAsString(socketMessage.getTextToSign());
            webView.loadData(signatureContent, "application/json", "UTF-8");
        } catch (IOException ex) { ex.printStackTrace(); }
        TextView textView = (TextView) findViewById(R.id.deviceName);
        textView.setText(getString(R.string.signature_request_from_device,
                socketMessage.getDeviceFromName()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.sign_request_lbl));
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.smime_signer, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.sign_document:
                PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                        getString(R.string.ping_to_sign_msg), false, TypeVS.MESSAGEVS_SIGN);
                return true;
            case android.R.id.home:
            case R.id.reject_sign_request:
                try {
                    SocketMessageDto messageDto = socketMessage.getResponse(ResponseVS.SC_ERROR,
                            getString(R.string.reject_websocket_request_msg,
                                    DeviceUtils.getDeviceName()), TypeVS.MESSAGEVS_SIGN_RESPONSE);
                    sendSocketMessage(messageDto);
                    this.finish();
                } catch(Exception ex) {ex.printStackTrace();}
                return true;
            case R.id.ban_device:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showMessage(int statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
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
    }

}