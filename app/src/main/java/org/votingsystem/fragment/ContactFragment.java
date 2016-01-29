package org.votingsystem.fragment;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.ResultListDto;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactFragment extends Fragment {

	public static final String TAG = ContactFragment.class.getSimpleName();

    private AppVS appVS = null;
    private View rootView;
    private String broadCastId = null;
    private Button toggle_contact_button;
    private UserVSDto userVS;
    private TextView connected_text;
    private Menu menu;
    private Set<DeviceVSDto> connectedDevices = new HashSet<>();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            SocketMessageDto socketMessageDto = null;
            try {
                if(intent.hasExtra(ContextVS.WEBSOCKET_MSG_KEY)) socketMessageDto =
                        JSON.readValue(intent.getStringExtra(ContextVS.WEBSOCKET_MSG_KEY),
                        SocketMessageDto.class);
            } catch (Exception ex) { ex.printStackTrace();}
            if(intent.hasExtra(ContextVS.PIN_KEY)) {
                switch(responseVS.getTypeVS()) {
                    case WEB_SOCKET_INIT:
                        setProgressDialogVisible(true, getString(R.string.connecting_caption),
                                getString(R.string.connecting_to_service_msg));
                        Utils.toggleWebSocketServiceConnection();
                        break;
                }
            } else setProgressDialogVisible(false, null, null);
            if(responseVS != null) {
                switch(responseVS.getTypeVS()) {
                    case MESSAGEVS:
                        Intent startIntent = new Intent(getActivity(), WebSocketService.class);
                        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.MESSAGEVS);
                        startIntent.putExtra(ContextVS.MESSAGE_KEY, responseVS.getMessage());
                        try {
                            startIntent.putExtra(ContextVS.DTO_KEY,
                                    JSON.writeValueAsString(connectedDevices));
                        } catch(Exception ex) { ex.printStackTrace();}
                        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                        getActivity().startService(startIntent);
                        break;
                }
            }
            if(socketMessageDto != null) {
                switch(socketMessageDto.getStatusCode()) {
                    case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.error_lbl), getString(R.string.usevs_connection_not_found_error_msg),
                                getFragmentManager());
                        connectedDevices = new HashSet<>();
                        setContactButtonState();
                        break;
                    case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.send_message_lbl), getString(R.string.messagevs_send_ok_msg),
                                getFragmentManager());
                        break;
                    case ResponseVS.SC_WS_CONNECTION_INIT_OK:
                        break;
                    default:
                        MessageDialogFragment.showDialog(socketMessageDto.getStatusCode(), getString(
                                R.string.error_lbl), socketMessageDto.getMessage(), getFragmentManager());
                }
            }
        }
    };

    public static Fragment newInstance(Long contactId) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.CURSOR_POSITION_KEY, contactId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(UserVSDto userVS) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.USER_KEY, userVS);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        rootView = inflater.inflate(R.layout.contact, container, false);
        toggle_contact_button = (Button) rootView.findViewById(R.id.toggle_contact_button);
        connected_text = (TextView) rootView.findViewById(R.id.connected_text);
        Long contactId =  getArguments().getLong(ContextVS.CURSOR_POSITION_KEY, -1);
        if(contactId > 0) {
            userVS = UserContentProvider.loadUser(contactId, getActivity());
        } else {
            userVS =  (UserVSDto) getArguments().getSerializable(ContextVS.USER_KEY);
            UserVSDto contactDB = UserContentProvider.loadUser(userVS, getActivity());
            if(contactDB != null) userVS = contactDB;
        }
        setContactButtonState();
        setHasOptionsMenu(true);
        broadCastId = ContactFragment.class.getSimpleName() + "_" + (contactId != null? contactId:
                UUID.randomUUID().toString());
        if(savedInstanceState != null) {
            try {
                connectedDevices = JSON.readValue(savedInstanceState.getString(ContextVS.DTO_KEY),
                        new TypeReference<Set<DeviceVSDto>>() {});
            } catch (Exception ex) {ex.printStackTrace();}
        }
        if(connectedDevices.isEmpty()) new UserVSDataFetcher().execute("");
        return rootView;
    }

    private void deleteContact() {
        UserContentProvider.deleteById(userVS.getId(), getActivity());
        getActivity().onBackPressed();
    }

    private void addContact() {
        getActivity().getContentResolver().insert(UserContentProvider.CONTENT_URI,
                UserContentProvider.getContentValues(userVS.setType(UserVSDto.Type.CONTACT)));
        setContactButtonState();
    }

    private void setContactButtonState() {
        if(UserVSDto.Type.CONTACT == userVS.getType()) toggle_contact_button.setVisibility(View.GONE);
        else {
            toggle_contact_button.setVisibility(View.VISIBLE);
            toggle_contact_button.setText(getString(R.string.add_contact_lbl));
            toggle_contact_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { addContact(); }
            });
        }
        if(menu != null) {
            menu.removeItem(R.id.send_message); //to avoid duplicated items
            menu.removeItem(R.id.delete_item);
            if(!connectedDevices.isEmpty()) menu.add(R.id.general_items, R.id.send_message, 1,
                        getString(R.string.send_message_lbl));
            if(UserVSDto.Type.CONTACT == userVS.getType()) menu.add(R.id.general_items, R.id.delete_item, 3,
                    getString(R.string.remove_contact_lbl));
        }
        if(!connectedDevices.isEmpty()) connected_text.setText(getString(R.string.user_connected_lbl, userVS.getName()));
        else connected_text.setText(getString(R.string.uservs_disconnected_lbl, userVS.getName()));
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(R.string.operation_ok_msg),
                    message, getFragmentManager());
        } else if(message != null) MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                    getString(R.string.operation_error_msg), message, getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contact, menu);
        this.menu = menu;
        setContactButtonState();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.send_message:
                if(!appVS.isWithSocketConnection()) {
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
                } else MessageVSInputDialogFragment.showDialog(getString(R.string.message_lbl),
                        broadCastId, TypeVS.MESSAGEVS, getFragmentManager());
                return true;
            case R.id.send_money:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, TransactionVSFormFragment.class.getName());
                intent.putExtra(ContextVS.TYPEVS_KEY, TransactionVSFormFragment.Type.TRANSACTIONVS_FORM);
                intent.putExtra(ContextVS.USER_KEY, userVS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.delete_item:
                deleteContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putString(ContextVS.DTO_KEY, JSON.writeValueAsString(connectedDevices));
        } catch (Exception ex) { ex.printStackTrace();}
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class UserVSDataFetcher extends AsyncTask<String, String, List<DeviceVSDto>> {

        public UserVSDataFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected List<DeviceVSDto> doInBackground(String... params) {
            ResultListDto<DeviceVSDto> resultListDto = null;
            try {
                resultListDto = HttpHelper.getData(
                        new TypeReference<ResultListDto<DeviceVSDto>>(){},
                        appVS.getCurrencyServer().getDeviceVSConnectedServiceURL(
                        userVS.getNIF()), MediaTypeVS.JSON);
            } catch (Exception ex) { ex.printStackTrace();}
            return resultListDto != null ? resultListDto.getResultList(): null;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(List<DeviceVSDto> deviceListDto) {
            connectedDevices =  new HashSet<>();
            try {
                if(deviceListDto != null) {
                    String deviceId = PrefUtils.getApplicationId();
                    for(DeviceVSDto deviceDto : deviceListDto) {
                        if(!deviceId.equals(deviceDto.getDeviceId())) {
                            connectedDevices.add(deviceDto);
                        }
                    }
                    userVS.setConnectedDevices(connectedDevices);
                    setContactButtonState();
                } else MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,getString(R.string.error_lbl),
                        getString(R.string.error_fetching_device_info_lbl), getFragmentManager());
                setProgressDialogVisible(false);
                LOGD(TAG + ".UserVSDataFetcher", "connectedDevices: " + connectedDevices.size());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}