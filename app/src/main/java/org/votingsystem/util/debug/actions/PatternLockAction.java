package org.votingsystem.util.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.votingsystem.AppVS;
import org.votingsystem.activity.PatternLockActivity;
import org.votingsystem.android.R;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class PatternLockAction implements DebugAction {

    private static final String TAG = PatternLockAction.class.getSimpleName();

    @Override public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(AppVS.getInstance(), PatternLockActivity.class);
                intent.putExtra(ContextVS.MESSAGE_KEY, context.getString(R.string.request_pattern_lock_msg));
                intent.putExtra(ContextVS.PASSWORD_CONFIRM_KEY, true);
                intent.putExtra(ContextVS.MODE_KEY, PatternLockActivity.MODE_VALIDATE_USER_INPUT_PATTERN);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                AppVS.getInstance().startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "Pattern lock view";
    }

}