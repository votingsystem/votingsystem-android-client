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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.WebSocketSession;

import java.util.HashSet;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageFormFragment extends Fragment {

    public static final String TAG = MessageFormFragment.class.getSimpleName();

    private String broadCastId = MessageFormFragment.class.getSimpleName();
    private UserVSDto userVS;
    private TextView caption_text;
    private EditText messageEditText;
    private Button send_msg_button;
    private SocketMessageDto socketMessage;
    private LinearLayout msg_form;
    private boolean messageDeliveredNotified = false;
    private Set<DeviceVSDto> connectedDevices = new HashSet<>();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            SocketMessageDto socketMessageDto = (SocketMessageDto) intent.getSerializableExtra(
                    ContextVS.WEBSOCKET_MSG_KEY);
            if(intent.hasExtra(ContextVS.PIN_KEY)) {
                switch(responseVS.getTypeVS()) {
                    case WEB_SOCKET_INIT:
                        setProgressDialogVisible(getString(R.string.connecting_caption),
                                getString(R.string.connecting_to_service_msg), true);
                        Utils.toggleWebSocketServiceConnection();
                        break;
                }
            } else setProgressDialogVisible(null, null, false);
            if(socketMessageDto != null) {
                WebSocketSession socketSession = AppVS.getInstance().getWSSession(socketMessageDto.getUUID());
                switch(socketMessageDto.getStatusCode()) {
                    case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.error_lbl), getString(R.string.usevs_connection_not_found_error_msg),
                                getFragmentManager());
                        connectedDevices = new HashSet<>();
                        send_msg_button.setVisibility(View.GONE);
                        break;
                    case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                        if(socketSession.getBroadCastId().equals(broadCastId)) {
                            MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                    R.string.send_message_lbl), getString(R.string.messagevs_send_ok_msg),
                                    getFragmentManager());
                        }
                        break;
                    case ResponseVS.SC_WS_CONNECTION_INIT_OK:
                        updateConnectedView();
                        break;
                    default:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.error_lbl), socketMessageDto.getMessage(), getFragmentManager());
                }
            }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messagevs_form_fragment, null);
        messageEditText = (EditText) view.findViewById(R.id.message);
        msg_form =  (LinearLayout) view.findViewById(R.id.msg_form);
        userVS =  (UserVSDto) getArguments().getSerializable(ContextVS.USER_KEY);
        socketMessage = (SocketMessageDto) getArguments().getSerializable(ContextVS.WEBSOCKET_MSG_KEY);
        String serviceURL = null;
        if(userVS != null) {
            serviceURL = ((AppVS)getActivity().getApplication()).getCurrencyServer()
                    .getDeviceVSConnectedServiceURL(userVS.getNIF());
        }
        if(socketMessage != null) {
            serviceURL = ((AppVS)getActivity().getApplication()).getCurrencyServer()
                    .getDeviceVSConnectedServiceURL(socketMessage.getDeviceFromId(), true);
        }
        caption_text = (TextView) view.findViewById(R.id.caption_text);
        send_msg_button = (Button) view.findViewById(R.id.send_msg_button);
        msg_form.setVisibility(View.GONE);
        send_msg_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(TextUtils.isEmpty(messageEditText.getText().toString())){
                    messageEditText.setError(getString(R.string.empty_field_msg));
                    return;
                }
                String msg = messageEditText.getText().toString();
                Intent startIntent = new Intent(getActivity(), WebSocketService.class);
                startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.MESSAGEVS);
                startIntent.putExtra(ContextVS.MESSAGE_KEY, msg);
                try {
                    startIntent.putExtra(ContextVS.DTO_KEY,
                            JSON.writeValueAsString(connectedDevices));
                } catch(Exception ex) { ex.printStackTrace();}
                startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                getActivity().startService(startIntent);
            }
        });
        if(savedInstanceState != null) {
            messageDeliveredNotified = savedInstanceState.getBoolean("messageDeliveredNotified");
            try {
                connectedDevices = JSON.readValue(savedInstanceState.getString(ContextVS.DTO_KEY),
                        new TypeReference<Set<DeviceVSDto>>() {});
            } catch (Exception ex) {ex.printStackTrace();}
        }
        checkSocketConnection();
        new UserDeviceLoader(serviceURL).execute("");
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.send_message_lbl));
        return view;
    }

    private boolean checkSocketConnection() {
        if(!AppVS.getInstance().isWithSocketConnection()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.send_message_lbl),
                    getString(R.string.connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            PinDialogFragment.showPinScreen(getFragmentManager(),
                                    broadCastId, getString(R.string.init_authenticated_session_pin_msg),
                                    false, TypeVS.WEB_SOCKET_INIT);
                        }
                    }).setNegativeButton(getString(R.string.cancel_lbl), null);
            UIUtils.showMessageDialog(builder);
            return false;
        } else return true;
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void updateConnectedView() {
        if(!connectedDevices.isEmpty() && AppVS.getInstance().isWithSocketConnection()) {
            msg_form.setVisibility(View.VISIBLE);
            caption_text.setText(getString(R.string.user_connected_lbl, userVS.getFullName()));
        } else {
            msg_form.setVisibility(View.GONE);
            caption_text.setText(getString(R.string.uservs_disconnected_lbl, userVS.getFullName()));
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putString(ContextVS.DTO_KEY, JSON.writeValueAsString(connectedDevices));
            outState.putSerializable("messageDeliveredNotified", messageDeliveredNotified);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class UserDeviceLoader extends AsyncTask<String, String, UserVSDto> {

        private String serviceURL;
        public UserDeviceLoader(String serviceURL) { this.serviceURL = serviceURL;}

        @Override protected void onPreExecute() {
            setProgressDialogVisible(getString(R.string.connecting_caption),
                    getString(R.string.check_devices_lbl), true); }

        @Override protected UserVSDto doInBackground(String... params) {
            UserVSDto result = null;
            try {
                result = HttpHelper.getData(UserVSDto.class, serviceURL, MediaTypeVS.JSON);
            } catch (Exception ex) { ex.printStackTrace();}
            return result;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(UserVSDto userDto) {
            connectedDevices =  new HashSet<>();
            try {
                if(userDto != null) {
                    String deviceId = PrefUtils.getApplicationId();
                    for(DeviceVSDto deviceDto : userDto.getConnectedDevices()) {
                        if(!deviceId.equals(deviceDto.getDeviceId())) {
                            connectedDevices.add(deviceDto);
                        }
                    }
                    if(userVS == null) userVS = userDto;
                    updateConnectedView();
                } else MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,getString(R.string.error_lbl),
                        getString(R.string.error_fetching_device_info_lbl), getFragmentManager());
                setProgressDialogVisible(null, null, false);
                LOGD(TAG + ".UserVSDataFetcher", "connectedDevices: " + connectedDevices.size());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}