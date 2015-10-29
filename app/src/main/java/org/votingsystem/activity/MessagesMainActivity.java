package org.votingsystem.activity;

import android.os.Bundle;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.fragment.MessagesGridFragment;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessagesMainActivity extends ActivityBase {

    public static final String TAG = MessagesMainActivity.class.getSimpleName();

    private WeakReference<MessagesGridFragment> messagesGridRef;
    private AppVS appVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getApplicationContext();
        MessagesGridFragment fragment = new MessagesGridFragment();
        messagesGridRef = new WeakReference<MessagesGridFragment>(fragment);
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object)fragment).getClass().getSimpleName()).commit();
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        getSupportActionBar().setLogo(null);
        getSupportActionBar().setTitle(getString(R.string.messages_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}