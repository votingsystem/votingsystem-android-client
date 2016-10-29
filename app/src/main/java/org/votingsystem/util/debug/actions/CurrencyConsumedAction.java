package org.votingsystem.util.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.votingsystem.App;
import org.votingsystem.activity.CurrencyConsumedGridActivity;
import org.votingsystem.util.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class CurrencyConsumedAction implements DebugAction {

    private static final String TAG = CurrencyConsumedAction.class.getSimpleName();

    @Override public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(App.getInstance(), CurrencyConsumedGridActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "currency consumed";
    }

}
