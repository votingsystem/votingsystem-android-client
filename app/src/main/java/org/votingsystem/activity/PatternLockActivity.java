package org.votingsystem.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.ui.PatternLockView;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;

public class PatternLockActivity extends Activity {

    private static final String TAG = PatternLockActivity.class.getSimpleName();

    private PatternLockView mCircleLockView;
    private TextView mPasswordTextView;
    private Boolean withPasswordConfirm = null;
    private String pattern = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        mCircleLockView = (PatternLockView) findViewById(R.id.lock_view_circle);
        mPasswordTextView = (TextView) findViewById(R.id.password_text);
        mPasswordTextView.setText(getIntent().getExtras().getString(ContextVS.MESSAGE_KEY));
        withPasswordConfirm = getIntent().getExtras().getBoolean(ContextVS.PASSWORD_CONFIRM_KEY);
        mCircleLockView.setCallBack(new PatternLockView.CallBack() {
            @Override
            public int onFinish(PatternLockView.Password password) {
                Log.d(TAG, "password length " + password.list.size());
                if(withPasswordConfirm) {
                    if(pattern == null) {
                        pattern = password.string;
                        mPasswordTextView.setText(getString(R.string.confirm_lock_msg));
                        return PatternLockView.CODE_PASSWORD_CORRECT;
                    } else {
                        if(pattern.equals(password.string)) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, ResponseVS.OK().setMessage(password.string));
                            setResult(Activity.RESULT_OK, resultIntent);
                            finish();
                            return PatternLockView.CODE_PASSWORD_CORRECT;
                        } else {
                            pattern = null;
                            mPasswordTextView.setText(getString(R.string.retry_msg));
                            return PatternLockView.CODE_PASSWORD_ERROR;
                        }
                    }
                } else {
                    return PatternLockView.CODE_PASSWORD_CORRECT;
                }
            }
        });
        mCircleLockView.setOnNodeTouchListener(new PatternLockView.OnNodeTouchListener() {
            @Override
            public void onNodeTouched(int NodeId) {
            Log.d(TAG, "node " + NodeId + " has touched!");
            }
        });
    }

}
