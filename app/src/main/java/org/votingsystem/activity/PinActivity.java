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
import org.votingsystem.service.WebSocketService;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
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
    private String msgUUID;
    private TypeVS operation;
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
        operation = (TypeVS) getIntent().getSerializableExtra(ContextVS.OPERATION_KEY);
        msgUUID = getIntent().getStringExtra(ContextVS.UUID_KEY);
        String operationCode = getIntent().getStringExtra(ContextVS.OPERATION_CODE_KEY);
        if(operationCode != null) {
            TextView operationCodeText = (TextView) findViewById(R.id.operation_code);
            operationCodeText.setText(operationCode);
            operationCodeText.setVisibility(View.VISIBLE);
        }
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
                    processPassword(null);
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
                        if(processPassword(pin)) {
                            //this is to make soft keyboard allways visible when input needed
                            new Handler().post(new Runnable() {
                                @Override public void run() {
                                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
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
                switch(pinChangeStep) {
                    case PIN_REQUEST:
                        if(passwAccessMode == null) {
                            msgTextView.setText(getString(R.string.enter_new_passw_to_app_msg));
                        } else if(passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PATTER_LOCK) {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray());
                        } else {
                            dniePassword = PrefUtils.getProtectedPassword(passw.toCharArray());
                            msgTextView.setText(getString(R.string.enter_new_passw_msg));
                            pinText.setText("");
                        }
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        return true;
                    case NEW_PIN_REQUEST:
                        newPin = passw;
                        pinChangeStep =  PinChangeStep.NEW_PIN_CONFIRM;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        pinText.setText("");
                        return true;
                    case NEW_PIN_CONFIRM:
                        if(passw.equals(newPin)) {
                            if(dniePassword != null) {
                                PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PIN,
                                        passw.toCharArray(), dniePassword);
                            } else if(PrefUtils.isDNIeEnabled()) {
                                Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                                intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_msg));
                                intent.putExtra(ContextVS.MODE_KEY, ID_CardNFCReaderActivity.MODE_PASSWORD_REQUEST);
                                startActivityForResult(intent, RC_IDCARD_PASSWORD);
                                return false;
                            }
                            showResultDialog();
                            return false;
                        } else {
                            pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            pinText.setText("");
                            return true;
                        }
                }
                break;
            case MODE_VALIDATE_INPUT:
                if(withPasswordConfirm) {
                    if(firstPin == null) {
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
                if(!passwAccessMode.validateHash(passw, this)) {
                    pinText.setText("");
                    return true;
                }
                break;
        }
        if(operation == TypeVS.WEB_SOCKET_REQUEST) {
            Intent startIntent = new Intent(this, WebSocketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.PIN);
            startIntent.putExtra(ContextVS.UUID_KEY, msgUUID);
            startService(startIntent);
        }
        finishOK(passw);
        return false;
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
        resultIntent.putExtra(ContextVS.MODE_KEY, CryptoDeviceAccessMode.Mode.PIN);
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
                    this.dniePassword  = new String(responseVS.getMessageBytes()).toCharArray();
                    PrefUtils.putProtectedPassword(CryptoDeviceAccessMode.Mode.PIN,
                            newPin.toCharArray(), dniePassword);
                    showResultDialog();
                } else {
                    pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                    msgTextView.setText(getString(R.string.new_password_error_msg));
                    pinText.setText("");
                }
                break;
        }
    }

}
