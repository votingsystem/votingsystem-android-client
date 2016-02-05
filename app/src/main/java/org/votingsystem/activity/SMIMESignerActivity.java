package org.votingsystem.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignerActivity extends AppCompatActivity {
	
	public static final String TAG = SMIMESignerActivity.class.getSimpleName();

    public static final int SIGN_REQUEST = 0;

    private AppVS appVS = null;
    private String broadCastId = SMIMESignerActivity.class.getSimpleName();
    private WebView webView;
    private Menu menu;
    private SMIMEMessage smime;
    private TextView signature_state;
    private SocketMessageDto socketMessage;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        SocketMessageDto socketMessageResponse = (SocketMessageDto) intent.getSerializableExtra(
                ContextVS.WEBSOCKET_MSG_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            signAndSendSocketMessage(socketMessage);
        }
        else {
            setProgressDialogVisible(false, null, null);
            if(socketMessageResponse != null) {
                if(TypeVS.MESSAGEVS_SIGN_RESPONSE == socketMessageResponse.getOperation()) {
                    if(ResponseVS.SC_WS_MESSAGE_SEND_OK == socketMessageResponse.getStatusCode()) {
                        UIUtils.launchMessageActivity(socketMessageResponse.getNotificationResponse(
                                SMIMESignerActivity.this));
                        SMIMESignerActivity.this.finish();
                    } else showMessage(socketMessageResponse.getStatusCode(),
                            getString(R.string.sign_document_lbl), socketMessageResponse.getMessage());
                } else LOGD(TAG + ".broadcastReceiver", "socketMessageResponse:" +
                        socketMessageResponse.getOperation());
            }
        }
    }};

    private void sendSocketMessage(SocketMessageDto socketMessage) {
        LOGD(TAG + ".sendSocketMessage() ", "sendSocketMessage");
        try {
            Intent startIntent = new Intent(this, WebSocketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, socketMessage.getOperation());
            startIntent.putExtra(ContextVS.MESSAGE_KEY,
                    JSON.writeValueAsString(socketMessage));
            startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    private void signAndSendSocketMessage(final SocketMessageDto socketMessage) {
        LOGD(TAG + ".signAndSendSocketMessage() ", "signAndSendSocketMessage");
        setProgressDialogVisible(true,
                getString(R.string.wait_msg), getString(R.string.signing_document_lbl));
        executorService.submit(new Runnable() {
            @Override public void run() {
                try {
                    SMIMEMessage smimeMessage = appVS.signMessage(socketMessage.getDeviceFromName(),
                            socketMessage.getTextToSign(), getString(R.string.sign_request_lbl));
                    sendSocketMessage(socketMessage.getResponse(ResponseVS.SC_OK, null, smimeMessage,
                            TypeVS.MESSAGEVS_SIGN_RESPONSE));
                    setMenu();
                } catch (Exception e) { e.printStackTrace(); }
            }});
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.smime_signer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        webView = (WebView) findViewById(R.id.smime_signed_content);
        signature_state = (TextView) findViewById(R.id.signature_state);
        appVS = (AppVS) getApplicationContext();
        socketMessage = (SocketMessageDto) getIntent().getSerializableExtra(ContextVS.WEBSOCKET_MSG_KEY);
        try {
            String signatureContent = JSON.getMapper().configure(SerializationFeature.INDENT_OUTPUT,
                    true).writeValueAsString(socketMessage.getTextToSign());
            webView.loadData(signatureContent, "application/json", "UTF-8");
        } catch (IOException ex) { ex.printStackTrace(); }
        if(savedInstanceState != null) {
            byte[] smimeBytes = savedInstanceState.getByteArray(ContextVS.SMIME_MSG_KEY);
            if(smimeBytes != null) {
                try {
                    smime = new SMIMEMessage(smimeBytes);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        TextView textView = (TextView) findViewById(R.id.deviceName);
        textView.setText(getString(R.string.signature_request_from_device,
                socketMessage.getDeviceFromName()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.sign_request_lbl));
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.smime_signer, menu);
        if(smime != null) setMenu();
        return true;
    }

    private void setMenu() {
        if(smime != null) {
            menu.setGroupVisible(R.id.signature_items, true);
            menu.removeItem(R.id.sign_document);
            menu.removeItem(R.id.reject_sign_request);
            findViewById(R.id.signature_state).setVisibility(View.VISIBLE);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG + ".onActivityResult", "onActivityResult");
        if(data == null) return;
        final ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(responseVS != null && responseVS.getSMIME() != null) {
            smime = responseVS.getSMIME();
            executorService.submit(new Runnable() {
                @Override public void run() {
                    try {
                        sendSocketMessage(socketMessage.getResponse(ResponseVS.SC_OK, null,
                                responseVS.getSMIME(), TypeVS.MESSAGEVS_SIGN_RESPONSE));
                    } catch (Exception e) { e.printStackTrace(); }
                }});
            setMenu();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.sign_document:
                if(PrefUtils.isDNIeEnabled()) {
                    Intent intent = new Intent(this, DNIeSigningActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, socketMessage.getTextToSign());
                    intent.putExtra(ContextVS.USER_KEY, socketMessage.getDeviceFromName());
                    intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, getString(R.string.sign_request_lbl));
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.signature_request_from_device,
                            socketMessage.getDeviceFromName()));
                    startActivityForResult(intent, SIGN_REQUEST);
                } else {
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                            getString(R.string.ping_to_sign_msg), false, TypeVS.MESSAGEVS_SIGN);
                }
                return true;
            case android.R.id.home:
            case R.id.reject_sign_request:
                try {
                    sendSocketMessage(socketMessage.getResponse(ResponseVS.SC_ERROR,
                            getString(R.string.reject_websocket_request_msg, Utils.getDeviceName()), null,
                            TypeVS.MESSAGEVS_SIGN_RESPONSE));
                    finish();
                } catch(Exception ex) {ex.printStackTrace();}
                return true;
            case R.id.ban_device:
                return true;
            case R.id.share_receipt:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType(ContentTypeVS.TEXT.getName());
                try {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile(smime.getBytes(), this));
                } catch (Exception e) { e.printStackTrace(); }
                startActivity(sendIntent);
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
        try {
            if(smime != null) outState.putByteArray(ContextVS.SMIME_MSG_KEY, smime.getBytes());
        }catch (Exception e) { e.printStackTrace(); }
        super.onSaveInstanceState(outState);
    }

}