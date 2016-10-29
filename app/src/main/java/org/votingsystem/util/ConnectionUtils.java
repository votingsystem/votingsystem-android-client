package org.votingsystem.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import org.votingsystem.App;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResponseDto;
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

    public static void initUnathenticatedConnection(AppCompatActivity activity, OperationType sessionType) {
        Intent startIntent = new Intent(activity, WebSocketService.class);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.WEB_SOCKET_INIT);
        startIntent.putExtra(Constants.SESSION_KEY, sessionType);
        activity.startService(startIntent);
    }

    public static void initConnection(final AppCompatActivity activity) {
        CryptoDeviceAccessMode passwordAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        if(passwordAccessMode != null) {
            Utils.getProtectionPassword(RC_PASSWORD_REQUEST,
                    activity.getString(R.string.connection_passw_msg), null, activity);
        } else {
            launchNFC_IDCard(activity, null);
        }
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data,
                        AppCompatActivity activity) {
        LOGD(TAG, " --- onActivityResult --- requestCode: " + requestCode);
        if(data == null) return;
        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
        switch (requestCode) {
            case RC_INIT_CONNECTION_REQUEST:
                if(responseDto != null && responseDto.getCMS() != null) {
                    try {
                        initSessionMessageDto = SocketMessageDto.INIT_SIGNED_SESSION_REQUEST();
                        initSessionMessageDto.setCMS(responseDto.getCMS());
                        Intent startIntent = new Intent(activity, WebSocketService.class);
                        startIntent.putExtra(Constants.MESSAGE_KEY, JSON.writeValueAsString(initSessionMessageDto));
                        activity.startService(startIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RC_PASSWORD_REQUEST:
                if(ResponseDto.SC_OK == responseDto.getStatusCode())
                    launchNFC_IDCard(activity, new String(responseDto.getMessageBytes()).toCharArray());
                break;
        }
    }

    private static void launchNFC_IDCard(AppCompatActivity activity, char[] accessModePassw) {
        try {
            Intent intent = new Intent(activity, ID_CardNFCReaderActivity.class);
            MessageDto initSessionDto = MessageDto.REQUEST(PrefUtils.getDeviceId(),
                    HttpConnection.getInstance().getSessionId(
                    StringUtils.getDomain(App.getInstance().getCurrencyServerURL())));
            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, JSON.writeValueAsString(initSessionDto));
            intent.putExtra(Constants.MESSAGE_SUBJECT_KEY,
                    activity.getString(R.string.init_authenticated_session_msg_subject));
            if(accessModePassw != null)
                intent.putExtra(Constants.PASSWORD_KEY, new String(accessModePassw).toCharArray());
            activity.startActivityForResult(intent, RC_INIT_CONNECTION_REQUEST);
        } catch (Exception ex) {
            MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, activity.getString(R.string.error_lbl),
                    ex.getMessage(), activity.getSupportFragmentManager());
        }
    }

}