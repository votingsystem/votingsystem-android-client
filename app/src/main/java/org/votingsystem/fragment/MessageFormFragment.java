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

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.service.WebSocketService;
import org.votingsystem.util.ConnectionUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;
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
    private UserDto user;
    private TextView caption_text;
    private EditText messageEditText;
    private Button send_msg_button;
    private SocketMessageDto socketMessage;
    private LinearLayout msg_form;
    private boolean messageDeliveredNotified = false;
    private Set<DeviceDto> connectedDevices = new HashSet<>();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
            SocketMessageDto socketMessageDto = (SocketMessageDto) intent.getSerializableExtra(
                    Constants.WEBSOCKET_MSG_KEY);
            if(socketMessageDto != null) {
                setProgressDialogVisible(null, null, false);
                WebSocketSession socketSession = App.getInstance().getWSSession(socketMessageDto.getUUID());
                switch(socketMessageDto.getStatusCode()) {
                    case ResponseDto.SC_WS_CONNECTION_NOT_FOUND:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.error_lbl), getString(R.string.usevs_connection_not_found_error_msg),
                                getFragmentManager());
                        connectedDevices = new HashSet<>();
                        send_msg_button.setVisibility(View.GONE);
                        break;
                    case ResponseDto.SC_WS_MESSAGE_SEND_OK:
                        if(socketSession.getBroadCastId().equals(broadCastId)) {
                            MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                    R.string.send_msg_lbl), getString(R.string.messagevs_send_ok_msg),
                                    getFragmentManager());
                        }
                        break;
                    case ResponseDto.SC_WS_CONNECTION_INIT_OK:
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
        user =  (UserDto) getArguments().getSerializable(Constants.USER_KEY);
        socketMessage = (SocketMessageDto) getArguments().getSerializable(Constants.WEBSOCKET_MSG_KEY);
        String serviceURL = null;
        if(user != null) {
            serviceURL = ((App)getActivity().getApplication()).getCurrencyServer()
                    .getDeviceConnectedServiceURL(user.getNIF());
        }
        if(socketMessage != null) {
            serviceURL = ((App)getActivity().getApplication()).getCurrencyServer()
                    .getDeviceConnectedServiceURL(socketMessage.getDeviceFromId(), true);
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
                startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.MESSAGEVS);
                startIntent.putExtra(Constants.MESSAGE_KEY, msg);
                try {
                    startIntent.putExtra(Constants.DTO_KEY,
                            JSON.writeValueAsString(connectedDevices));
                } catch(Exception ex) { ex.printStackTrace();}
                startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
                getActivity().startService(startIntent);
            }
        });
        if(savedInstanceState != null) {
            messageDeliveredNotified = savedInstanceState.getBoolean("messageDeliveredNotified");
            try {
                connectedDevices = JSON.readValue(savedInstanceState.getString(Constants.DTO_KEY),
                        new TypeReference<Set<DeviceDto>>() {});
            } catch (Exception ex) {ex.printStackTrace();}
        }
        checkSocketConnection();
        new UserDeviceLoader(serviceURL).execute("");
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.send_msg_lbl));
        return view;
    }

    private boolean checkSocketConnection() {
        if(!App.getInstance().isWithSocketConnection()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.send_msg_lbl),
                    getString(R.string.connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ConnectionUtils.initConnection((AppCompatActivity) getActivity());
                        }
                    });
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
        if(!connectedDevices.isEmpty() && App.getInstance().isWithSocketConnection()) {
            msg_form.setVisibility(View.VISIBLE);
            caption_text.setText(getString(R.string.user_connected_lbl, user.getFullName()));
        } else {
            msg_form.setVisibility(View.GONE);
            caption_text.setText(getString(R.string.user_disconnected_lbl, user.getFullName()));
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putString(Constants.DTO_KEY, JSON.writeValueAsString(connectedDevices));
            outState.putSerializable("messageDeliveredNotified", messageDeliveredNotified);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class UserDeviceLoader extends AsyncTask<String, String, UserDto> {

        private String serviceURL;
        public UserDeviceLoader(String serviceURL) { this.serviceURL = serviceURL;}

        @Override protected void onPreExecute() {
            setProgressDialogVisible(getString(R.string.connecting_caption),
                    getString(R.string.check_devices_lbl), true); }

        @Override protected UserDto doInBackground(String... params) {
            UserDto result = null;
            try {
                result = HttpConnection.getInstance().getData(UserDto.class, serviceURL, MediaType.JSON);
            } catch (Exception ex) { ex.printStackTrace();}
            return result;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(UserDto userDto) {
            connectedDevices =  new HashSet<>();
            try {
                if(userDto != null) {
                    String deviceId = PrefUtils.getDeviceId();
                    for(DeviceDto deviceDto : userDto.getConnectedDevices()) {
                        if(!deviceId.equals(deviceDto.getDeviceId())) {
                            connectedDevices.add(deviceDto);
                        }
                    }
                    if(user == null) user = userDto;
                    updateConnectedView();
                } else MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,getString(R.string.error_lbl),
                        getString(R.string.error_fetching_device_info_lbl), getFragmentManager());
                setProgressDialogVisible(null, null, false);
                LOGD(TAG + ".UserDataFetcher", "connectedDevices: " + connectedDevices.size());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}