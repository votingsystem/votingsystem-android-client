package org.votingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessModeSelectorActivity extends ActivityConnected {

    public static final String TAG = CryptoDeviceAccessModeSelectorActivity.class.getSimpleName();

    public static final int RC_PATTERN_LOCK = 0;
    public static final int RC_PIN          = 1;

    private RadioButton radio_pin;
    private RadioButton radio_pattern;
    private RadioGroup radio_group;
    private CryptoDeviceAccessMode passwAccessMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_device_access_mode_selector);
        radio_group = (RadioGroup) findViewById(R.id.radio_group);
        radio_pin = (RadioButton) findViewById(R.id.radio_pin);
        radio_pattern = (RadioButton) findViewById(R.id.radio_pattern);
        UIUtils.setSupportActionBar(this, getString(R.string.crypto_device_access_mode_lbl));
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        updateView();
    }

    public void onRadioButtonClicked(View view) {
        Intent intent = null;
        switch(view.getId()) {
            case R.id.radio_pin:
                if(passwAccessMode != null && passwAccessMode.getMode() ==
                        CryptoDeviceAccessMode.Mode.PIN) {
                    radio_group.check(radio_pin.getId());
                    radio_pin.setChecked(true);
                    return;
                }
                if(checkConnection()) {
                    intent = new Intent(this, PinActivity.class);
                    intent.putExtra(ContextVS.MODE_KEY, PinActivity.MODE_CHANGE_PASSWORD);
                    startActivityForResult(intent, RC_PIN);
                }
                break;
            case R.id.radio_pattern:
                if(passwAccessMode != null && passwAccessMode.getMode() ==
                        CryptoDeviceAccessMode.Mode.PATTER_LOCK)  {
                    radio_group.check(radio_pattern.getId());
                    return;
                }
                if(checkConnection()) {
                    intent = new Intent(this, PatternLockActivity.class);
                    intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_CHANGE_PASSWORD);
                    intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                    startActivityForResult(intent, RC_PATTERN_LOCK);
                }
                break;
        }
    }

    private boolean checkConnection() {
        if(!PrefUtils.isDNIeEnabled()) return true;
        if(!AppVS.getInstance().isWithSocketConnection()) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ConnectionUtils.initConnection(CryptoDeviceAccessModeSelectorActivity.this);
                            dialog.dismiss();
                        }
                    });
            DialogButton negativeButton = new DialogButton(getString(R.string.cancel_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.connect_lbl), getString(R.string.connection_required_msg),
                    positiveButton, negativeButton, this);
            return false;
        } else return true;
    }

    private void updateView() {
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        radio_group.clearCheck();
        if(passwAccessMode != null) {
            switch (passwAccessMode.getMode()) {
                case PATTER_LOCK:
                    radio_group.check(radio_pattern.getId());
                    break;
                case PIN:
                    radio_group.check(radio_pin.getId());
                    break;
            }
        }

    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        updateView();
        switch (requestCode) {
            case RC_PATTERN_LOCK:
            case RC_PIN:
                if(Activity.RESULT_OK == resultCode) {
                    if(getCallingActivity() != null) {
                        setResult(Activity.RESULT_OK, data);
                        finish();
                    }
                }
                break;
        }

    }
}
