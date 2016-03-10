package org.votingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.fragment.RepresentationStateFragment;
import org.votingsystem.fragment.RepresentativeGridFragment;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativesMainActivity extends ActivityBase {

	public static final String TAG = RepresentativesMainActivity.class.getSimpleName();

    public static final int RC_PASSW                       = 0;
    public static final int RC_SIGN_REQUEST = 1;

    private WeakReference<RepresentationStateFragment> stateFragment;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getString(R.string.representatives_drop_down_lbl));
        stateFragment = new WeakReference<RepresentationStateFragment>(new RepresentationStateFragment());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                stateFragment.get(), RepresentationStateFragment.TAG).commit();
        setMenu(R.menu.drawer_voting);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        AlertDialog.Builder builder = null;
        switch (item.getItemId()) {
            case R.id.cancel_anonymouys_representation:
                builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.cancel_anonymouys_representation_lbl),
                        getString(R.string.cancel_anonymouys_representation_msg), this);
                builder.setPositiveButton(getString(R.string.continue_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Utils.getProtectionPassword(RC_PASSW, null, null,
                                        RepresentativesMainActivity.this);
                            }
                        });
                UIUtils.showMessageDialog(builder);
                return true;
            case R.id.representative_list:
                Intent intent = new Intent(this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, RepresentativeGridFragment.class.getName());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.removeGroup(R.id.general_items);
        inflater.inflate(R.menu.representative_main, menu);
        RepresentationStateDto representation = PrefUtils.getRepresentationState();
        if(representation != null) {
            switch(representation.getState()) {
                case REPRESENTATIVE:
                    menu.removeGroup(R.id.options_for_uservs);
                    break;
                case WITHOUT_REPRESENTATION:
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
            }
        } else {
            menu.removeGroup(R.id.options_for_uservs);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if (isVisible) ProgressDialogFragment.showDialog(
                caption, message, getSupportFragmentManager());
        else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    RepresentativeDelegationDto delegation = PrefUtils.getAnonymousDelegation();
                    if(delegation == null) {
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                                getString(R.string.missing_anonymous_delegation_cancellation_data), 
                                getSupportFragmentManager());
                    } else {
                        try {
                            RepresentativeDelegationDto request = 
                                    delegation.getAnonymousCancelationRequest();
                            Intent intent = new Intent(this, ID_CardNFCReaderActivity.class);
                            intent.putExtra(ContextVS.PASSWORD_KEY, new
                                    String(responseVS.getMessageBytes()).toCharArray());
                            intent.putExtra(ContextVS.USER_KEY,
                                    AppVS.getInstance().getAccessControl().getName());
                            intent.putExtra(ContextVS.MESSAGE_KEY, 
                                    getString(R.string.anonymous_delegation_cancellation_lbl));
                            intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, 
                                    JSON.writeValueAsString(request));
                            intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY,
                                    getString(R.string.anonymous_delegation_cancellation_lbl));
                            startActivityForResult(intent, RC_SIGN_REQUEST);    
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
                break;
            case RC_SIGN_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                    new AnonymousDelegationCancellationTask(responseVS.getCMS()).execute();
                }
                break;
        }
    }

    public class AnonymousDelegationCancellationTask extends AsyncTask<String, String, ResponseVS> {

        private CMSSignedMessage cmsSignedMessage;
        
        public AnonymousDelegationCancellationTask(CMSSignedMessage cmsMessage) {
            this.cmsSignedMessage = cmsMessage;
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.cancel_anonymouys_representation_lbl), getString(R.string.wait_msg)); }

        @Override protected ResponseVS doInBackground(String... urls) {
            ResponseVS responseVS = null;
            RepresentativeDelegationDto delegation = PrefUtils.getAnonymousDelegation();
            RepresentativeDelegationDto cancelationRequest =
                    delegation.getAnonymousRepresentationDocumentCancelationRequest();
            try {
                CMSSignedMessage anonymousCMSMessage = delegation.getCertificationRequest().signData(
                        JSON.getMapper().writeValueAsString(cancelationRequest));
                MessageTimeStamper timeStamper = new MessageTimeStamper(anonymousCMSMessage,
                        AppVS.getInstance().getAccessControl().getTimeStampServiceURL());
                anonymousCMSMessage = timeStamper.call();
                Map<String, Object> mapToSend = new HashMap<>();
                mapToSend.put(ContextVS.CMS_FILE_NAME, cmsSignedMessage.toPEM());
                mapToSend.put(ContextVS.CMS_ANONYMOUS_FILE_NAME, anonymousCMSMessage.toPEM());
                responseVS =  HttpHelper.sendObjectMap(mapToSend,
                        AppVS.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    CMSSignedMessage delegationReceipt = responseVS.getCMS();
                    Collection matches = delegationReceipt.checkSignerCert(
                            AppVS.getInstance().getAccessControl().getCertificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                    responseVS.setCMS(delegationReceipt);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, RepresentativesMainActivity.this);
            } finally {
                return responseVS;
            }
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            setProgressDialogVisible(false, null, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PrefUtils.putAnonymousDelegation(null);
                responseVS.setCaption(getString(R.string.cancel_anonymouys_representation_lbl)).
                        setNotificationMessage(getString(R.string.cancel_anonymous_representation_ok_msg));
                if(stateFragment.get() != null)
                    stateFragment.get().launchRepresentativeService(TypeVS.STATE);
            }
            MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
        }
    }

}