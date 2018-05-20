package org.votingsystem.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.votingsystem.android.R;
import org.votingsystem.dto.CryptoDeviceAccessMode;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessModeSelectorActivity extends AppCompatActivity {

    public static final String TAG = CryptoDeviceAccessModeSelectorActivity.class.getSimpleName();

    public static final int RC_PATERN_LOCK = 0;
    public static final int RC_PIN         = 1;

    private CryptoDeviceAccessMode passwAccessMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_device_access_mode_selector);
        final RadioGroup radio_group = (RadioGroup) findViewById(R.id.radio_group);
        RadioButton radio_pin = (RadioButton) findViewById(R.id.radio_pin);
        RadioButton radio_pattern = (RadioButton) findViewById(R.id.radio_pattern);
        RadioButton radio_dnie = (RadioButton) findViewById(R.id.radio_dnie);
        UIUtils.setSupportActionBar(this, getString(R.string.crypto_device_access_mode_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        passwAccessMode = PrefUtils.getCryptoDeviceAccessMode();
        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int radioButtonID = radio_group.getCheckedRadioButtonId();
                switch (radioButtonID) {
                    case R.id.radio_pin: {
                        if (passwAccessMode == null || passwAccessMode.getMode() !=
                                CryptoDeviceAccessMode.Mode.PIN) {
                            Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this,
                                    PinActivity.class);
                            intent.putExtra(Constants.MODE_KEY, PinActivity.MODE_CHANGE_PASSWORD);
                            startActivityForResult(intent, RC_PIN);
                        }
                        finish();
                        break;
                    }
                    case R.id.radio_pattern:
                        if (passwAccessMode == null || passwAccessMode.getMode() !=
                                CryptoDeviceAccessMode.Mode.PATTER_LOCK) {
                            Intent intent = new Intent(CryptoDeviceAccessModeSelectorActivity.this,
                                    PatternLockActivity.class);
                            intent.putExtra(Constants.MODE_KEY, PatternLockActivity.MODE_CHANGE_PASSWORD);
                            intent.putExtra(Constants.PASSWORD_CONFIRM_KEY, true);
                            startActivityForResult(intent, RC_PATERN_LOCK);
                        } else finish();
                        break;
                    case R.id.radio_dnie:
                        PrefUtils.putCryptoDeviceAccessMode(new CryptoDeviceAccessMode(
                                CryptoDeviceAccessMode.Mode.DNIE_PASSW, null));
                        setResult(Activity.RESULT_CANCELED, null);
                        finish();
                        break;
                    default:
                        LOGD(TAG, "OnClick - unknown radioButtonID: " + radioButtonID);
                }
            }
        });
        if (passwAccessMode != null) {
            switch (passwAccessMode.getMode()) {
                case PATTER_LOCK:
                    radio_group.check(radio_pattern.getId());
                    break;
                case PIN:
                    radio_group.check(radio_pin.getId());
                    break;
                case DNIE_PASSW:
                    radio_group.check(radio_dnie.getId());
                    break;
            }
        } else radio_group.check(radio_dnie.getId());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
