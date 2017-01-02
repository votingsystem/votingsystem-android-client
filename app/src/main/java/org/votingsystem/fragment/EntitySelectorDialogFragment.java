package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
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
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.TrustedEntitiesDto;
import org.votingsystem.util.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EntitySelectorDialogFragment extends DialogFragment {

    public static final String TAG = EntitySelectorDialogFragment.class.getSimpleName();

    private SimpleAdapter simpleAdapter;

    public static void showDialog(String dialogCaller, List<TrustedEntitiesDto.EntityDto> entityList,
                                  String caption, FragmentManager manager) {
        EntitySelectorDialogFragment dialogFragment = new EntitySelectorDialogFragment();
        Bundle args = new Bundle();
        args.putString(Constants.CALLER_KEY, dialogCaller);
        args.putString(Constants.CAPTION_KEY, caption);
        args.putSerializable(Constants.LIST_STATE_KEY, (Serializable) entityList);
        dialogFragment.setArguments(args);
        dialogFragment.show(manager, TAG);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String dialogCaller = getArguments().getString(Constants.CALLER_KEY);
        final List<TrustedEntitiesDto.EntityDto> serviceList = (List<TrustedEntitiesDto.EntityDto>)
                getArguments().getSerializable(Constants.LIST_STATE_KEY);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.list_selector_dialog, null);
        TextView dialogCaption = (TextView) view.findViewById(R.id.caption);
        dialogCaption.setText(getArguments().getString(Constants.CAPTION_KEY));
        ListView service_list_view = (ListView) view.findViewById(R.id.service_list_view);
        simpleAdapter = new SimpleAdapter(new ArrayList<String>(), getActivity());
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);
        service_list_view.setAdapter(simpleAdapter);
        service_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                Intent intent = new Intent(dialogCaller);
                ResponseDto responseDto = ResponseDto.OK().setServiceCaller(TAG)
                        .setMessage(simpleAdapter.getItemList().get(position));
                intent.putExtra(Constants.RESPONSE_KEY, responseDto);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        simpleAdapter.notifyDataSetChanged();
        if(savedInstanceState == null) {
            new EntityLoaderTask(serviceList).execute();
        } else {
            simpleAdapter.setItemList((List<String>) savedInstanceState.getSerializable(
                    Constants.LIST_STATE_KEY));
            simpleAdapter.notifyDataSetChanged();
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.LIST_STATE_KEY, (Serializable) simpleAdapter.getItemList());
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
    public class EntityLoaderTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = EntityLoaderTask.class.getSimpleName();

        private List<TrustedEntitiesDto.EntityDto> entityList;
        private List<String> loadedEntityList;

        public EntityLoaderTask(List<TrustedEntitiesDto.EntityDto> entityList) {
            this.entityList = entityList;
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            ResponseDto responseDto = null;
            loadedEntityList = new ArrayList<>();
            for(TrustedEntitiesDto.EntityDto entityId : entityList) {
                MetadataDto metadata = App.getInstance().getSystemEntity(entityId.getId(), true);
                if(metadata != null)
                    loadedEntityList.add(entityId.getId());
            }
            return responseDto.OK();
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG, "onPostExecute - num. entities loaded: " + loadedEntityList.size());
            simpleAdapter.setItemList(loadedEntityList);
            simpleAdapter.notifyDataSetChanged();
            ProgressDialogFragment.hide(getFragmentManager());
        }
    }

}
