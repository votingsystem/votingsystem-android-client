package org.votingsystem.dto;

import android.support.v7.app.AppCompatActivity;

import org.votingsystem.android.R;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.throwable.ExceptionBase;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HashUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessMode implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Mode {PIN, PATTER_LOCK, DNIE_PASSW}

    private Mode mode;
    private String hashBase64;

    public CryptoDeviceAccessMode(Mode mode, char[] passw) {
        try {
            this.mode = mode;
            if (passw != null)
                this.hashBase64 = HashUtils.getHashBase64(new String(passw).getBytes(),
                        Constants.DATA_DIGEST_ALGORITHM);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Mode getMode() {
        return mode;
    }

    public boolean validateHash(String passw, AppCompatActivity activity) {
        int numRetries = -1;
        try {
            String passwHash = HashUtils.getHashBase64(passw.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
            if (!hashBase64.equals(passwHash)) {
                numRetries = PrefUtils.incrementPasswordRetries();
                throw new ExceptionBase(activity.getString(R.string.password_error_msg) + ", " +
                        activity.getString(R.string.enter_password_retry_msg,
                                Integer.valueOf(Constants.NUM_MAX_PASSW_RETRIES - numRetries).toString()));
            }
            PrefUtils.resetPasswordRetries();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if ((Constants.NUM_MAX_PASSW_RETRIES - numRetries) == 0) {
                UIUtils.launchMessageActivity(ResponseDto.ERROR(activity.getString(
                        R.string.retries_exceeded_caption), null).setNotificationMessage(
                        activity.getString(R.string.retries_exceeded_msg)));
                activity.setResult(ResponseDto.SC_ERROR);
                activity.finish();
            } else
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, activity.getString(R.string.error_lbl),
                        ex.getMessage(), activity.getSupportFragmentManager());
            return false;
        }
    }
}
