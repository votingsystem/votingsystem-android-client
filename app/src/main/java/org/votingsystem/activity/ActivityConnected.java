package org.votingsystem.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public abstract class ActivityConnected extends AppCompatActivity {

    public static final String TAG = ActivityConnected.class.getSimpleName();

    private Dialog connectionRequiredDialog;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(Constants.WEBSOCKET_MSG_KEY);
            if(socketMsg != null) {
                LOGD(TAG + ".broadcastReceiver", "WebSocketMessage typeVS: " + socketMsg.getOperation());
                if(connectionRequiredDialog != null) connectionRequiredDialog.dismiss();
                ProgressDialogFragment.hide(getSupportFragmentManager());
                changeConnectionStatus();
                if(ResponseDto.SC_ERROR == socketMsg.getStatusCode() ||
                        ResponseDto.SC_WS_CONNECTION_INIT_ERROR == socketMsg.getStatusCode()) {
                    MessageDialogFragment.showDialog(socketMsg.getStatusCode(),
                            socketMsg.getCaption(), socketMsg.getMessage(),
                            getSupportFragmentManager());
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ConnectionUtils.onActivityResult(requestCode, resultCode, data, this);
    }

    @Override protected void onResume() {
        super.onResume();
        if(isConnectionRequired() && PrefUtils.isDNIeEnabled() &&
                !App.getInstance().isWithSocketConnection() && connectionRequiredDialog == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                ConnectionUtils.initConnection(ActivityConnected.this);
                            } catch (Exception ex) { ex.printStackTrace();}

                        }
                    });
            DialogButton negativeButton = new DialogButton(getString(R.string.cancel_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            connectionRequiredDialog = UIUtils.showMessageDialog(getString(R.string.connect_lbl),
                    getString(R.string.connection_required_msg),
                    positiveButton, negativeButton, this);
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }

    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
    }

    public abstract void changeConnectionStatus();

    public abstract boolean isConnectionRequired();

}