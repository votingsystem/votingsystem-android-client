package org.votingsystem.util.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.App;
import org.votingsystem.activity.BrowserActivity;
import org.votingsystem.util.Constants;
import org.votingsystem.util.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class BrowserVSAction implements DebugAction {

    private static final String TAG = BrowserVSAction.class.getSimpleName();

    public BrowserVSAction() {  }

    @Override public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                String targetURL = "https://192.168.1.5/CurrencyServer/";
                LOGD(TAG, "doInBackground - targetURL: " + targetURL);
                Intent intent = new Intent(App.getInstance(), BrowserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.URL_KEY, targetURL);
                App.getInstance().startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "BrowserVS";
    }

}
