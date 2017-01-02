package org.votingsystem.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.ui.PatternLockView;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.xml.XMLUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PatternLockActivity extends AppCompatActivity {

    private static final String TAG = PatternLockActivity.class.getSimpleName();

    private enum PinChangeStep {PATTERN_REQUEST, NEW_PATTERN_REQUEST, NEW_PATTERN_CONFIRM}

    public static final int RC_IDCARD_PASSWORD = 0;

    //we are here because we want to validate a signature request
    public static final int MODE_VALIDATE_INPUT = 0;
    //we are here because we want to set/change the password
    public static final int MODE_CHANGE_PASSWORD = 1;
    private int requestMode;

    private PatternLockView mCircleLockView;
    private TextView msgTextView;
    private PinChangeStep passwChangeStep = PinChangeStep.PATTERN_REQUEST;
    private Boolean withPasswordConfirm;
    private String newPin;
    private String firstPin;
    private CryptoDeviceAccessMode passwAccessMode;
    private char[] smartCardPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        UIUtils.setSupportActionBar(this);
        mCircleLockView = (PatternLockView) findViewById(R.id.lock_view_circle);
        msgTextView = (TextView) findViewById(R.id.msg);
        String operationCode = getIntent().getStringExtra(Constants.OPERATION_CODE_KEY);

        if(PrefUtils.getDNIeCAN() == null) {
            Intent intent = new Intent(this, UserDataFormActivity.class);
            startActivity(intent);
            finish();
        }

        if (operationCode != null) {
            TextView operationCodeText = (TextView) findViewById(R.id.operation_code);
            operationCodeText.setText(operationCode);
            operationCodeText.setVisibility(View.VISIBLE);
        }
        if (getIntent().getStringExtra(Constants.MESSAGE_KEY) != null) {
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(Constants.MESSAGE_KEY)));
        }
        withPasswordConfirm = getIntent().getExtras().getBoolean(Constants.PASSWORD_CONFIRM_KEY, false);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        requestMode = getIntent().getExtras().getInt(Constants.MODE_KEY, MODE_VALIDATE_INPUT);
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                processPassword(null);
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
            @Override
            public void onNodeTouched(int NodeId) {
                //LOGD(TAG, "node " + NodeId + " has touched!");
            }
        });
    }

    private void processPassword(final String passw) {
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                switch (passwChangeStep) {
                    case PATTERN_REQUEST:
                        msgTextView.setText(getString(R.string.enter_new_passw_to_app_msg));
                        passwChangeStep = PinChangeStep.NEW_PATTERN_REQUEST;
                        return;
                    case NEW_PATTERN_REQUEST:
                        newPin = passw;
                        passwChangeStep = PinChangeStep.NEW_PATTERN_CONFIRM;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        return;
                    case NEW_PATTERN_CONFIRM:
                        if (passw.equals(newPin)) {
                            Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                            intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.enter_password_msg));
                            intent.putExtra(Constants.MODE_KEY, ID_CardNFCReaderActivity.MODE_REQUEST_PASSWORD);
                            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, XMLUtils.dummyString().getBytes());
                            startActivityForResult(intent, RC_IDCARD_PASSWORD);
                            return;
                        } else {
                            passwChangeStep = PinChangeStep.NEW_PATTERN_REQUEST;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            return;
                        }
                }
                break;
            case MODE_VALIDATE_INPUT:
                if (withPasswordConfirm) {
                    if (firstPin == null) {
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
                if (!passwAccessMode.validateHash(passw, this)) return;
                break;
        }
        finishOK(passw);
    }

    private void finishOK(String passw) {
        Intent resultIntent = new Intent();
        ResponseDto response = ResponseDto.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(Constants.RESPONSE_KEY, response);
        resultIntent.putExtra(Constants.MODE_KEY, CryptoDeviceAccessMode.Mode.PATTER_LOCK);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_IDCARD_PASSWORD:
                if (Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    this.smartCardPassword = responseDto.getMessage().toCharArray();
                    PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PATTER_LOCK,
                            newPin.toCharArray(), smartCardPassword);
                    DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finishOK(newPin);
                                }
                            });
                    UIUtils.showMessageDialog(getString(R.string.change_password_lbl), getString(
                            R.string.new_password_ok_msg), positiveButton, null, this);
                } else {
                    passwChangeStep = PinChangeStep.NEW_PATTERN_REQUEST;
                    msgTextView.setText(getString(R.string.new_password_error_msg));
                }
                break;
        }
    }

}
