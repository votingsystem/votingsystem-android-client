package org.votingsystem.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.util.Constants;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageActivity extends AppCompatActivity {

    public static final String TAG = MessageActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_activity);
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                App.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        ResponseDto responseDto = getIntent().getParcelableExtra(Constants.RESPONSE_KEY);
        ((TextView) findViewById(R.id.caption_text)).setText(responseDto.getCaption());
        ((TextView) findViewById(R.id.message_text)).setText(Html.fromHtml(
                responseDto.getNotificationMessage()));
        ProgressDialogFragment.hide(getSupportFragmentManager());
    }

}