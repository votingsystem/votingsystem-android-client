package org.votingsystem.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.votingsystem.App;
import org.votingsystem.activity.ActivityBase;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.ui.DialogButton;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.xml.XmlReader;

import static org.votingsystem.util.Constants.FRAGMENT_KEY;
import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRActionsFragment extends Fragment {

    public static final String TAG = QRActionsFragment.class.getSimpleName();

    private static int RC_SIGN_PUBLISH_DOCUMENT_RESULT = 0;

    private String broadCastId = QRActionsFragment.class.getSimpleName();
    private QRMessageDto qrMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_actions_fragment, container, false);
        Button read_qr_btn = (Button) rootView.findViewById(R.id.read_qr_btn);
        read_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Utils.launchQRScanner(QRActionsFragment.this);
            }
        });
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_codes_lbl));
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
            qrMessage = (QRMessageDto) savedInstanceState.getSerializable(
                    Constants.QR_CODE_KEY);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.QR_CODE_KEY, qrMessage);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if (requestCode == RC_SIGN_PUBLISH_DOCUMENT_RESULT) {
            if (Activity.RESULT_OK == resultCode) {
                ResponseDto signatureResponse = data.getParcelableExtra(Constants.RESPONSE_KEY);
                new PostDataTask(signatureResponse.getMessageBytes(),
                        OperationType.PUBLISH_ELECTION.getUrl(qrMessage.getSystemEntityId()),
                        ContentType.XML).execute();
            }
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null && result.getContents() != null) {
                String qrMessageStr = result.getContents();
                LOGD(TAG, "onActivityResult - qrMessage: " + qrMessageStr);
                qrMessage = QRMessageDto.FROM_QR_CODE(qrMessageStr);
                if (qrMessage.getOperation() != null) {
                    new GetDataTask(qrMessage.getSystemEntityId(), qrMessage.getOperation()).execute();
                } else {
                    new GetDataTask(qrMessage.getSystemEntityId(), qrMessage.getUUID()).execute();
                }
            }
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityResult activityResult = ((ActivityBase) getActivity()).getActivityResult();
        if (activityResult != null) {
            onActivityResult(activityResult.getRequestCode(),
                    activityResult.getResultCode(), activityResult.getData());
        }
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = GetDataTask.class.getSimpleName();

        private String systemEntityId;
        private String qrUUID;
        private OperationType operationType;
        private byte[] qrResponseData;
        private ElectionDto electionDto;
        private MetadataDto entityMetadata;

        public GetDataTask(String systemEntityId, String qrUUID) {
            this.systemEntityId = systemEntityId;
            this.qrUUID = qrUUID;
        }

        public GetDataTask(String systemEntityId, OperationType operationType) {
            this.systemEntityId = systemEntityId;
            this.operationType = operationType;
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg));
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            try {
                entityMetadata = App.getInstance().getSystemEntity(systemEntityId, true);
                if(operationType != null) {
                    switch (operationType) {
                        case FETCH_ELECTIONS:
                            if(entityMetadata != null) {
                                PrefUtils.addVotingServiceProvider(entityMetadata.getEntity().getId());
                                App.getInstance().setVotingServiceProvider(entityMetadata.getEntity());
                            }
                            return ResponseDto.OK();
                    }
                }
                ResponseDto response = HttpConn.getInstance().doPostRequest(qrUUID.getBytes(),
                        null, OperationType.GET_QR_INFO.getUrl(systemEntityId));
                if (ResponseDto.SC_OK == response.getStatusCode()) {
                    try {
                        QRResponseDto qrResponse = XmlReader.readQRResponse(response.getMessageBytes());
                        operationType = qrResponse.getOperationType();
                        qrResponseData = qrResponse.getData();
                        switch (operationType) {
                            case PUBLISH_ELECTION:
                                break;
                            case ANON_VOTE_CERT_REQUEST:
                                electionDto = XmlReader.readElection(qrResponseData);
                                App.getInstance().getSystemEntity(electionDto.getEntityId(), true);
                                break;
                            default:
                                LOGE(TAG, "unknown operation: " + qrResponse.getOperationType());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return response;
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseDto.ERROR(null, ex.getMessage());
            }
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG, "onPostExecute - statusCode: " + response.getStatusCode() +
                    " - operationType: " + operationType);
            setProgressDialogVisible(false, null, null);
            try {
                if (ResponseDto.SC_OK == response.getStatusCode()) {
                    switch (operationType) {
                        case FETCH_ELECTIONS: {
                            Intent intent = new Intent(ActivityBase.BROADCAST_ID);
                            ResponseDto responseDto = ResponseDto.OK()
                                    .setServiceCaller(ActivityBase.CHILD_FRAGMENT)
                                    .setMessage(Integer.valueOf(R.id.elections).toString());
                            intent.putExtra(Constants.RESPONSE_KEY, responseDto);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                            break;
                        }
                        case PUBLISH_ELECTION:
                            signXMLDocument(new String(qrResponseData),
                                    getString(R.string.election_publish_msg),
                                    RC_SIGN_PUBLISH_DOCUMENT_RESULT);
                            break;
                        case ANON_VOTE_CERT_REQUEST: {
                            Intent intent = new Intent(QRActionsFragment.this.getActivity(),
                                    FragmentContainerActivity.class);
                            intent.putExtra(FRAGMENT_KEY, ElectionFragment.class.getName());
                            intent.putExtra(Constants.ID_SERVICE_ENTITY_ID, systemEntityId);
                            intent.putExtra(Constants.ELECTION_KEY, ObjectUtils.serializeObject(electionDto ));
                            startActivity(intent);
                            break;
                        }
                        default:
                            LOGE(TAG, "unknown operation: " + operationType);
                    }
                } else {
                    MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                            response.getMessage(), getFragmentManager());
                }
            } catch (Exception ex) {
                MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                        ex.getMessage(), getFragmentManager());
            }
        }
    }

    public class PostDataTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = PostDataTask.class.getSimpleName();

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
                        response.getMessage(), positiveButton, null, getActivity());
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    response.getMessage(), getFragmentManager());
        }
    }

    private void signXMLDocument(String documentToSign, String messagePrefix, int activityResultCode)
            throws Exception {
        MetadataDto metadataDto = App.getInstance().getSystemEntity(qrMessage.getSystemEntityId(), false);
        String timeStampServiceURL = OperationType.TIMESTAMP_REQUEST.getUrl(
                metadataDto.getFirstTimeStampEntityId());
        Intent intent = new Intent(getActivity(), ID_CardNFCReaderActivity.class);
        intent.putExtra(Constants.MESSAGE_KEY, messagePrefix + " - " +
                getString(R.string.enter_password_msg));
        intent.putExtra(Constants.MESSAGE_CONTENT_KEY, documentToSign.getBytes());
        intent.putExtra(Constants.TIMESTAMP_SERVER_KEY, timeStampServiceURL);
        if (qrMessage != null && qrMessage.getUUID() != null) {
            intent.putExtra(Constants.QR_CODE_KEY, qrMessage.getUUID().substring(0, 4));
        }
        startActivityForResult(intent, activityResultCode);
    }

}