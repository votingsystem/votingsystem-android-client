package org.votingsystem.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessModeSelectorActivity  extends AppCompatActivity {

    public static final String TAG = CryptoDeviceAccessModeSelectorActivity.class.getSimpleName();

    public static final int PATTERN_LOCK = 0;
    public static final int PIN          = 1;

    private Switch pinSwitch;
    private Switch patternLockSwitch;
    private CryptoDeviceAccessMode passwAccessMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_device_access_mode_selector);
        UIUtils.setSupportActionBar(this, getString(R.string.crypto_device_access_mode_lbl));
        pinSwitch = (Switch) findViewById(R.id.pinSwitch);
        patternLockSwitch = (Switch) findViewById(R.id.patternLockSwitch);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        pinSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(passwAccessMode == null ||
                            passwAccessMode.getMode() != CryptoDeviceAccessMode.Mode.PIN){
                        Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this,
                                PinActivity.class);
                        intent.putExtra(ContextVS.MODE_KEY, PinActivity.MODE_CHANGE_PASSWORD);
                        startActivityForResult(intent, PIN);
                    }
                } else {
                    if(passwAccessMode != null &&
                            passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PIN)
                        pinSwitch.setChecked(true);
                }
            }
        });
        patternLockSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(passwAccessMode == null ||
                            passwAccessMode.getMode() != CryptoDeviceAccessMode.Mode.PATTER_LOCK){
                        Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this, PatternLockActivity.class);
                        intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_CHANGE_PASSWORD);
                        intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                        startActivityForResult(intent, PATTERN_LOCK);
                    }
                } else {
                    if(passwAccessMode != null &&
                            passwAccessMode.getMode() == CryptoDeviceAccessMode.Mode.PATTER_LOCK)
                        patternLockSwitch.setChecked(true);
                }
            }
        });
        updateUI();
    }

    private void updateUI() {
        patternLockSwitch.setChecked(false);
        pinSwitch.setChecked(false);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        if(passwAccessMode != null) {
            switch (passwAccessMode.getMode()) {
                case PATTER_LOCK:
                    patternLockSwitch.setChecked(true);
                    break;
                case PIN:
                    pinSwitch.setChecked(true);
                    break;
            }
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        switch (requestCode) {
            case PIN:
                break;
            case PATTERN_LOCK:
                break;
        }
        updateUI();
    }
}
