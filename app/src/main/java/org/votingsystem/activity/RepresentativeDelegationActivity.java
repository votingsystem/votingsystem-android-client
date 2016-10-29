package org.votingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.ReceiptFragment;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.InputFilterMinMax;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.crypto.CMSUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeDelegationActivity extends AppCompatActivity {
	
	public static final String TAG = RepresentativeDelegationActivity.class.getSimpleName();

    public static final int RC_PASSW                       = 0;
    public static final int RC_SIGN_ANONYMOUS_CERT_REQUEST = 1;
    
    private EditText weeks_delegation;
    private UserDto representative = null;
    private RepresentativeDelegationDto delegationDto = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        representative = (UserDto) getIntent().getSerializableExtra(Constants.USER_KEY);
        setContentView(R.layout.representative_delegation);
        UIUtils.setSupportActionBar(this, getString(R.string.representative_delegation_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        weeks_delegation = (EditText)findViewById(R.id.weeks_delegation);
        EditText et = (EditText) findViewById(R.id.weeks_delegation);
        et.setFilters(new InputFilter[]{
                new InputFilterMinMax(1, Constants.MAX_WEEKS_ANONYMOUS_DELEGATION)});
        String fileToLoad = "delegation_message_" +
                Locale.getDefault().getLanguage().toLowerCase() + ".html";
        try {
            if(!Arrays.asList(getResources().getAssets().list("")).contains(fileToLoad)) {
                LOGD(TAG + ".onCreate", "missing fileToLoad: " + fileToLoad);
                fileToLoad = "delegation_message_es.html";
            }
        } catch(Exception ex) { ex.printStackTrace(); }
        WebView webView = (WebView)findViewById(R.id.webview);
        webView.setBackgroundColor(getResources().getColor(R.color.bkg_screen_vs));
        webView.loadUrl("file:///android_asset/" + fileToLoad);
        if(savedInstanceState != null) {
            delegationDto = (RepresentativeDelegationDto) savedInstanceState.getSerializable(
                    Constants.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY);
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible) ProgressDialogFragment.showDialog(
                caption, message, getSupportFragmentManager());
        else ProgressDialogFragment.hide(getSupportFragmentManager());
    }


    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.accept_button:
                if(TextUtils.isEmpty(weeks_delegation.getText())) {
                    weeks_delegation.setError(getString(R.string.anonymous_delegation_time_msg));
                    return;
                }
                Calendar calendar = DateUtils.getMonday(DateUtils.addDays(7));//7 -> next week monday
                Date anonymousDelegationFromDate = calendar.getTime();
                Integer weeksDelegation = Integer.valueOf(weeks_delegation.getText().toString());
                calendar.add(Calendar.DAY_OF_YEAR, weeksDelegation*7);
                Date anonymousDelegationToDate = calendar.getTime();
                String confirmDialogMsg = getString(R.string.anonymous_delegation_confirm_msg,
                        representative.getName(),  weeks_delegation.getText().toString(),
                        DateUtils.getDayWeekDateStr(anonymousDelegationFromDate, "HH:mm"),
                        DateUtils.getDayWeekDateStr(anonymousDelegationToDate, "HH:mm"));
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.representative_delegation_lbl), confirmDialogMsg, this);
                builder.setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Utils.getProtectionPassword(RC_PASSW, null, null,
                                        RepresentativeDelegationActivity.this);
                            }
                        });
                UIUtils.showMessageDialog(builder);
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    try {
                        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                        delegationDto = new RepresentativeDelegationDto();
                        delegationDto.setServerURL(App.getInstance().getAccessControl().getServerURL());
                        delegationDto.setOperation(OperationType.ANONYMOUS_REPRESENTATIVE_SELECTION);
                        delegationDto.setRepresentative(representative);
                        delegationDto.setWeeksOperationActive(
                                Integer.valueOf(weeks_delegation.getText().toString()));
                        RepresentativeDelegationDto anonymousCertRequest = delegationDto.getAnonymousCertRequest();
                        Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                        intent.putExtra(Constants.PASSWORD_KEY, new
                                String(responseDto.getMessageBytes()).toCharArray());
                        intent.putExtra(Constants.USER_KEY,
                                App.getInstance().getAccessControl().getName());
                        intent.putExtra(Constants.MESSAGE_KEY,
                                getString(R.string.anonimous_representative_request_lbl));
                        intent.putExtra(Constants.MESSAGE_CONTENT_KEY,
                                JSON.writeValueAsString(anonymousCertRequest));
                        intent.putExtra(Constants.MESSAGE_SUBJECT_KEY,
                                getString(R.string.anonimous_representative_request_lbl));
                        startActivityForResult(intent, RC_SIGN_ANONYMOUS_CERT_REQUEST);
                    } catch ( Exception ex) { ex.printStackTrace();}
                }
                break;
            case RC_SIGN_ANONYMOUS_CERT_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                    new AnonymousDelegationTask(responseDto.getCMS()).execute();
                }
                break;
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, delegationDto);
    }

    public class AnonymousDelegationTask extends AsyncTask<String, String, ResponseDto> {

        private CMSSignedMessage cmsMessage;

        public AnonymousDelegationTask(CMSSignedMessage cmsMessage) {
            this.cmsMessage = cmsMessage;
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.representative_delegation_lbl), getString(R.string.wait_msg)); }

        @Override protected ResponseDto doInBackground(String... urls) {
            ResponseDto responseDto = null;
            try {
                RepresentativeDelegationDto anonymousDelegationRequest = delegationDto.getDelegation();
                delegationDto.setAnonymousDelegationRequestBase64ContentDigest(cmsMessage.getContentDigestStr());
                Map<String, Object> mapToSend = new HashMap<>();
                mapToSend.put(Constants.CSR_FILE_NAME, delegationDto.getCertificationRequest().getCsrPEM());
                mapToSend.put(Constants.CMS_FILE_NAME, cmsMessage.toPEM());
                responseDto = HttpConnection.getInstance().sendObjectMap(mapToSend,
                        App.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
                if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
                    delegationDto.getCertificationRequest().initSigner(responseDto.getMessageBytes());
                    //this is the delegation request signed with anonymous cert
                    byte[] contentToSign = JSON.getMapper().writeValueAsBytes(anonymousDelegationRequest);
                    String signatureMechanism =
                            delegationDto.getCertificationRequest().getSignatureMechanism();
                    TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(
                            signatureMechanism, contentToSign);
                    cmsMessage = delegationDto.getCertificationRequest().signData(
                            contentToSign, timeStampToken);
                    responseDto = HttpConnection.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                            App.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
                    if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                        delegationDto.setDelegationReceipt(responseDto.getCMS(),
                                App.getInstance().getAccessControl().getCertificate());
                        CMSSignedMessage delegationReceipt = CMSSignedMessage.FROM_PEM(responseDto.getMessageBytes());
                        Collection matches = delegationReceipt.checkSignerCert(
                                App.getInstance().getAccessControl().getCertificate());
                        if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");

                        PrefUtils.putAnonymousDelegation(delegationDto);
                        responseDto.setCaption(getString(R.string.anonymous_delegation_caption))
                                .setNotificationMessage(getString(R.string.anonymous_delegation_msg,
                                        delegationDto.getRepresentative().getName(),
                                        delegationDto.getWeeksOperationActive()));
                    }
                } else {
                    responseDto.setCaption(getString(R.string.error_lbl));
                    if(ContentType.JSON == responseDto.getContentType()) {
                        MessageDto messageDto = (MessageDto) responseDto.getMessage(MessageDto.class);
                        responseDto.setNotificationMessage(messageDto.getMessage());
                        responseDto.setData(messageDto.getURL());
                    } else responseDto.setNotificationMessage(responseDto.getMessage());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseDto = ResponseDto.EXCEPTION(ex, RepresentativeDelegationActivity.this);
            } finally {
                return responseDto;
            }
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(final ResponseDto responseDto) {
            setProgressDialogVisible(false, null, null);
            if(ResponseDto.SC_ERROR_REQUEST_REPEATED == responseDto.getStatusCode()) {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.error_lbl),
                        responseDto.getNotificationMessage(), RepresentativeDelegationActivity.this).
                        setPositiveButton(getString(R.string.open_receipt_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(getApplicationContext(), FragmentContainerActivity.class);
                                        intent.putExtra(Constants.URL_KEY, (String) responseDto.getData());
                                        intent.putExtra(Constants.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                        startActivity(intent);
                                    }
                                });
                UIUtils.showMessageDialog(builder);
            } else {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(responseDto.getCaption(),
                        responseDto.getNotificationMessage(),  RepresentativeDelegationActivity.this).
                        setPositiveButton(getString(R.string.accept_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent resultIntent = new Intent(
                                                RepresentativeDelegationActivity.this, ActivityBase.class);
                                        resultIntent.putExtra(Constants.FRAGMENT_KEY, R.id.representatives);
                                        startActivity(resultIntent);
                                        RepresentativeDelegationActivity.this.finish();
                                    }
                                });
                UIUtils.showMessageDialog(builder);
            }
        }
    }
    
}