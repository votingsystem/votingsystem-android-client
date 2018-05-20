package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DNIeCANFormActivity extends AppCompatActivity {

    public static final String TAG = DNIeCANFormActivity.class.getSimpleName();

    private EditText canText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreate", "onCreate");
        setContentView(R.layout.dnie_can_form);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.user_data_lbl));


        final TextView messageTextView = (TextView) findViewById(R.id.message);
        final ImageView can_info_image = (ImageView) findViewById(R.id.can_info_image);
        messageTextView.setText(getString(R.string.can_question_msg));
        messageTextView.setMovementMethod(new LinkMovementMethod());
        messageTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                can_info_image.setVisibility(View.VISIBLE);
                messageTextView.setText(getString(R.string.can_info_msg));
            }
        });
        can_info_image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (can_info_image.getVisibility() == View.VISIBLE) {
                    can_info_image.setVisibility(View.GONE);
                    messageTextView.setText(getString(R.string.can_question_msg));
                }
            }
        });
        canText = (EditText) findViewById(R.id.can);
        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                checkDNIe();
            }
        });
        try {
            String can = PrefUtils.getDNIeCAN();
            if (can != null) canText.setText(can);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        canText.setError(null);
        if (TextUtils.isEmpty(canText.getText().toString())) {
            canText.setError(getString(R.string.can_dialog_caption));
            return false;
        }
        return true;
    }

    private void checkDNIe() {
        if (validateForm()) {
            String deviceId = PrefUtils.getDeviceId();
            LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.user_data_form_lbl),
                    getString(R.string.dnie_can_confirm_msg, canText.getText().toString()), this);
            builder.setPositiveButton(getString(R.string.continue_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            PrefUtils.putDNIeCAN(canText.getText().toString());
                            finish();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}