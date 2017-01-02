package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

import org.votingsystem.android.R;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.PrefUtils;

import java.util.List;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SelectVotingServiceDialogFragment extends DialogFragment {

    public static final String TAG = SelectVotingServiceDialogFragment.class.getSimpleName();

    public static void showDialog(String dialogCaller, FragmentManager manager) {
        SelectVotingServiceDialogFragment dialogFragment = new SelectVotingServiceDialogFragment();
        Bundle args = new Bundle();
        args.putString(Constants.CALLER_KEY, dialogCaller);
        dialogFragment.setArguments(args);
        dialogFragment.show(manager, TAG);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String dialogCaller = getArguments().getString(Constants.CALLER_KEY);
        final List<String> serviceList = PrefUtils.getVotingServiceProviders();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.list_selector_dialog, null);
        ListView service_list_view = (ListView) view.findViewById(R.id.service_list_view);

        SimpleAdapter simpleAdapter = new SimpleAdapter(serviceList, getActivity());
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);
        service_list_view.setAdapter(simpleAdapter);
        service_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                Intent intent = new Intent(dialogCaller);
                ResponseDto responseDto = ResponseDto.OK().setServiceCaller(TAG)
                        .setMessage(serviceList.get(position));
                intent.putExtra(Constants.RESPONSE_KEY, responseDto);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        simpleAdapter.notifyDataSetChanged();
        return dialog;
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
