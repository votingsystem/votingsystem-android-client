package org.votingsystem.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.xml.XMLUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PinActivity extends AppCompatActivity {

    private static final String TAG = PinActivity.class.getSimpleName();

    private enum PinChangeStep {PIN_REQUEST, NEW_PIN_REQUEST, NEW_PIN_CONFIRM}

    public static final int RC_IDCARD_PASSWORD = 0;

    //we are here because we want to validate a signature request
    public static final int MODE_VALIDATE_INPUT = 0;
    //we are here because we want to set/change the password
    public static final int MODE_CHANGE_PASSWORD = 1;
    private int requestMode;

    private TextView msgTextView;
    private EditText pinText;
    private PinChangeStep pinChangeStep = PinChangeStep.PIN_REQUEST;
    private boolean withPasswordConfirm;
    private String newPin;
    private String firstPin;
    private CryptoDeviceAccessMode passwAccessMode;
    private char[] smartCardPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pinText = (EditText) findViewById(R.id.pin);
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
        withPasswordConfirm = getIntent().getBooleanExtra(Constants.PASSWORD_CONFIRM_KEY, false);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        requestMode = getIntent().getExtras().getInt(Constants.MODE_KEY, MODE_VALIDATE_INPUT);
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                processPassword(null);
                break;
            case MODE_VALIDATE_INPUT:
                getSupportActionBar().setTitle(R.string.pin_lbl);
                break;
        }
        pinText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String pin = pinText.getText().toString();
                    if (pin != null && pin.length() == 4) {
                        if (processPassword(pin)) {
                            //this is to make soft keyboard allways visible when input needed
                            new Handler().post(new Runnable() {
                                @Override
                                public void run() {
                                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                                            .showSoftInput(pinText, InputMethodManager.SHOW_FORCED);
                                }
                            });
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    //returns false when no more inputs required
    private boolean processPassword(final String passw) {
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                switch (pinChangeStep) {
                    case PIN_REQUEST:
                        msgTextView.setText(getString(R.string.enter_new_passw_to_app_msg));
                        pinChangeStep = PinChangeStep.NEW_PIN_REQUEST;
                        return true;
                    case NEW_PIN_REQUEST:
                        newPin = passw;
                        pinChangeStep = PinChangeStep.NEW_PIN_CONFIRM;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        pinText.setText("");
                        return true;
                    case NEW_PIN_CONFIRM:
                        if (passw.equals(newPin)) {
                            Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                            intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.enter_password_msg));
                            intent.putExtra(Constants.MODE_KEY, ID_CardNFCReaderActivity.MODE_REQUEST_PASSWORD);
                            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, XMLUtils.dummyString().getBytes());
                            startActivityForResult(intent, RC_IDCARD_PASSWORD);
                            return false;
                        } else {
                            pinChangeStep = PinChangeStep.NEW_PIN_REQUEST;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            pinText.setText("");
                            return true;
                        }
                }
                break;
            case MODE_VALIDATE_INPUT:
                if (withPasswordConfirm) {
                    if (firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.repeat_password));
                        pinText.setText("");
                        return true;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            pinText.setText("");
                            msgTextView.setText(getString(R.string.password_mismatch));
                            return true;
                        }
                    }
                }
                if (!passwAccessMode.validateHash(passw, this)) {
                    pinText.setText("");
                    return true;
                }
                break;
        }
        finishOK(passw);
        return false;
    }

    private void finishOK(String passw) {
        Intent resultIntent = new Intent();

        ResponseDto responseDto = ResponseDto.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(Constants.RESPONSE_KEY, responseDto);
        resultIntent.putExtra(Constants.MODE_KEY, CryptoDeviceAccessMode.Mode.PIN);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PIN,
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
                    pinChangeStep = PinChangeStep.NEW_PIN_REQUEST;
                    msgTextView.setText(getString(R.string.new_password_error_msg));
                    pinText.setText("");
                }
                break;
        }
    }

}
