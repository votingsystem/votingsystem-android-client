package org.votingsystem.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.SerializationFeature;

import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSSignerActivity extends AppCompatActivity {
	
	public static final String TAG = CMSSignerActivity.class.getSimpleName();

    public static final int RC_PASSWORD_REQUEST   = 0;
    public static final int RC_SIGN_REQUEST       = 1;

    private String broadCastId = CMSSignerActivity.class.getSimpleName();
    private WebView webView;
    private Menu menu;
    private CMSSignedMessage cms;
    private TextView signature_state;
    private SocketMessageDto socketMessage;
    private OperationDto operationDto;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        SocketMessageDto socketMessageResponse = (SocketMessageDto) intent.getSerializableExtra(
                ContextVS.WEBSOCKET_MSG_KEY);
        setProgressDialogVisible(false, null, null);
        if(socketMessageResponse != null) {
            if(TypeVS.OPERATION_RESULT == socketMessageResponse.getMessageType()) {
                if(ResponseVS.SC_WS_MESSAGE_SEND_OK != socketMessageResponse.getStatusCode()) {
                    showMessage(socketMessageResponse.getStatusCode(),
                            getString(R.string.sign_document_lbl), socketMessageResponse.getMessage());
                }
            } else LOGD(TAG + ".broadcastReceiver", "socketMessageResponse:" +
                    socketMessageResponse.getOperation());
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

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.cms_signer);
        UIUtils.setSupportActionBar(this);
        webView = (WebView) findViewById(R.id.cms_signed_content);
        signature_state = (TextView) findViewById(R.id.signature_state);
        socketMessage = (SocketMessageDto) getIntent().getSerializableExtra(ContextVS.WEBSOCKET_MSG_KEY);
        try {
            operationDto = socketMessage.getMessage(OperationDto.class);
            String signatureContent = JSON.getMapper().configure(SerializationFeature.INDENT_OUTPUT,
                    true).writeValueAsString(socketMessage.getMessage());
            webView.loadData(signatureContent, "application/json", "UTF-8");
        } catch (Exception ex) { ex.printStackTrace(); }
        if(savedInstanceState != null) {
            byte[] cmsBytes = savedInstanceState.getByteArray(ContextVS.CMS_MSG_KEY);
            if(cmsBytes != null) {
                try {
                    cms = new CMSSignedMessage(cmsBytes);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        TextView textView = (TextView) findViewById(R.id.deviceName);
        textView.setText(getString(R.string.sign_send_msg));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.sign_request_lbl));
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cms_signer, menu);
        if(cms != null) setMenu();
        return true;
    }

    private void setMenu() {
        if(cms != null) {
            if(menu != null) {
                menu.setGroupVisible(R.id.signature_items, true);
                menu.removeItem(R.id.sign_document);
                menu.removeItem(R.id.reject_sign_request);
            }
            findViewById(R.id.signature_state).setVisibility(View.VISIBLE);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if(data == null) return;
        final ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        switch(requestCode) {
            case RC_SIGN_REQUEST:
                if(responseVS != null && responseVS.getCMS() != null) {
                    cms = responseVS.getCMS();
                    executorService.submit(new Runnable() {
                        @Override public void run() {
                            try {
                                ResponseVS response = HttpHelper.sendData(cms.toPEM(),
                                        ContentType.JSON_SIGNED,
                                        operationDto.getServiceURL());
                                sendSocketMessage(socketMessage.getResponse(response.getStatusCode(),
                                        response.getMessage(), null, TypeVS.OPERATION_RESULT));
                            } catch (Exception e) { e.printStackTrace(); }
                        }});
                    setMenu();
                }
                break;
            case RC_PASSWORD_REQUEST:
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    String accessModePassw = new String(responseVS.getMessageBytes());
                    Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, socketMessage.getMessage());
                    intent.putExtra(ContextVS.USER_KEY, socketMessage.getDeviceFromName());
                    intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, getString(R.string.sign_request_lbl));
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.sign_send_msg));
                    intent.putExtra(ContextVS.PASSWORD_KEY, accessModePassw.toCharArray());
                    startActivityForResult(intent, RC_SIGN_REQUEST);
                }
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.sign_document:
                Utils.getProtectionPassword(RC_PASSWORD_REQUEST, null, null, this);
                return true;
            case android.R.id.home:
            case R.id.reject_sign_request:
                finish();
                return true;
            case R.id.ban_device:
                return true;
            case R.id.share_receipt:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType(ContentType.TEXT.getName());
                try {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile(cms.toPEM(), this));
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
            if(cms != null) outState.putByteArray(ContextVS.CMS_MSG_KEY, cms.getEncoded());
        }catch (Exception e) { e.printStackTrace(); }
        super.onSaveInstanceState(outState);
    }

}