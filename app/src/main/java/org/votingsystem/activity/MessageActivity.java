package org.votingsystem.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageActivity extends AppCompatActivity {
	
	public static final String TAG = MessageActivity.class.getSimpleName();


    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.message_activity);
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                AppVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        ((TextView) findViewById(R.id.caption_text)).setText(responseVS.getCaption());
        ((TextView) findViewById(R.id.message_text)).setText(Html.fromHtml(
                responseVS.getNotificationMessage()));
        ProgressDialogFragment.hide(getSupportFragmentManager());
    }


}