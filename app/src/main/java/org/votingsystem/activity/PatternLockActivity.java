package org.votingsystem.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.ui.PatternLockView;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PatternLockActivity extends AppCompatActivity {

    private static final String TAG = PatternLockActivity.class.getSimpleName();

    private enum PinChangeStep {PATTERN_REQUEST, NEW_PATTERN_REQUEST, NEW_PATTERN_CONFIRM}

    public static final int RC_PIN_PASSWORD  = 0;
    public static final int RC_IDCARD_PASSWORD  = 1;

    public static final int MODE_VALIDATE_INPUT = 0;
    public static final int MODE_CHANGE_PASSWORD    = 1;
    private int requestMode;

    private PatternLockView mCircleLockView;
    private TextView msgTextView;
    private PinChangeStep passwChangeStep = PinChangeStep.PATTERN_REQUEST;
    private Boolean withPasswordConfirm;
    private String newPin;
    private String firstPin;
    private CryptoDeviceAccessMode passwAccessMode;
    private char[] dniePassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        UIUtils.setSupportActionBar(this);
        mCircleLockView = (PatternLockView) findViewById(R.id.lock_view_circle);
        msgTextView = (TextView) findViewById(R.id.msg);
        if(getIntent().getStringExtra(ContextVS.MESSAGE_KEY) != null) {
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(ContextVS.MESSAGE_KEY)));
        }
        withPasswordConfirm = getIntent().getExtras().getBoolean(ContextVS.PASSWORD_CONFIRM_KEY, false);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        requestMode = getIntent().getExtras().getInt(ContextVS.MODE_KEY, MODE_VALIDATE_INPUT);
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                if(passwAccessMode == null) {
                    processPassword(null);
                } else if(passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PIN) {
                    Intent intent = new Intent(this, PinActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_actual_passw_msg));
                    startActivityForResult(intent, RC_PIN_PASSWORD);
                } else {
                    msgTextView.setText(getString(R.string.enter_actual_passw_msg));
                }
                break;
            case MODE_VALIDATE_INPUT:
                getSupportActionBar().setTitle(R.string.pattern_lock_lbl);
                break;
        }
        mCircleLockView.setCallBack(new PatternLockView.CallBack() {
            @Override
            public int onFinish(PatternLockView.Password password) {
                LOGD(TAG, "password length " + password.list.size());
                processPassword(password.string);
                return PatternLockView.CODE_PASSWORD_CORRECT;
            }
        });
        mCircleLockView.setOnNodeTouchListener(new PatternLockView.OnNodeTouchListener() {
            @Override public void onNodeTouched(int NodeId) {
                //LOGD(TAG, "node " + NodeId + " has touched!");
            }
        });
    }

    private void processPassword(final String passw) {
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                switch(passwChangeStep) {
                    case PATTERN_REQUEST:
                        if(passwAccessMode == null) {
                            msgTextView.setText(getString(R.string.enter_new_passw_to_app_msg));
                        } else if(passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PIN) {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray());
                        } else {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray());
                            msgTextView.setText(getString(R.string.enter_new_passw_msg));
                        }
                        passwChangeStep =  PinChangeStep.NEW_PATTERN_REQUEST;
                        return;
                    case NEW_PATTERN_REQUEST:
                        newPin = passw;
                        passwChangeStep =  PinChangeStep.NEW_PATTERN_CONFIRM;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        return;
                    case NEW_PATTERN_CONFIRM:
                        if(passw.equals(newPin)) {
                            if(dniePassword != null) {
                                PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PATTER_LOCK,
                                        passw.toCharArray(), dniePassword);
                            } else if(PrefUtils.isDNIeEnabled()) {
                                Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                                intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_msg));
                                intent.putExtra(ContextVS.MODE_KEY, ID_CardNFCReaderActivity.MODE_PASSWORD_REQUEST);
                                startActivityForResult(intent, RC_IDCARD_PASSWORD);
                                return;
                            }
                            showResultDialog();
                            return;
                        } else {
                            passwChangeStep =  PinChangeStep.NEW_PATTERN_REQUEST;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            return;
                        }
                }
                break;
            case MODE_VALIDATE_INPUT:
                if(withPasswordConfirm) {
                    if(firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.repeat_password));
                        return;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            msgTextView.setText(getString(R.string.password_mismatch));
                            return;
                        }
                    }
                }
                if(!passwAccessMode.validateHash(passw, this)) return;
                break;
        }
        finishOK(passw);
    }

    private void showResultDialog() {
        DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finishOK(newPin);
                    }
                });
        UIUtils.showMessageDialog(getString(R.string.change_password_lbl), getString(
                R.string.new_password_ok_msg), positiveButton, null, this);
    }

    private void finishOK(String passw) {
        Intent resultIntent = new Intent();
        ResponseVS responseVS = ResponseVS.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        resultIntent.putExtra(ContextVS.MODE_KEY, CryptoDeviceAccessMode.Mode.PATTER_LOCK);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
    
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PIN_PASSWORD:
                if(Activity.RESULT_OK == resultCode)  {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    processPassword(new String(responseVS.getMessageBytes()));
                } else if(ResponseVS.SC_ERROR == resultCode) {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                } else {
                    DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    setResult(Activity.RESULT_CANCELED, new Intent());
                                    finish();
                                }
                            });
                    UIUtils.showMessageDialog(getString(R.string.error_lbl),
                            getString(R.string.missing_actual_passw_error_msg), positiveButton,
                            null, this);
                }
                break;
            case RC_IDCARD_PASSWORD:
                if(Activity.RESULT_OK == resultCode)  {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    this.dniePassword  = new String(responseVS.getMessageBytes()).toCharArray();
                    PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PIN,
                            newPin.toCharArray(), dniePassword);
                    showResultDialog();
                } else {
                    passwChangeStep =  PinChangeStep.NEW_PATTERN_REQUEST;
                    msgTextView.setText(getString(R.string.new_password_error_msg));
                }
                break;
        }
    }

}
