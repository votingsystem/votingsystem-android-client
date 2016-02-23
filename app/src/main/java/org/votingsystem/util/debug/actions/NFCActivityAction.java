package org.votingsystem.util.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.votingsystem.AppVS;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.util.ContextVS;
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
                Intent intent = new Intent(appContext, ID_CardNFCReaderActivity.class);
                //intent.putExtra(ContextVS.OPERATIONVS_KEY, TypeVS.CURRENCY_REQUEST);
                intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, "message content");
                intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, "smime message subject");
                intent.putExtra(ContextVS.MESSAGE_KEY, "Do you want to sign the message?");
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
