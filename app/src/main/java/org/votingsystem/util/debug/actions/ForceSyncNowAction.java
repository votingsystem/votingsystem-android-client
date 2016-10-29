package org.votingsystem.util.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.App;
import org.votingsystem.util.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class ForceSyncNowAction implements DebugAction {

    private static final String TAG = ForceSyncNowAction.class.getSimpleName();

    private App appContext;

    public ForceSyncNowAction(App context) {
        this.appContext = context;
    }

    @Override public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "Force data sync now";
    }

}
