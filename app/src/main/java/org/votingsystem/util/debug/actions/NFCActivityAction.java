package org.votingsystem.util.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.votingsystem.AppVS;
import org.votingsystem.activity.DNIeVotingActivity;
import org.votingsystem.util.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class NFCActivityAction implements DebugAction {
    private static final String TAG = NFCActivityAction.class.getSimpleName();

    private AppVS appContext;

    public NFCActivityAction(AppVS context) {
        this.appContext = context;
    }

    @Override public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(appContext, DNIeVotingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "NFCActivityAction";
    }

}
