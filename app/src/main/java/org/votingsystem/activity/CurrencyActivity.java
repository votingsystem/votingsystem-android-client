package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.CurrencyFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.SelectDeviceDialogFragment;
import org.votingsystem.model.Currency;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyActivity extends AppCompatActivity {
	
	public static final String TAG = CurrencyActivity.class.getSimpleName();

    private WeakReference<CurrencyFragment> currencyRef;
    private AppVS appVS;
    private Currency currency;
    private String broadCastId = CurrencyActivity.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(
                    ContextVS.WEBSOCKET_MSG_KEY);
            if(socketMsg != null){
                setProgressDialogVisible(false, null, null);
                switch(socketMsg.getOperation()) {
                    case MESSAGEVS_TO_DEVICE:
                        break;
                    case CURRENCY_WALLET_CHANGE:
                        if(ResponseVS.SC_OK == socketMsg.getStatusCode()) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.send_to_wallet),
                                    getString(R.string.item_sended_ok_msg),
                                    CurrencyActivity.this).setPositiveButton(getString(R.string.accept_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(CurrencyActivity.this, ActivityBase.class);
                                            intent.putExtra(ContextVS.FRAGMENT_KEY, R.id.wallet);
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                        } else MessageDialogFragment.showDialog(socketMsg.getStatusCode(),
                                getString(R.string.error_lbl), getString(R.string.device_not_found_error_msg),
                                getSupportFragmentManager());
                        break;
                    default:
                        LOGD(TAG + ".broadcastReceiver", "socketMsg: " + socketMsg.getOperation());
                }
            } else {
                setProgressDialogVisible(false, null, null);
                switch(responseVS.getTypeVS()) {
                    case DEVICE_SELECT:
                        try {
                            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                                DeviceVSDto targetDevice = (DeviceVSDto) responseVS.getMessage(DeviceVSDto.class);
                                SocketMessageDto socketMessage = SocketMessageDto.getCurrencyWalletChangeRequest(
                                        targetDevice, Arrays.asList(currency));
                                Intent startIntent = new Intent(CurrencyActivity.this, WebSocketService.class);
                                startIntent.putExtra(ContextVS.MESSAGE_KEY, JSON.writeValueAsString(socketMessage));
                                startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                                startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CURRENCY_WALLET_CHANGE);
                                setProgressDialogVisible(true, getString(R.string.send_to_wallet),
                                        getString(R.string.connecting_lbl));
                                startService(startIntent);
                                Toast.makeText(CurrencyActivity.this,
                                        getString(R.string.send_to_wallet) + " - " +
                                        getString(R.string.check_target_device_lbl),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch(Exception ex) {ex.printStackTrace();}
                        break;
                    default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                            responseVS.getCaption(), responseVS.getNotificationMessage(),
                            getSupportFragmentManager());
                }
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        currency = (Currency) getIntent().getSerializableExtra(ContextVS.CURRENCY_KEY);
        setContentView(R.layout.fragment_container_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null) {
            currencyRef = new WeakReference<>(new CurrencyFragment());
            currencyRef.get().setArguments(Utils.intentToFragmentArguments(getIntent()));
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currencyRef.get(),
                    CurrencyFragment.class.getSimpleName()).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ConnectionUtils.onActivityResult(requestCode, resultCode, data, this);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    this.finish();
                    return true;
                case R.id.cert_info:
                    MessageDialogFragment.showDialog(null, getString(R.string.currency_cert_caption),
                            MsgUtils.getCertInfoMessage(currency.getCertificationRequest().
                                    getCertificate(), this), getSupportFragmentManager());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(currency.getReceipt().getSigner().
                                    getTimeStampToken(), appVS.getTimeStampCert(),
                            getSupportFragmentManager(), this);
                    break;
                case R.id.send_to_wallet:
                    if(!appVS.isWithSocketConnection()) {
                        DialogButton positiveButton = new DialogButton(getString(R.string.connect_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ConnectionUtils.init_IDCARD_NFC_Process(CurrencyActivity.this);
                                    }
                                });
                        UIUtils.showMessageDialog(getString(R.string.send_to_wallet), getString(
                            R.string.send_to_wallet_connection_required_msg), positiveButton, null, this);
                    } else SelectDeviceDialogFragment.showDialog(broadCastId, this.
                            getSupportFragmentManager(), SelectDeviceDialogFragment.TAG);
                    break;
                case R.id.share_currency:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(currency.getReceipt().getBytes());
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                        sendIntent.setType(ContentTypeVS.TEXT.getName());
                        startActivity(sendIntent);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected model type:" + currency.getTypeVS());
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.currency_fragment, menu);
        try {
            if(currency.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_currency);
            }
            if(currency.getState() != Currency.State.OK) {
                menu.removeItem(R.id.send_to_wallet);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    public class CurrencyFetcher extends AsyncTask<String, String, ResponseVS> {

        public CurrencyFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.loading_data_msg), getString(R.string.loading_info_msg)); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String currencyURL = urls[0];
            return HttpHelper.getData(currencyURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    currency.setReceiptBytes(responseVS.getMessageBytes());
                    if(currencyRef.get() != null) currencyRef.get().initCurrencyScreen(currency);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getSupportFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                        getString(R.string.error_lbl), responseVS.getMessage(),
                        getSupportFragmentManager());
            }
            setProgressDialogVisible(false, null, null);
        }
    }


}