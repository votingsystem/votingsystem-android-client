package org.votingsystem.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.App;
import org.votingsystem.activity.BrowserActivity;
import org.votingsystem.debug.DebugAction;
import org.votingsystem.util.Constants;

import static org.votingsystem.util.LogUtils.LOGD;

public class BrowserAction implements DebugAction {

    private static final String TAG = BrowserAction.class.getSimpleName();

    public BrowserAction() {
    }

    @Override
    public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                String targetURL = "https://voting.ddns.net/voting-service/test/android.xhtml";
                LOGD(TAG, "doInBackground - targetURL: " + targetURL);
                Intent intent = new Intent(App.getInstance(), BrowserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.URL_KEY, targetURL);
                App.getInstance().startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "BrowserVS";
    }

}
