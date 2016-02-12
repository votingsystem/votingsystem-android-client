package org.votingsystem.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PinInputActivity extends AppCompatActivity  {

    private static final String TAG = PinInputActivity.class.getSimpleName();

    private enum PinChangeStep {PIN_REQUEST, NEW_PIN_REQUEST, NEW_PIN_CONFIRM}
    
    private TypeVS typeVS;
    private TextView msgTextView;
    private EditText pinText;
    private PinChangeStep pinChangeStep;
    private boolean withPasswordConfirm;
    private boolean withHashValidation;
    private String newPin;
    private String firstPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_input_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        typeVS = (TypeVS) getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        pinText = (EditText) findViewById(R.id.pin);
        msgTextView = (TextView) findViewById(R.id.msg);
        if(getIntent().hasExtra(ContextVS.MESSAGE_KEY)) {
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(ContextVS.MESSAGE_KEY)));
        }
        withPasswordConfirm = getIntent().getBooleanExtra(ContextVS.PASSWORD_CONFIRM_KEY, false);
        withHashValidation = getIntent().getBooleanExtra(ContextVS.HASH_VALIDATION_KEY, true);
        getSupportActionBar().setTitle(R.string.pin_dialog_caption);
        if(TypeVS.PIN_CHANGE == typeVS) {
            getSupportActionBar().setTitle(R.string.pin_change_lbl);
            switch (pinChangeStep) {
                case PIN_REQUEST:
                    msgTextView.setText(getString(R.string.enter_actual_pin_msg));
                    break;
                case NEW_PIN_REQUEST:
                    msgTextView.setText(getString(R.string.enter_new_pin_msg));
                    break;
                case NEW_PIN_CONFIRM:
                    msgTextView.setText(getString(R.string.confirm_new_pin_msg));
                    break;
            }
        }
        pinText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String pin = pinText.getText().toString();
                    if(pin != null && pin.length() == 4) {
                        setPin(pin);
                        return true;
                    }
                }
                return false;
            }
        });
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
                InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void setPin(final String pin) {
        if(typeVS == TypeVS.PIN_CHANGE) {
            switch(pinChangeStep) {
                case PIN_REQUEST:
                    if(validatePinHash(pin)) {
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        msgTextView.setText(getString(R.string.enter_new_pin_msg));
                        pinText.setText("");
                        return;
                    } else return;
                case NEW_PIN_REQUEST:
                    newPin = pin;
                    pinChangeStep =  PinChangeStep.NEW_PIN_CONFIRM;
                    msgTextView.setText(getString(R.string.confirm_new_pin_msg));
                    pinText.setText("");
                    return;
                case NEW_PIN_CONFIRM:
                    if(pin.equals(newPin)) {
                        PrefUtils.putPin(pin.toCharArray());
                        pinText.setVisibility(View.GONE);
                        msgTextView.setText(getString(R.string.new_pin_ok_msg));
                        return;
                    } else {
                        pinChangeStep =  PinChangeStep.NEW_PIN_REQUEST;
                        msgTextView.setText(getString(R.string.new_pin_error_msg));
                        pinText.setText("");
                        return;
                    }
            }
        } else {
            if(withPasswordConfirm) {
                if(firstPin == null) {
                    firstPin = pin;
                    msgTextView.setText(getString(R.string.repeat_password));
                    pinText.setText("");
                    return;
                } else {
                    if (!firstPin.equals(pin)) {
                        firstPin = null;
                        pinText.setText("");
                        msgTextView.setText(getString(R.string.password_mismatch));
                        return;
                    }
                }
            }
        }
        if(withHashValidation && !validatePinHash(pin)) return;
        Intent resultIntent = new Intent();
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, typeVS)
                .setMessageBytes(pin.getBytes());
        resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


    private boolean validatePinHash(String pin) {
        try {
            String expectedHash = PrefUtils.getPinHash();
            String pinHash = StringUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            if(!expectedHash.equals(pinHash)) {
                msgTextView.setText(getString(R.string.pin_error_msg));
                pinText.setText("");
                return false;
            } else return true;
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
