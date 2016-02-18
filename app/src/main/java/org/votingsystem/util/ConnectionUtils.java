package org.votingsystem.util;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.signature.smime.SMIMEMessage;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ConnectionUtils {

    public static final String TAG = ConnectionUtils.class.getSimpleName();

    //high enough in order to avoid collisions with other request codes
    public static final int RC_PASSWORD_REQUEST        = 1001;

    public static void initConnection(final AppCompatActivity activity) {
        Utils.getProtectionPassword(RC_PASSWORD_REQUEST,
                activity.getString(R.string.connection_passw_msg), null, activity);
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data,
                        AppCompatActivity activity) {
        LOGD(TAG, " --- onActivityResult ---");
        if(data == null) return;
        ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        switch (requestCode) {
            case RC_PASSWORD_REQUEST:
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    new ConnectionTask(activity).execute();
                }
                break;
        }
    }

    public static class ConnectionTask extends AsyncTask<String, String, Void> {

        private AppCompatActivity activity;

        public ConnectionTask(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override protected void onPreExecute() {
            if(activity != null) {
                ProgressDialogFragment.showDialog(
                        activity.getString(R.string.connecting_caption),
                        activity.getString(R.string.wait_msg),
                        activity.getSupportFragmentManager());
            }
        }

        @Override protected Void doInBackground(String... urls) {
            try {
                SocketMessageDto initSessionMessageDto = SocketMessageDto.INIT_SESSION_REQUEST();
                SMIMEMessage smimeMessage = AppVS.getInstance().signMessage(
                        AppVS.getInstance().getCurrencyServer().getName(),
                        JSON.writeValueAsString(initSessionMessageDto),
                        activity.getString(R.string.init_authenticated_session_msg_subject));
                initSessionMessageDto.setSMIME(smimeMessage);
                Intent startIntent = new Intent(activity, WebSocketService.class);
                startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.WEB_SOCKET_INIT);
                startIntent.putExtra(ContextVS.MESSAGE_KEY, JSON.writeValueAsString(initSessionMessageDto));
                activity.startService(startIntent);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return null;
            }
        }

        @Override protected void onPostExecute(Void response) {
            if(activity != null) ProgressDialogFragment.hide(activity.getSupportFragmentManager());
        }
    }
}
