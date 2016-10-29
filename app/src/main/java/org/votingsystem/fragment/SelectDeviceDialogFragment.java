package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SelectDeviceDialogFragment extends DialogFragment {

    public static final String TAG = SelectDeviceDialogFragment.class.getSimpleName();

    private App app;
    private SimpleAdapter simpleAdapter;
    private String dialogCaller;
    private Set<DeviceDto> deviceSetDto;
    private TextView msg_text;
    private List<String> connectedDeviceList = new ArrayList<>();
    private DeviceLoader deviceLoader;

    public static void showDialog(String dialogCaller, FragmentManager manager, String tag) {
        SelectDeviceDialogFragment dialogFragment = new SelectDeviceDialogFragment();
        Bundle args = new Bundle();
        args.putString(Constants.CALLER_KEY, dialogCaller);
        dialogFragment.setArguments(args);
        dialogFragment.show(manager, tag);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        app = (App) getActivity().getApplicationContext();
        dialogCaller = getArguments().getString(Constants.CALLER_KEY);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.select_device_dialog, null);
        ListView tag_list_view = (ListView) view.findViewById(R.id.tag_list_view);
        msg_text = (TextView) view.findViewById(R.id.msg_text);
        simpleAdapter = new SimpleAdapter(new ArrayList<String>(), getActivity());
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);
        tag_list_view.setAdapter(simpleAdapter);
        tag_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                Intent intent = new Intent(dialogCaller);
                ResponseDto responseDto = new ResponseDto(ResponseDto.SC_OK, OperationType.DEVICE_SELECT);
                DeviceDto selectedDeviceDto = null;
                for(DeviceDto deviceDto : deviceSetDto) {
                    if(connectedDeviceList.get(position).equals(deviceDto.getDeviceName()))
                        selectedDeviceDto = deviceDto;
                }
                try {
                    responseDto.setMessage(JSON.writeValueAsString(selectedDeviceDto));
                } catch (IOException e) { e.printStackTrace(); }
                intent.putExtra(Constants.RESPONSEVS_KEY, responseDto);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        return dialog;
    }

    @Override public void onStop() {
        super.onStop();
        if(deviceLoader != null) deviceLoader.cancel(true);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Constants.CALLER_KEY, dialogCaller);
        try {
            outState.putSerializable(Constants.DTO_KEY, JSON.writeValueAsString(deviceSetDto));
        } catch (IOException e) { e.printStackTrace(); }
        outState.putSerializable(Constants.FORM_DATA_KEY, (Serializable) connectedDeviceList);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(Constants.FORM_DATA_KEY)) {
            dialogCaller = savedInstanceState.getString(Constants.CALLER_KEY);
            deviceSetDto = (Set<DeviceDto>) savedInstanceState.getSerializable(Constants.DTO_KEY);
            connectedDeviceList = (List<String>) savedInstanceState.getSerializable(Constants.FORM_DATA_KEY);
            if(connectedDeviceList.size() > 0) {
                msg_text.setText(getString(R.string.select_connected_device_msg));
            }
            simpleAdapter.setItemList(connectedDeviceList);
            simpleAdapter.notifyDataSetChanged();
        } else {
            deviceLoader = new DeviceLoader();
            String targetURL = ((App)getActivity().getApplicationContext()).getCurrencyServer().
                    getDeviceConnectedServiceURL(app.getUser().getNIF());
            deviceLoader.execute(targetURL);
        }
    }

    private class DeviceLoader extends AsyncTask<String, Void, List<String>> {

        private final ProgressDialog dialog = new ProgressDialog(getActivity());

        @Override protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            LOGD(TAG + ".DeviceLoader", "onPostExecute");
            if(connectedDeviceList.size() > 0) {
                msg_text.setText(getString(R.string.select_connected_device_msg));
            }
            dialog.dismiss();
            simpleAdapter.setItemList(result);
            simpleAdapter.notifyDataSetChanged();
        }

        @Override protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.searching_devices_lbl));
            dialog.show();
        }

        @Override protected List<String> doInBackground(String... params) {
            connectedDeviceList.clear();
            try {
                UserDto userDto = HttpConnection.getInstance().getData(UserDto.class, params[0], MediaType.JSON);
                for(DeviceDto deviceDto : userDto.getConnectedDevices()) {
                    if(!Utils.getDeviceName().toLowerCase().equals(
                            deviceDto.getDeviceName().toLowerCase()))
                        connectedDeviceList.add(deviceDto.getDeviceName());
                }
            } catch (Exception ex) { ex.printStackTrace();}
            return connectedDeviceList;
        }
    }

    public class SimpleAdapter extends ArrayAdapter<String> {

        private List<String> itemList;
        private Context context;

        public SimpleAdapter(List<String> itemList, Context ctx) {
            super(ctx, R.layout.adapter_list_item, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public String getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        public long getItemId(int position) {
            if (itemList != null) return itemList.get(position).hashCode();
            return 0;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            String tag = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.adapter_list_item, null);
            }
            TextView text = (TextView) itemView.findViewById(R.id.list_item);
            text.setText(tag);
            return itemView;
        }

        public List<String> getItemList() {
            return itemList;
        }

        public void setItemList(List<String> itemList) {
            this.itemList = itemList;
        }

    }

}
