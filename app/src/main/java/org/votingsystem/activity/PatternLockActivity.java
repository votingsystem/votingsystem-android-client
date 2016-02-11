package org.votingsystem.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.ui.PatternLockView;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

public class PatternLockActivity extends AppCompatActivity {

    private static final String TAG = PatternLockActivity.class.getSimpleName();

    public static final int DNIE_PASSWORD_REQUEST      = 0;

    public static final int MODE_VALIDATE_USER_INPUT_PATTERN = 0;
    public static final int MODE_SET_PATTERN                 = 1;
    private int requestMode;

    private String broadCastId = PatternLockActivity.class.getSimpleName();
    private PatternLockView mCircleLockView;
    private TextView mPasswordTextView;
    private Boolean withPasswordConfirm;
    private String first_pattern_input;
    private char[] password;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            final ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                if(ResponseVS.SC_CANCELED == responseVS.getStatusCode()) {
                    showPasswordRequiredDialog();
                } else {
                    switch(responseVS.getTypeVS()) {
                        case PIN:
                            password = new String(responseVS.getMessageBytes()).toCharArray() ;
                            break;
                    }
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        mCircleLockView = (PatternLockView) findViewById(R.id.lock_view_circle);
        mPasswordTextView = (TextView) findViewById(R.id.password_text);
        mPasswordTextView.setText(getIntent().getExtras().getString(ContextVS.MESSAGE_KEY));
        withPasswordConfirm = getIntent().getExtras().getBoolean(ContextVS.PASSWORD_CONFIRM_KEY, false);
        mCircleLockView.setCallBack(new PatternLockView.CallBack() {
            @Override
            public int onFinish(PatternLockView.Password password) {
                LOGD(TAG, "password length " + password.list.size());
                if(withPasswordConfirm) {
                    if(first_pattern_input == null) {
                        first_pattern_input = password.string;
                        mPasswordTextView.setText(getString(R.string.confirm_lock_msg));
                        return PatternLockView.CODE_PASSWORD_CORRECT;
                    } else {
                        if(first_pattern_input.equals(password.string)) {
                            processResult(password.string);
                            return PatternLockView.CODE_PASSWORD_CORRECT;
                        } else {
                            first_pattern_input = null;
                            mPasswordTextView.setText(getString(R.string.retry_msg));
                            return PatternLockView.CODE_PASSWORD_ERROR;
                        }
                    }
                } else {
                    processResult(password.string);
                    return PatternLockView.CODE_PASSWORD_CORRECT;
                }
            }
        });
        mCircleLockView.setOnNodeTouchListener(new PatternLockView.OnNodeTouchListener() {
            @Override public void onNodeTouched(int NodeId) {
                LOGD(TAG, "node " + NodeId + " has touched!");
            }
        });
        requestMode = getIntent().getExtras().getInt(ContextVS.MODE_KEY);
        switch (requestMode) {
            case MODE_VALIDATE_USER_INPUT_PATTERN:
                break;
            case MODE_SET_PATTERN:
                if(PrefUtils.isDNIeEnabled()) {
                    Intent intent = new Intent(this, DNIeSigningActivity.class);
                    intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.enter_password_for_dni_lock_msg));
                    intent.putExtra(ContextVS.MODE_KEY, DNIeSigningActivity.MODE_PASSWORD_REQUEST);
                    startActivityForResult(intent, DNIE_PASSWORD_REQUEST);
                } else {
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                            getString(R.string.enter_pin_msg), false, TypeVS.PIN);
                }
                break;
        }
    }

    private void processResult(String patternPassword) {
        Intent resultIntent = null;
        switch (requestMode) {
            case MODE_VALIDATE_USER_INPUT_PATTERN:
                try {
                    PrefUtils.checkLockPatternHash(patternPassword, this);
                } catch(ExceptionVS ex) {
                    ex.printStackTrace();
                    mPasswordTextView.setText(ex.getMessage());
                    first_pattern_input = null;
                    return;
                }
                resultIntent = new Intent();
                resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, ResponseVS.OK()
                        .setMessageBytes(patternPassword.getBytes()));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                break;
            case MODE_SET_PATTERN:
                PrefUtils.putLockPatterProtectedPassword(patternPassword, password);
                resultIntent = new Intent();
                resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, ResponseVS.OK()
                        .setMessageBytes(patternPassword.getBytes()));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                break;
        }
    }

    private void showPasswordRequiredDialog() {
        DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, resultIntent);
                        finish();
                    }
                });
        UIUtils.showMessageDialog(getString(R.string.error_lbl),
                getString(R.string.pattern_lock_missing_passw_msg), positiveButton, null, this);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " + resultCode);
        if(data == null) {
            showPasswordRequiredDialog();
            return;
        }
        final ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(Activity.RESULT_OK == resultCode && requestCode == DNIE_PASSWORD_REQUEST) {
            password = new String(responseVS.getMessageBytes()).toCharArray();
        }
    }

    @Override public void onBackPressed() {
        super.onBackPressed();
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}
