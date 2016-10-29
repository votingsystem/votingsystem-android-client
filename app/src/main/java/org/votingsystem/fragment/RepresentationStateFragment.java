package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.App;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.crypto.CMSUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentationStateFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = RepresentationStateFragment.class.getSimpleName();

    public static final int RC_PASSW        = 0;
    public static final int RC_SIGN_REQUEST = 1;

    private RepresentationStateDto representation;
    private RepresentativeDelegationDto representativeDelegationDto;
    private View rootView;
    private String broadCastId = RepresentationStateFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
            setProgressDialogVisible(false);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                switch(responseDto.getOperationType()) {
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                        MessageDialogFragment.showDialog(responseDto, getFragmentManager());
                        break;
                }
            } else if(ResponseDto.SC_OK != responseDto.getStatusCode()) MessageDialogFragment.showDialog(
                    responseDto, getFragmentManager());
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        rootView = inflater.inflate(R.layout.representative_state, container, false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.user_representative_lbl);
        setRepresentationView(PrefUtils.getRepresentationState());
        setHasOptionsMenu(true);
        return rootView;
    }

    private void setRepresentationView(RepresentationStateDto representation) {
        this.representation = representation;
        if(representation == null) {
            if(App.getInstance().getUser() != null) launchRepresentativeService(OperationType.STATE);
            rootView.setVisibility(View.GONE);
            return;
        } else {
            rootView.setVisibility(View.VISIBLE);
            ((TextView)rootView.findViewById(R.id.last_checked_date)).setText(getString(
                    R.string.representation_last_checked_msg,
                    DateUtils.getDayWeekDateStr(representation.getLastCheckedDate(), "HH:mm")));
        }
        switch(representation.getState()) {
            case WITHOUT_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.without_representative_msg));
                break;
            case WITH_ANONYMOUS_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.with_anonymous_representation_msg, DateUtils.getDayWeekDateStr(
                                representation.getDateTo(), "HH:mm")));
                representativeDelegationDto = PrefUtils.getAnonymousDelegation();
                if(representativeDelegationDto == null) {
                    ((TextView)rootView.findViewById(R.id.representative_name)).setText(
                            getString(R.string.missing_anonymous_delegation_cancellation_data));
                }
                break;
            case REPRESENTATIVE:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.representative_msg));
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.removeGroup(R.id.general_items);
        menuInflater.inflate(R.menu.representative_main, menu);
        RepresentationStateDto representation = PrefUtils.getRepresentationState();
        if(representation != null) {
            switch(representation.getState()) {
                case REPRESENTATIVE:
                    menu.removeGroup(R.id.options_for_user);
                    break;
                case WITHOUT_REPRESENTATION:
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
            }
        } else {
            menu.removeGroup(R.id.options_for_user);
        }

        if(representation == null) return;

        switch(representation.getState()) {
            case WITH_ANONYMOUS_REPRESENTATION:
                if(representativeDelegationDto == null)
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                break;
            default:
                menu.removeItem(R.id.cancel_anonymouys_representation);
        }
    }

    public void launchRepresentativeService(OperationType operationType) {
        LOGD(TAG + ".launchRepresentativeService", "operation:" + operationType.toString());
        Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
        startIntent.putExtra(Constants.TYPEVS_KEY, operationType);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true);
        getActivity().startService(startIntent);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if (isVisible) ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onResume() {
        super.onResume();
        PrefUtils.registerPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        PrefUtils.unregisterPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog.Builder builder = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                return true;
            case R.id.check_representation_state:
                launchRepresentativeService(OperationType.STATE);
                return true;
            case R.id.cancel_anonymouys_representation:
                builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.cancel_anonymouys_representation_lbl),
                        getString(R.string.cancel_anonymouys_representation_msg), getActivity());
                builder.setPositiveButton(getString(R.string.continue_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Utils.getProtectionPassword(RC_PASSW, null, null,
                                        (AppCompatActivity)getActivity());
                            }
                        });
                UIUtils.showMessageDialog(builder);
                return true;
            case R.id.representative_list:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(Constants.FRAGMENT_KEY, RepresentativeGridFragment.class.getName());
                startActivity(intent);
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
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                    RepresentativeDelegationDto delegation = PrefUtils.getAnonymousDelegation();
                    if(delegation == null) {
                        MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.error_lbl),
                                getString(R.string.missing_anonymous_delegation_cancellation_data),
                                getFragmentManager());
                    } else {
                        try {
                            RepresentativeDelegationDto request =
                                    delegation.getAnonymousCancelationRequest();
                            Intent intent = new Intent(getActivity(), ID_CardNFCReaderActivity.class);
                            intent.putExtra(Constants.PASSWORD_KEY, new
                                    String(responseDto.getMessageBytes()).toCharArray());
                            intent.putExtra(Constants.USER_KEY,
                                    App.getInstance().getAccessControl().getName());
                            intent.putExtra(Constants.MESSAGE_KEY,
                                    getString(R.string.anonymous_delegation_cancellation_lbl));
                            intent.putExtra(Constants.MESSAGE_CONTENT_KEY,
                                    JSON.writeValueAsString(request));
                            intent.putExtra(Constants.MESSAGE_SUBJECT_KEY,
                                    getString(R.string.anonymous_delegation_cancellation_lbl));
                            startActivityForResult(intent, RC_SIGN_REQUEST);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
                break;
            case RC_SIGN_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                    new AnonymousDelegationCancellationTask(responseDto.getCMS()).execute();
                }
                break;
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(RepresentationStateDto.class.getSimpleName().equals(key)) {
            LOGD(TAG + ".onSharedPreferenceChanged", "key: " + key);
            setRepresentationView(PrefUtils.getRepresentationState());
        }
    }

    public class AnonymousDelegationCancellationTask extends AsyncTask<String, String, ResponseDto> {

        private CMSSignedMessage cmsSignedMessage;

        public AnonymousDelegationCancellationTask(CMSSignedMessage cmsMessage) {
            this.cmsSignedMessage = cmsMessage;
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.cancel_anonymouys_representation_lbl), getString(R.string.wait_msg)); }

        @Override protected ResponseDto doInBackground(String... urls) {
            ResponseDto responseDto = null;
            RepresentativeDelegationDto delegation = PrefUtils.getAnonymousDelegation();
            RepresentativeDelegationDto cancelationRequest =
                    delegation.getAnonymousRepresentationDocumentCancelationRequest();
            try {
                byte[] contentToSign = JSON.getMapper().writeValueAsBytes(cancelationRequest);
                TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(
                        delegation.getCertificationRequest().getSignatureMechanism(), contentToSign);
                CMSSignedMessage anonymousCMSMessage = delegation.getCertificationRequest().signData(
                        contentToSign, timeStampToken);
                Map<String, Object> mapToSend = new HashMap<>();
                mapToSend.put(Constants.CMS_FILE_NAME, cmsSignedMessage.toPEM());
                mapToSend.put(Constants.CMS_ANONYMOUS_FILE_NAME, anonymousCMSMessage.toPEM());
                responseDto =  HttpConnection.getInstance().sendObjectMap(mapToSend,
                        App.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
                if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
                    CMSSignedMessage delegationReceipt = responseDto.getCMS();
                    Collection matches = delegationReceipt.checkSignerCert(
                            App.getInstance().getAccessControl().getCertificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                    responseDto.setCMS(delegationReceipt);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseDto = ResponseDto.EXCEPTION(ex, getActivity());
            } finally {
                return responseDto;
            }
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            setProgressDialogVisible(false, null, null);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                PrefUtils.putAnonymousDelegation(null);
                responseDto.setCaption(getString(R.string.cancel_anonymouys_representation_lbl)).
                        setNotificationMessage(getString(R.string.cancel_anonymous_representation_ok_msg));
                launchRepresentativeService(OperationType.STATE);
            }
            MessageDialogFragment.showDialog(responseDto, getFragmentManager());
        }
    }
}