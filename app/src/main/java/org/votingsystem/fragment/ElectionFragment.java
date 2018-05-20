package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.votingsystem.App;
import org.votingsystem.activity.BrowserActivity;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.ReceiptContainer;
import org.votingsystem.crypto.VoteContainer;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.identity.IdentityRequestDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.xades.SignatureAlgorithm;
import org.votingsystem.xades.SignatureBuilder;
import org.votingsystem.xades.SignatureValidator;
import org.votingsystem.xades.XAdESUtils;
import org.votingsystem.xades.XmlSignature;
import org.votingsystem.xml.XMLUtils;
import org.votingsystem.xml.XmlWriter;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.util.Constants.MAX_SUBJECT_SIZE;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = ElectionFragment.class.getSimpleName();

    public static final int RC_REQUEST_ANON_CERT_VOTE = 1;
    public static final int RC_SEND_VOTE = 2;

    private ElectionDto election;
    private VoteContainer voteContainer;
    private ElectionOptionDto optionSelected;
    private List<Button> voteOptionsButtonList;
    private Button saveReceiptButton;
    private App app;
    private View rootView;
    private String broadCastId = null;
    private String identityServiceEntityId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
            final ResponseDto response = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            if(ResponseDto.SC_OK == response.getStatusCode()) {
                if(EntitySelectorDialogFragment.TAG.equals(response.getServiceCaller())) {
                    identityServiceEntityId = response.getMessage();
                    processSelectedOption(optionSelected);
                } else {
                    voteContainer = (VoteContainer) intent.getSerializableExtra(Constants.VOTE_KEY);
                    processVoteResult(voteContainer, response);
                }
            }
        }
    };

    private void processVoteResult(VoteContainer voteContainer, ResponseDto response) {
        if (response.getOperationType() == org.votingsystem.util.OperationType.SEND_VOTE) {
            if (ResponseDto.SC_OK == response.getStatusCode()) {
                Set<XmlSignature> signatures = null;
                try {
                    signatures = new SignatureValidator(response.getMessageBytes()).validate();
                } catch (Exception ex) {ex.printStackTrace();}
                voteContainer.setReceipt(response.getMessageBytes());
                LOGD(TAG, "receipt received");
                for(XmlSignature signature : signatures) {
                    LOGD(TAG, "receipt signer: " + signature.getSigningCertificate());
                }

                showReceiptScreen(voteContainer);
                MessageDialogFragment.showDialog(null, getString(R.string.vote_ok_caption),
                        getString(R.string.result_ok_description), getFragmentManager());
            } else if (ResponseDto.SC_ERROR_REQUEST_REPEATED == response.getStatusCode()) {
                final String accessRequestReceiptURL = response.getMessage();
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        response.getCaption(), response.getNotificationMessage(), getActivity());
                builder.setPositiveButton(getString(R.string.open_receipt_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                intent.putExtra(Constants.URL_KEY, accessRequestReceiptURL);
                                intent.putExtra(Constants.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                startActivity(intent);
                            }
                        });
                UIUtils.showMessageDialog(builder);
            } else {
                setOptionButtonsEnabled(true);
                MessageDialogFragment.showDialog(response, getFragmentManager());
            }
        }
        setProgressDialogVisible(false, null, null);
    }

    private void launchVoteService(org.votingsystem.util.OperationType operation,
                                   byte[] signedAnonCertRequest) {
        LOGD(TAG + ".launchVoteService", "operation: " + operation.toString());
        try {
            voteContainer = VoteContainer.generate(election, optionSelected, identityServiceEntityId);
            setProgressDialogVisible(true,  getString(R.string.sending_vote_lbl), null);
            setOptionButtonsEnabled(false);
            new SendVoteTask(voteContainer, signedAnonCertRequest, operation).execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static ElectionFragment newInstance(byte[] electionSerialized) {
        ElectionFragment fragment = new ElectionFragment();
        Bundle args = new Bundle();
        args.putByteArray(Constants.ELECTION_KEY, electionSerialized);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView - savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        ResponseDto responseDto = null;
        try {
            identityServiceEntityId = getArguments().getString(Constants.ID_SERVICE_ENTITY_ID);
            if (getArguments().get(Constants.ELECTION_KEY) != null) {
                election = (ElectionDto) ObjectUtils.deSerializeObject(
                        getArguments().getByteArray(Constants.ELECTION_KEY));
            } else if (getArguments().getSerializable(Constants.VOTE_KEY) != null) {
                voteContainer = (VoteContainer) getArguments().getSerializable(Constants.VOTE_KEY);
                election = voteContainer.getElection();
                responseDto = getArguments().getParcelable(Constants.RESPONSE_KEY);
            }
            election.setVotingServiceProvider(app.getSystemEntity(election.getEntityId(), false).getEntity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.election_fragment, container, false);
        if(election == null) {
            LOGD(TAG, "onCreateView - null election");
            return rootView;
        }
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        saveReceiptButton.setOnClickListener(this);
        setHasOptionsMenu(true);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        broadCastId = ElectionFragment.class.getSimpleName() + "_" + election.getUUID();
        String subtTitle = null;
        switch (election.getState()) {
            case ACTIVE:
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_open_lbl,
                                DateUtils.getElapsedTimeStr(election.getDateFinish())));
                break;
            case PENDING:
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_pending_lbl,
                                DateUtils.getElapsedTimeStr(election.getDateBegin())));
                subtTitle = getString(R.string.init_lbl) + ": " +
                        DateUtils.getDayWeekDateStr(election.getDateBegin(), "HH:mm");
                break;
            default:
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.voting_closed_lbl));
                subtTitle = DateUtils.getUTCDateStr(election.getDateBegin());
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(subtTitle);
        if (voteContainer != null)
            processVoteResult(voteContainer, responseDto);
        if(savedInstanceState != null) {
            identityServiceEntityId = savedInstanceState.getString(Constants.ID_SERVICE_ENTITY_ID);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (bundle != null) {
            voteContainer = (VoteContainer) bundle.getSerializable(Constants.VOTE_KEY);
            optionSelected = (ElectionOptionDto) bundle.getSerializable(Constants.DTO_KEY);
        }
        if (voteContainer != null && voteContainer.getReceipt() != null)
            showReceiptScreen(voteContainer);
        else setEventScreen(election);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_receipt_button:
                voteContainer.setOperationType(org.votingsystem.util.OperationType.SEND_VOTE);
                Uri uri = getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI,
                        ReceiptContentProvider.getContentValues(voteContainer, ReceiptContainer.State.ACTIVE));
                LOGD(TAG + ".saveVote", "uri: " + uri.toString());
                saveReceiptButton.setEnabled(false);
                break;
            case R.id.event_subject:
                if (election != null && election.getSubject() != null &&
                        election.getSubject().length() > MAX_SUBJECT_SIZE) {
                    MessageDialogFragment.showDialog(null, getString(R.string.subject_lbl),
                            election.getSubject(), getFragmentManager());
                }
                break;
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if (isVisible) {
            if(message == null)
                message = getString(R.string.wait_msg);
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.election, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(App.getInstance(), BrowserActivity.class);
                intent.putExtra(Constants.URL_KEY, OperationType.getVoteStatsURL(
                        election.getEntityId(), election.getUUID()));
                intent.putExtra(Constants.DOUBLE_BACK_KEY, false);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        try {
            switch (requestCode) {
                case RC_REQUEST_ANON_CERT_VOTE:
                    if (Activity.RESULT_OK == resultCode) {
                        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                        launchVoteService(OperationType.SEND_VOTE, responseDto.getMessageBytes());
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showReceiptScreen(final VoteContainer voteWrapper) {
        LOGD(TAG, "showReceiptScreen");
        rootView.findViewById(R.id.receipt_buttons).setVisibility(View.VISIBLE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = voteWrapper.getElection().getSubject();
        if (subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        try {
            String eventContent = new String(Base64.decode(
                    voteWrapper.getElection().getContent().getBytes(), Base64.NO_WRAP), "UTF-8");
            contentTextView.setText(Html.fromHtml(eventContent));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<ElectionOptionDto> electionOptions = voteWrapper.getElection().getElectionOptions();
        LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.option_button_container);
        if (voteOptionsButtonList == null) {
            voteOptionsButtonList = new ArrayList<>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 25, 15);
            for (final ElectionOptionDto option : electionOptions) {
                Button optionButton = new Button(getActivity());
                optionButton.setText(option.getContent());
                voteOptionsButtonList.add(optionButton);
                optionButton.setEnabled(false);
                linearLayout.addView(optionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }

    private void setEventScreen(final ElectionDto event) {
        LOGD(TAG + ".setEventScreen", "setEventScreen");
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        saveReceiptButton.setEnabled(true);
        String subject = event.getSubject();
        if (subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(event.getContent()));
        //contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<ElectionOptionDto> electionOptions = event.getElectionOptions();
        LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.option_button_container);
        if (voteOptionsButtonList != null) linearLayout.removeAllViews();
        voteOptionsButtonList = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final ElectionOptionDto option : electionOptions) {
            Button optionButton = new Button(getActivity());
            optionButton.setText(option.getContent());
            optionButton.setTextAppearance(getActivity(), R.style.electionOptionText);
            optionButton.setOnClickListener(new Button.OnClickListener() {
                ElectionOptionDto optionSelected = option;
                public void onClick(View v) {
                    LOGD(TAG, ".optionSelected");
                    processSelectedOption(optionSelected);
                }
            });
            voteOptionsButtonList.add(optionButton);
            if (!event.isActive())
                optionButton.setEnabled(false);
            linearLayout.addView(optionButton, paramsButton);
        }
    }

    private void processSelectedOption(ElectionOptionDto optionSelected) {
        LOGD(TAG + ".processSelectedOption", "processSelectedOption");
        try {
            this.optionSelected = optionSelected;
            if(identityServiceEntityId == null) {
                List<TrustedEntitiesDto.EntityDto> trustedEntityList =   App.getInstance().getTrustedEntityList(
                        App.getInstance().getVotingServiceProvider().getId(), SystemEntityType.VOTING_ID_PROVIDER);
                EntitySelectorDialogFragment.showDialog(broadCastId, trustedEntityList,
                        getString(R.string.identity_provider_lbl), getFragmentManager());
                return;
            }
            String optionSelectedMsg = StringUtils.truncate(optionSelected.getContent(),
                    Constants.SELECTED_OPTION_MAX_LENGTH);
            Intent intent = new Intent(getActivity(), ID_CardNFCReaderActivity.class);
            MetadataDto indentityServiceMetadata = app.getSystemEntity(identityServiceEntityId, false);
            intent.putExtra(Constants.TIMESTAMP_SERVER_KEY,
                    OperationType.TIMESTAMP_REQUEST_DISCRETE.getUrl(indentityServiceMetadata.getFirstTimeStampEntityId()));
            intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.vote_selected_msg,
                    optionSelectedMsg));
            voteContainer = VoteContainer.generate(election, optionSelected, identityServiceEntityId);
            IdentityRequestDto requestDto = voteContainer.getIdentityRequest();
            byte[] identityRequest = XmlWriter.write(requestDto);
            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, identityRequest);
            startActivityForResult(intent, RC_REQUEST_ANON_CERT_VOTE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if (voteOptionsButtonList == null) return;
        for (Button button : voteOptionsButtonList) {
            button.setEnabled(areEnabled);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.VOTE_KEY, voteContainer);
        outState.putSerializable(Constants.DTO_KEY, optionSelected);
        outState.putSerializable(Constants.ID_SERVICE_ENTITY_ID, identityServiceEntityId);
    }

    public class SendVoteTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = SendVoteTask.class.getSimpleName();

        private byte[] anonCertRequestBytes;
        private OperationType operation;
        private VoteContainer voteContainer;

        public SendVoteTask(VoteContainer voteContainer, byte[] anonCertRequestBytes, OperationType operation) {
            this.anonCertRequestBytes = anonCertRequestBytes;
            this.operation = operation;
            this.voteContainer = voteContainer;
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.sending_vote_lbl));
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            ResponseDto responseDto = null;
            try {
                switch (operation) {
                    case SEND_VOTE:
                        VoteDto vote = voteContainer.getVote();
                        String indentityServiceEntity = vote.getIndentityServiceEntity();
                        MetadataDto indentityServiceMetadata = app.getSystemEntity(indentityServiceEntity, true);
                        Map<String, String> mapToSend = new HashMap<>();
                        mapToSend.put(Constants.CSR_FILE_NAME,
                                new String(voteContainer.getCertificationRequest().getCsrPEM()));
                        mapToSend.put(Constants.ANON_CERTIFICATE_REQUEST_FILE_NAME, new String(anonCertRequestBytes));
                        String serviceURL = OperationType.ANON_VOTE_CERT_REQUEST.getUrl(indentityServiceEntity);
                        responseDto = HttpConn.getInstance().doPostMultipartRequest(mapToSend, serviceURL);
                        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                            CertificationRequest certificationRequest = voteContainer.getCertificationRequest();
                            certificationRequest.setSignedCsr(responseDto.getMessageBytes());
                            byte[] voteBytes = XmlWriter.write(voteContainer.getVote());
                            String voteStr = XMLUtils.prepareRequestToSign(voteBytes);
                            LOGD(TAG, "vote: " + new String(voteBytes));
                            Certificate[] certificateChain = new Certificate[]{certificationRequest.getCertificate()};
                            //we use discrete timestamps to avoid associate by time proximity signed request with votes in the audits
                            String timeStampServiceURL = OperationType.TIMESTAMP_REQUEST_DISCRETE.getUrl(
                                    indentityServiceMetadata.getFirstTimeStampEntityId());
                            MockDNIe mockDNIe = new MockDNIe(certificationRequest.getPrivateKey(),
                                    certificationRequest.getCertificate()).setCertificateChain(certificateChain);
                            byte[] signedVote = new SignatureBuilder(voteStr.getBytes(), XAdESUtils.XML_MIME_TYPE,
                                    SignatureAlgorithm.RSA_SHA_256.getName(), mockDNIe,
                                    timeStampServiceURL).build();
                            LOGD(TAG, "signedVote: " + new String(signedVote));
                            responseDto = HttpConn.getInstance().doPostRequest(signedVote, ContentType.XML,
                                    OperationType.SEND_VOTE.getUrl(vote.getVotingServiceEntity()));
                            LOGD(TAG, "status: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
                        }
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseDto.ERROR(null, ex.getMessage());
            }
            return responseDto;
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG + ".onPostExecute() ", " - statusCode: " + response.getStatusCode());
            setProgressDialogVisible(false, null, null);
            if(ResponseDto.SC_OK != response.getStatusCode()) {
                MessageDialogFragment.showDialog(null, getString(R.string.error_lbl),
                        response.getMessage(), getFragmentManager());
            } else processVoteResult(voteContainer, response.setOperationType(OperationType.SEND_VOTE));
        }
    }

}