package org.votingsystem.dto;

import android.support.v7.app.AppCompatActivity;

import org.votingsystem.android.R;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.UIUtils;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessMode implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Mode{PIN, PATTER_LOCK}

    private Mode mode;
    private String hashBase64;

    public CryptoDeviceAccessMode(Mode mode, String passw) {
        try {
            this.mode = mode;
            this.hashBase64 = StringUtils.getHashBase64(passw, ContextVS.VOTING_DATA_DIGEST);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getHashBase64() {
        return hashBase64;
    }

    public void setHashBase64(String hashBase64) {
        this.hashBase64 = hashBase64;
    }

    public boolean validateHash(String passw, AppCompatActivity activity) {
        int numRetries = -1;
        try {
            String passwHash = StringUtils.getHashBase64(passw, ContextVS.VOTING_DATA_DIGEST);
            if(!hashBase64.equals(passwHash)) {
                numRetries = PrefUtils.incrementPasswordRetries();
                throw new ExceptionVS(activity.getString(R.string.password_error_msg) + ", " +
                        activity.getString(R.string.enter_password_retry_msg,
                                (ContextVS.NUM_MAX_PASSW_RETRIES - numRetries)));
            }
            PrefUtils.resetPasswordRetries();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if((ContextVS.NUM_MAX_PASSW_RETRIES - numRetries) == 0) {
                UIUtils.launchMessageActivity(ResponseVS.ERROR(activity.getString(
                        R.string.retries_exceeded_caption), null).setNotificationMessage(
                        activity.getString(R.string.retries_exceeded_msg)));
                activity.setResult(ResponseVS.SC_ERROR);
                activity.finish();
            } else MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, activity.getString(R.string.error_lbl),
                    ex.getMessage(), activity.getSupportFragmentManager());
            return false;
        }
    }
}
