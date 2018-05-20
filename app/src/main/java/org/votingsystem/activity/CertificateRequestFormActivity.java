package org.votingsystem.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.votingsystem.android.R;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.QRActionsFragment;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificateRequestFormActivity extends AppCompatActivity {

    public static final String TAG = CertificateRequestFormActivity.class.getSimpleName();

    private EditText nameText;
    private EditText surnameText;
    private EditText phoneText;

    private String broadCastId = CertificateRequestFormActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreate", "onCreate");
        setContentView(R.layout.cert_request_form);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.cert_request_lbl));

        nameText = (EditText) findViewById(R.id.name);
        surnameText = (EditText) findViewById(R.id.surname);
        phoneText = (EditText) findViewById(R.id.phone);
        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                validateForm();
            }
        });
    }

    private void validateForm() {
        nameText.setError(null);
        surnameText.setError(null);
        phoneText.setError(null);
        if (TextUtils.isEmpty(nameText.getText().toString())) {
            nameText.setError(getString(R.string.name_lbl));
        }
        if (TextUtils.isEmpty(surnameText.getText().toString())) {
            surnameText.setError(getString(R.string.surname_lbl));
        }
        if (TextUtils.isEmpty(phoneText.getText().toString())) {
            phoneText.setError(getString(R.string.can_dialog_caption));
        }
        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                getString(R.string.user_data_form_lbl),
                getString(R.string.user_data_confirm_msg, nameText.getText().toString(),
                surnameText.getText().toString(), phoneText.getText().toString()), this);
        builder.setPositiveButton(getString(R.string.continue_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setProgressDialogVisible(true, getString(R.string.connecting_caption),
                                getString(R.string.wait_msg));
                    }
                });
        UIUtils.showMessageDialog(builder);
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getSupportFragmentManager());
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

    public class PostDataTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = QRActionsFragment.PostDataTask.class.getSimpleName();

        private String systemServiceURL;
        private byte[] requestContent;
        private ContentType contentType;

        public PostDataTask(byte[] requestContent, String systemServiceURL, ContentType contentType) {
            this.requestContent = requestContent;
            this.systemServiceURL = systemServiceURL;
            this.contentType = contentType;
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg));
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            return HttpConn.getInstance().doPostRequest(requestContent, contentType, systemServiceURL);
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG + ".onPostExecute() ", " - statusCode: " + response.getStatusCode());
            if (ResponseDto.SC_OK == response.getStatusCode()) {
                setProgressDialogVisible(false, null, null);
                DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                UIUtils.showMessageDialog(getString(R.string.msg_lbl),
                        response.getMessage(), positiveButton, null,
                        CertificateRequestFormActivity.this);
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    response.getMessage(), getSupportFragmentManager());
        }
    }

}