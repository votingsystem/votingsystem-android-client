package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.votingsystem.android.R;
import org.votingsystem.dto.AESParamsDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.WebSocketSession;

import java.security.NoSuchAlgorithmException;

import static org.votingsystem.util.Constants.FRAGMENT_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRActionsFragment extends Fragment {

	public static final String TAG = QRActionsFragment.class.getSimpleName();
    public static final String PENDING_ACTION_KEY = "PENDING_ACTION_KEY";

    private enum Action {READ_QR, CREATE_QR, OPERATION, AUTHENTICATED_OPERATION}

    private String broadCastId = QRActionsFragment.class.getSimpleName();
    private QRMessageDto qrMessageDto;
    private Action pendingAction;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            LOGD(TAG, "broadcastReceiver - pendingAction: " + pendingAction +
                    " - isWithSocketConnection: " + App.getInstance().isWithSocketConnection() +
                    " - ConnectedDevice: " + App.getInstance().getConnectedDevice());
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(Constants.WEBSOCKET_MSG_KEY);
            if(socketMsg != null){
                setProgressDialogVisible(false, null, null);
                if(App.getInstance().getConnectedDevice() != null && pendingAction != null) {
                    if(pendingAction == Action.OPERATION) {
                        processAction(pendingAction);
                        pendingAction = null;
                    } else if(pendingAction == Action.AUTHENTICATED_OPERATION &&
                            App.getInstance().isWithSocketConnection()) {
                        processAction(pendingAction);
                        pendingAction = null;
                    }
                }
            }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_actions_fragment, container, false);
        Button read_qr_btn = (Button) rootView.findViewById(R.id.read_qr_btn);
        read_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processAction(Action.READ_QR);
            }
        });
        Button gen_qr_btn = (Button) rootView.findViewById(R.id.gen_qr_btn);
        gen_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processAction(Action.CREATE_QR);
            }
        });
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_codes_lbl));
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
            qrMessageDto = (QRMessageDto) savedInstanceState.getSerializable(Constants.DTO_KEY);
            pendingAction = (Action) savedInstanceState.getSerializable(PENDING_ACTION_KEY);
        }
        return rootView;
    }

    private void processAction(Action action) {
        switch (action) {
            case READ_QR:
                Utils.launchQRScanner(this);
                break;
            case CREATE_QR:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(Constants.FRAGMENT_KEY, TransactionFormFragment.class.getName());
                intent.putExtra(Constants.TYPEVS_KEY, TransactionFormFragment.Type.QR_FORM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case AUTHENTICATED_OPERATION:
            case OPERATION:
                processQRCode(qrMessageDto);
                break;
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null && result.getContents() != null) {
            String qrMessage = result.getContents();
            LOGD(TAG, "QR reader - onActivityResult - qrMessage: " + qrMessage);
            qrMessageDto = QRMessageDto.FROM_QR_CODE(qrMessage);
            if(qrMessageDto.getOperation() != null) {
                processQRCode(qrMessageDto);
            } else if(qrMessage.contains("http://") || qrMessage.contains("https://")) {
                new SendDataTask().execute(qrMessage);
            } else LOGD(TAG, "onActivityResult - qrMessage unprocessed");
        }
    }

    private void processQRCode(QRMessageDto qrMessageDto) {
        try {
            SocketMessageDto socketMessage = null;
            switch (qrMessageDto.getOperation()) {
                case INIT_REMOTE_SIGNED_SESSION:
                    if(!App.getInstance().isWithSocketConnection()) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(null,
                                getString(R.string.connection_required_msg),
                                getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        pendingAction = Action.AUTHENTICATED_OPERATION;
                                        if(getActivity() != null) ConnectionUtils.initConnection(
                                                ((ActivityBase)getActivity()));
                                        dialog.cancel();
                                    }
                                });
                        UIUtils.showMessageDialog(builder);
                    } else {
                        qrMessageDto.setAesParams(AESParamsDto.CREATE());
                        socketMessage = SocketMessageDto.getQRInfoRequest(qrMessageDto, true);
                        sendSocketMessage(socketMessage, qrMessageDto.getSessionType());
                    }
                    break;
                case SEND_VOTE:
                    new GetDataTask(OperationType.SEND_VOTE).execute(App.getInstance().
                            getAccessControl().getEventVSURL(qrMessageDto.getItemId()));
                    break;
                case OPERATION_PROCESS:
                    if(App.getInstance().getConnectedDevice() != null) {
                        socketMessage = SocketMessageDto.getQRInfoRequest(qrMessageDto, false);
                        sendSocketMessage(socketMessage, qrMessageDto.getSessionType());
                    } else {
                        pendingAction = Action.OPERATION;
                        this.qrMessageDto = qrMessageDto;
                        ConnectionUtils.initUnathenticatedConnection((ActivityBase)getActivity(),
                                qrMessageDto.getSessionType());
                    }
                    break;
                case ANONYMOUS_REPRESENTATIVE_SELECTION:
                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                    intent.putExtra(Constants.FRAGMENT_KEY, RepresentativeFragment.class.getName());
                    intent.putExtra(Constants.ITEM_ID_KEY, qrMessageDto.getItemId());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                    break;
                case GET_AES_PARAMS:
                    WebSocketSession socketSession = App.getInstance()
                            .getWSSessionByIV(qrMessageDto.getMsg());
                    if(socketSession == null) {
                        MessageDialogFragment.showDialog(null, getString(R.string.error_lbl),
                                getString(R.string.browser_session_expired_msg), getFragmentManager());
                    } else {
                        qrMessageDto.setOperation(OperationType.SEND_AES_PARAMS)
                                .setAesParams(socketSession.getAesParams());
                        socketMessage = SocketMessageDto.getQRInfoRequest(qrMessageDto, true);
                        sendSocketMessage(socketMessage, qrMessageDto.getSessionType());
                    }
                    break;
                default: LOGD(TAG, "processQRCode: " + qrMessageDto.getOperation());
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    private void sendSocketMessage(SocketMessageDto socketMessage, OperationType sessionType) throws Exception {
        Intent startIntent = new Intent(getActivity(), WebSocketService.class);
        startIntent.putExtra(Constants.SESSION_KEY, sessionType);
        startIntent.putExtra(Constants.MESSAGE_KEY, JSON.writeValueAsString(socketMessage));
        getActivity().startService(startIntent);
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.DTO_KEY, qrMessageDto);
        outState.putSerializable(PENDING_ACTION_KEY, pendingAction);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        ActivityResult activityResult = ((ActivityBase)getActivity()).getActivityResult();
        if(activityResult != null) {
            onActivityResult(activityResult.getRequestCode(),
                    activityResult.getResultCode(), activityResult.getData());
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }


    public class SendDataTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = SendDataTask.class.getSimpleName();

        private QRMessageDto qrMessageDto;

        public SendDataTask() { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg));
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "url: " + urls[0]);
            try {
                qrMessageDto = QRMessageDto.FROM_URL(urls[0]);
                return  HttpConnection.getInstance().sendData(qrMessageDto.getRevocationHash().getBytes(), null, urls[0]);
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
                return ResponseDto.ERROR(null, ex.getMessage());
            }
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override  protected void onPostExecute(ResponseDto responseDto) {
            LOGD(TAG + ".onPostExecute() ", " - statusCode: " + responseDto.getStatusCode());
            setProgressDialogVisible(false, null, null);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    TransactionDto dto = (TransactionDto) responseDto.getMessage(TransactionDto.class);
                    dto.setQrMessageDto(qrMessageDto);
                    switch (dto.getOperation()) {
                        case TRANSACTION_INFO:
                        case DELIVERY_WITHOUT_PAYMENT:
                        case DELIVERY_WITH_PAYMENT:
                        case REQUEST_FORM:
                            Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                            intent.putExtra(Constants.FRAGMENT_KEY, PaymentFragment.class.getName());
                            intent.putExtra(Constants.TRANSACTION_KEY, dto);
                            startActivity(intent);
                            break;
                    }
                } catch (Exception e) { e.printStackTrace();}
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    responseDto.getMessage(), getFragmentManager());
        }
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = GetDataTask.class.getSimpleName();

        private OperationType operation;

        public GetDataTask(OperationType operation) {
            this.operation = operation;
        }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg));
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "url: " + urls[0]);
            try {
                return HttpConnection.getInstance().getData(urls[0], ContentType.JSON);
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseDto.ERROR(null, ex.getMessage());
            }
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override  protected void onPostExecute(ResponseDto responseDto) {
            LOGD(TAG + ".onPostExecute() ", " - statusCode: " + responseDto.getStatusCode());
            setProgressDialogVisible(false, null, null);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    switch (operation) {
                        case SEND_VOTE:
                            Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                            intent.putExtra(FRAGMENT_KEY, EventVSFragment.class.getName());
                            intent.putExtra(Constants.EVENTVS_KEY, responseDto.getMessage());
                            startActivity(intent);
                            break;
                    }
                } catch (Exception e) { e.printStackTrace();}
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    responseDto.getMessage(), getFragmentManager());
        }
    }

}