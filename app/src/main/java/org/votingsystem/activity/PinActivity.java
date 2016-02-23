package org.votingsystem.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PinActivity extends AppCompatActivity  {

    private static final String TAG = PinActivity.class.getSimpleName();

    private enum PinChangeStep {PIN_REQUEST, NEW_PIN_REQUEST, NEW_PIN_CONFIRM}

    public static final int RC_PATERN_PASSWORD  = 0;
    public static final int RC_IDCARD_PASSWORD  = 1;


    public static final int MODE_VALIDATE_INPUT  = 0;
    public static final int MODE_CHANGE_PASSWORD = 1;
    private int requestMode;

    private TextView msgTextView;
    private EditText pinText;
    private PinChangeStep pinChangeStep = PinChangeStep.PIN_REQUEST;
    private boolean withPasswordConfirm;
    private String newPin;
    private String firstPin;
    private CryptoDeviceAccessMode passwAccessMode;
    private char[] dniePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pinText = (EditText) findViewById(R.id.pin);
        msgTextView = (TextView) findViewById(R.id.msg);
        if(getIntent().getStringExtra(ContextVS.MESSAGE_KEY) != null) {
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(ContextVS.MESSAGE_KEY)));
        }
        withPasswordConfirm = getIntent().getBooleanExtra(ContextVS.PASSWORD_CONFIRM_KEY, false);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        requestMode = getIntent().getExtras().getInt(ContextVS.MODE_KEY, MODE_VALIDATE_INPUT);
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                if(passwAccessMode == null) {
                    Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_for_dni_msg));
                    intent.putExtra(ContextVS.MODE_KEY, ID_CardNFCReaderActivity.MODE_PASSWORD_REQUEST);
                    startActivityForResult(intent, RC_IDCARD_PASSWORD);
                } else if(passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PATTER_LOCK) {
                    Intent intent = new Intent(this, PatternLockActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_actual_passw_msg));
                    startActivityForResult(intent, RC_PATERN_PASSWORD);
                } else {
                    msgTextView.setText(getString(R.string.enter_actual_passw_msg));
                }
                break;
            case MODE_VALIDATE_INPUT:
                getSupportActionBar().setTitle(R.string.pin_lbl);
                break;
        }
        pinText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String pin = pinText.getText().toString();
                    if(pin != null && pin.length() == 4) {
                        processPassword(pin);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void processPassword(final String passw) {
        switch (requestMode) {
            case MODE_CHANGE_PASSWORD:
                switch(pinChangeStep) {
                    case PIN_REQUEST:
                        if(passwAccessMode == null) {
                            dniePassword = passw.toCharArray();
                        } else if(passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PATTER_LOCK) {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray(),
                                    AppVS.getInstance().getToken());
                        } else {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray(),
                                    AppVS.getInstance().getToken());
                            msgTextView.setText(getString(R.string.enter_new_passw_msg));
                            pinText.setText("");
                        }
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        return;
                    case NEW_PIN_REQUEST:
                        newPin = passw;
                        pinChangeStep =  PinChangeStep.NEW_PIN_CONFIRM;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        pinText.setText("");
                        return;
                    case NEW_PIN_CONFIRM:
                        if(passw.equals(newPin)) {
                            PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PIN,
                                    passw.toCharArray(), AppVS.getInstance().getToken(), dniePassword);
                            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finishOK(passw);
                                        }
                                    });
                            UIUtils.showMessageDialog(getString(R.string.change_password_lbl), getString(
                                    R.string.new_password_ok_msg), positiveButton, null, this);
                            return;
                        } else {
                            pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            pinText.setText("");
                            return;
                        }
                }
                break;
            case MODE_VALIDATE_INPUT:
                if(withPasswordConfirm) {
                    if(firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.repeat_password));
                        pinText.setText("");
                        return;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            pinText.setText("");
                            msgTextView.setText(getString(R.string.password_mismatch));
                            return;
                        }
                    }
                }
                if(!passwAccessMode.validateHash(passw, this)) {
                    pinText.setText("");
                    return;
                }
                break;
        }
        finishOK(passw);
    }


    private void finishOK(String passw) {
        Intent resultIntent = new Intent();
        ResponseVS responseVS = ResponseVS.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PATERN_PASSWORD:
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
                                    setResult(Activity.RESULT_CANCELED);
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
                    processPassword(new String(responseVS.getMessageBytes()));
                } else {
                    UIUtils.showPasswordRequiredDialog(this);
                }
                break;
        }
    }

}
