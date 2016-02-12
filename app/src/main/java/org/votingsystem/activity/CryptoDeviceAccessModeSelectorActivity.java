package org.votingsystem.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;

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
    private CryptoDeviceAccessMode accessMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_device_access_mode_selector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.crypto_device_access_mode_lbl));
        pinSwitch = (Switch) findViewById(R.id.pinSwitch);
        pinSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this,
                            PinInputActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.request_pattern_lock_msg));
                    intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_SET_PATTERN);
                    intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                    startActivityForResult(intent, PIN);
                }else{

                }
            }
        });
        patternLockSwitch = (Switch) findViewById(R.id.patternLockSwitch);
        patternLockSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this, PatternLockActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.request_pattern_lock_msg));
                    intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_SET_PATTERN);
                    intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                    startActivityForResult(intent, PATTERN_LOCK);
                } else {

                }
            }
        });
        updateUI();
    }

    private void updateUI() {
        accessMode = PrefUtils.getCryptoDeviceAccessMode();
        if(accessMode != null) {
            switch (accessMode.getMode()) {
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
                if(data == null || Activity.RESULT_OK != resultCode) {
                    patternLockSwitch.setChecked(false);
                    return;
                } else {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                }
                break;
        }
        updateUI();
    }
}
