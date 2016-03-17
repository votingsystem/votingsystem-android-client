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

import org.votingsystem.AppVS;
import org.votingsystem.activity.ActivityBase;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.security.NoSuchAlgorithmException;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRActionsFragment extends Fragment {

	public static final String TAG = QRActionsFragment.class.getSimpleName();

    private enum Action {READ_QR, CREATE_QR}

    private String broadCastId = QRActionsFragment.class.getSimpleName();
    private QRMessageDto qrMessageDto;
    private Action pendingAction;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            SocketMessageDto socketMsg = (SocketMessageDto) intent.getSerializableExtra(ContextVS.WEBSOCKET_MSG_KEY);
            if(socketMsg != null){
                setProgressDialogVisible(false, null, null);
                if(AppVS.getInstance().isWithSocketConnection() && pendingAction != null) {
                    processAction(pendingAction);
                    pendingAction = null;
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
            qrMessageDto = (QRMessageDto) savedInstanceState.getSerializable(ContextVS.DTO_KEY);
        }
        return rootView;
    }

    private void processAction(Action action) {
        if(!AppVS.getInstance().isWithSocketConnection()) {
            pendingAction = action;
            String caption = (action == Action.CREATE_QR)? getString(R.string.qr_create_lbl):
                    getString(R.string.qr_read_lbl);
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(caption,
                    getString(R.string.qr_connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ConnectionUtils.initConnection(((ActivityBase)getActivity()));
                        }
                    });
            UIUtils.showMessageDialog(builder);
        } else {
            switch (action) {
                case READ_QR:
                    Utils.launchQRScanner(this);
                    break;
                case CREATE_QR:
                    Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                    intent.putExtra(ContextVS.FRAGMENT_KEY, TransactionFormFragment.class.getName());
                    intent.putExtra(ContextVS.TYPEVS_KEY, TransactionFormFragment.Type.QR_FORM);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                    break;
            }
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
                try {
                    switch (qrMessageDto.getOperation()) {
                        case INIT_REMOTE_SIGNED_SESSION:
                            SocketMessageDto socketMessage =
                                    SocketMessageDto.getQRInfoRequest(qrMessageDto);
                            sendQRRequestInfo(socketMessage);
                            break;
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(qrMessage.contains("http://") || qrMessage.contains("https://")) {
                new GetDataTask().execute(qrMessage);
            }
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    private void sendQRRequestInfo(SocketMessageDto socketMessage) throws Exception {
        Intent startIntent = new Intent(getActivity(), WebSocketService.class);
        startIntent.putExtra(ContextVS.MESSAGE_KEY, JSON.writeValueAsString(socketMessage));
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.QR_MESSAGE_INFO);
        getActivity().startService(startIntent);
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.DTO_KEY, qrMessageDto);
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
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }


    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        public final String TAG = GetDataTask.class.getSimpleName();

        private QRMessageDto qrMessageDto;

        public GetDataTask() { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg));
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "url: " + urls[0]);
            try {
                qrMessageDto = QRMessageDto.FROM_URL(urls[0]);
                return  HttpHelper.sendData(qrMessageDto.getHashCertVS().getBytes(), null, urls[0]);
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
                return ResponseVS.ERROR(null, ex.getMessage());
            }
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            LOGD(TAG + ".onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            setProgressDialogVisible(false, null, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    TransactionDto dto = (TransactionDto) responseVS.getMessage(TransactionDto.class);
                    dto.setQrMessageDto(qrMessageDto);
                    switch (dto.getOperation()) {
                        case TRANSACTION_INFO:
                        case DELIVERY_WITHOUT_PAYMENT:
                        case DELIVERY_WITH_PAYMENT:
                        case REQUEST_FORM:
                            Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                            intent.putExtra(ContextVS.FRAGMENT_KEY, PaymentFragment.class.getName());
                            intent.putExtra(ContextVS.TRANSACTION_KEY, dto);
                            startActivity(intent);
                            break;
                    }
                } catch (Exception e) { e.printStackTrace();}
            } else MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    responseVS.getMessage(), getFragmentManager());
        }
    }

}