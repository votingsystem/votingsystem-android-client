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
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.ReceiptFragment;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.InputFilterMinMax;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
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
        representative = (UserDto) getIntent().getSerializableExtra(ContextVS.USER_KEY);
        setContentView(R.layout.representative_delegation);
        UIUtils.setSupportActionBar(this, getString(R.string.representative_delegation_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        weeks_delegation = (EditText)findViewById(R.id.weeks_delegation);
        EditText et = (EditText) findViewById(R.id.weeks_delegation);
        et.setFilters(new InputFilter[]{
                new InputFilterMinMax(1, ContextVS.MAX_WEEKS_ANONYMOUS_DELEGATION)});
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
                    ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY);
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
                        ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                        delegationDto = new RepresentativeDelegationDto();
                        delegationDto.setServerURL(AppVS.getInstance().getAccessControl().getServerURL());
                        delegationDto.setOperation(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
                        delegationDto.setRepresentative(representative);
                        delegationDto.setWeeksOperationActive(
                                Integer.valueOf(weeks_delegation.getText().toString()));
                        RepresentativeDelegationDto anonymousCertRequest = delegationDto.getAnonymousCertRequest();
                        Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                        intent.putExtra(ContextVS.PASSWORD_KEY, new
                                String(responseVS.getMessageBytes()).toCharArray());
                        intent.putExtra(ContextVS.USER_KEY,
                                AppVS.getInstance().getAccessControl().getName());
                        intent.putExtra(ContextVS.MESSAGE_KEY,
                                getString(R.string.anonimous_representative_request_lbl));
                        intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY,
                                JSON.writeValueAsString(anonymousCertRequest));
                        intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY,
                                getString(R.string.anonimous_representative_request_lbl));
                        startActivityForResult(intent, RC_SIGN_ANONYMOUS_CERT_REQUEST);
                    } catch ( Exception ex) { ex.printStackTrace();}
                }
                break;
            case RC_SIGN_ANONYMOUS_CERT_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    new AnonymousDelegationTask(responseVS.getCMS()).execute();
                }
                break;
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, delegationDto);
    }

    public class AnonymousDelegationTask extends AsyncTask<String, String, ResponseVS> {

        private CMSSignedMessage cmsMessage;

        public AnonymousDelegationTask(CMSSignedMessage cmsMessage) {
            this.cmsMessage = cmsMessage;
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.representative_delegation_lbl), getString(R.string.wait_msg)); }

        @Override protected ResponseVS doInBackground(String... urls) {
            ResponseVS responseVS = null;
            try {
                RepresentativeDelegationDto anonymousDelegationRequest = delegationDto.getDelegation();
                delegationDto.setAnonymousDelegationRequestBase64ContentDigest(cmsMessage.getContentDigestStr());
                Map<String, Object> mapToSend = new HashMap<>();
                mapToSend.put(ContextVS.CSR_FILE_NAME, delegationDto.getCertificationRequest().getCsrPEM());
                mapToSend.put(ContextVS.CMS_FILE_NAME, cmsMessage.toPEM());
                responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                        AppVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    delegationDto.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                    //this is the delegation request signed with anonymous cert
                    byte[] contentToSign = JSON.getMapper().writeValueAsBytes(anonymousDelegationRequest);
                    String signatureMechanism =
                            delegationDto.getCertificationRequest().getSignatureMechanism();
                    TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(
                            signatureMechanism, contentToSign);
                    cmsMessage = delegationDto.getCertificationRequest().signData(
                            contentToSign, timeStampToken);
                    responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                            AppVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        delegationDto.setDelegationReceipt(responseVS.getCMS(),
                                AppVS.getInstance().getAccessControl().getCertificate());
                        CMSSignedMessage delegationReceipt = CMSSignedMessage.FROM_PEM(responseVS.getMessageBytes());
                        Collection matches = delegationReceipt.checkSignerCert(
                                AppVS.getInstance().getAccessControl().getCertificate());
                        if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");

                        PrefUtils.putAnonymousDelegation(delegationDto);
                        responseVS.setCaption(getString(R.string.anonymous_delegation_caption))
                                .setNotificationMessage(getString(R.string.anonymous_delegation_msg,
                                        delegationDto.getRepresentative().getName(),
                                        delegationDto.getWeeksOperationActive()));
                    }
                } else {
                    responseVS.setCaption(getString(R.string.error_lbl));
                    if(ContentType.JSON == responseVS.getContentType()) {
                        MessageDto messageDto = (MessageDto) responseVS.getMessage(MessageDto.class);
                        responseVS.setNotificationMessage(messageDto.getMessage());
                        responseVS.setData(messageDto.getURL());
                    } else responseVS.setNotificationMessage(responseVS.getMessage());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, RepresentativeDelegationActivity.this);
            } finally {
                return responseVS;
            }
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(final ResponseVS responseVS) {
            setProgressDialogVisible(false, null, null);
            if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.error_lbl),
                        responseVS.getNotificationMessage(), RepresentativeDelegationActivity.this).
                        setPositiveButton(getString(R.string.open_receipt_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(getApplicationContext(), FragmentContainerActivity.class);
                                        intent.putExtra(ContextVS.URL_KEY, (String) responseVS.getData());
                                        intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                        startActivity(intent);
                                    }
                                });
                UIUtils.showMessageDialog(builder);
            } else {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(responseVS.getCaption(),
                        responseVS.getNotificationMessage(),  RepresentativeDelegationActivity.this).
                        setPositiveButton(getString(R.string.accept_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent resultIntent = new Intent(
                                                RepresentativeDelegationActivity.this, ActivityBase.class);
                                        resultIntent.putExtra(ContextVS.FRAGMENT_KEY, R.id.representatives);
                                        startActivity(resultIntent);
                                        RepresentativeDelegationActivity.this.finish();
                                    }
                                });
                UIUtils.showMessageDialog(builder);
            }
        }
    }
    
}