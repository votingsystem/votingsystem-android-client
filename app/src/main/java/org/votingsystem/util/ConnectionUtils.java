package org.votingsystem.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import org.votingsystem.activity.DNIeSigningActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.service.WebSocketService;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ConnectionUtils {

    public static final String TAG = ConnectionUtils.class.getSimpleName();

    //high enough in order to avoid collisions with other request codes
    public static final int RC_INIT_CONNECTION_REQUEST = 1000;
    public static final int RC_PASSWORD_REQUEST        = 1001;

    private static SocketMessageDto initSessionMessageDto;

    public static void startConnectionRequest(String patternLock, AppCompatActivity activity) {
        try {
            Intent intent = new Intent(activity, DNIeSigningActivity.class);
            initSessionMessageDto = SocketMessageDto.INIT_SESSION_REQUEST();
            intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, JSON.writeValueAsString(initSessionMessageDto));
            intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY,
                    activity.getString(R.string.init_authenticated_session_msg_subject));
            if(patternLock != null)  intent.putExtra(ContextVS.PASSWORD_KEY, patternLock.toCharArray());
            activity.startActivityForResult(intent, RC_INIT_CONNECTION_REQUEST);
        } catch (Exception ex) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, activity.getString(R.string.error_lbl),
                    ex.getMessage(), activity.getSupportFragmentManager());
        }
    }

    public static void init_IDCARD_NFC_Process(final AppCompatActivity activity) {
        Utils.init_IDCARD_NFC_Process(RC_PASSWORD_REQUEST,
                activity.getString(R.string.enter_pattern_lock_msg), null, activity);
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data,
                        AppCompatActivity activity) {
        LOGD(TAG, " --- onActivityResult ---");
        if(data == null) return;
        final ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        switch (requestCode) {
            case RC_INIT_CONNECTION_REQUEST:
                if(responseVS != null && responseVS.getSMIME() != null) {
                    try {
                        initSessionMessageDto.setSMIME(responseVS.getSMIME());
                        Intent startIntent = new Intent(activity, WebSocketService.class);
                        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.WEB_SOCKET_INIT);
                        startIntent.putExtra(ContextVS.MESSAGE_KEY, JSON.writeValueAsString(initSessionMessageDto));
                        activity.startService(startIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RC_PASSWORD_REQUEST:
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    startConnectionRequest(new String(responseVS.getMessageBytes()), activity);
                }
                break;
        }
    }
}
