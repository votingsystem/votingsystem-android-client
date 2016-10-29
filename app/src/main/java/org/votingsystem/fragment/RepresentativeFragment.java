package org.votingsystem.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import org.votingsystem.App;
import org.votingsystem.activity.RepresentativeDelegationActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserDto;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeFragment extends Fragment {

	public static final String TAG = RepresentativeFragment.class.getSimpleName();

    private static final int REPRESENTATIVE_DELEGATION   = 1;

    private View rootView;
    private String broadCastId = null;
    private Button selectButton;
    private Long representativeId;
    private UserDto representative;
    private ImageView representative_image;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
        if(OperationType.ITEM_REQUEST == responseDto.getOperationType()) {
            printRepresentativeData((UserDto) intent.getSerializableExtra(Constants.USER_KEY));
            setProgressDialogVisible(false);
        }
        }
    };

    public static Fragment newInstance(Long representativeId) {
        RepresentativeFragment fragment = new RepresentativeFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.ITEM_ID_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        representativeId =  getArguments().getLong(Constants.ITEM_ID_KEY);
        broadCastId = RepresentativeFragment.class.getSimpleName() + "_" + representativeId;
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        rootView = inflater.inflate(R.layout.representative, container, false);
        representative_image = (ImageView) rootView.findViewById(R.id.representative_image);
        selectButton = (Button) rootView.findViewById(R.id.select_representative_button);
        selectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RepresentativeDelegationActivity.class);
                intent.putExtra(Constants.USER_KEY, representative);
                startActivityForResult(intent, REPRESENTATIVE_DELEGATION);
            }
        });
        selectButton.setVisibility(View.GONE);
        setHasOptionsMenu(true);
        Cursor cursor = getActivity().getContentResolver().query(UserContentProvider.
                getUserURI(representativeId), null, null, null, null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            final UserDto representative = (UserDto) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
            if(representative.getDescription() != null) printRepresentativeData(representative);
            else requestRepresentativeData(representativeId);
        } else requestRepresentativeData(representativeId);
        return rootView;
    }

    private void requestRepresentativeData(Long representativeId) {
        setProgressDialogVisible(true);
        Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
        startIntent.putExtra(Constants.ITEM_ID_KEY, representativeId);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.ITEM_REQUEST);
        getActivity().startService(startIntent);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        String message = null;
        if(data != null) message = data.getStringExtra(Constants.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            MessageDialogFragment.showDialog(ResponseDto.SC_OK, getString(R.string.operation_ok_msg),
                    message, getFragmentManager());
        } else if(message != null) MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                    getString(R.string.operation_error_msg), message, getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    private void printRepresentativeData(UserDto representative) {
        if(representative == null) {
            LOGD(TAG + ".printRepresentativeData", "representative null");
            return;
        }
        this.representative = representative;
        if(representative.getImageBytes() != null)
            UIUtils.setImage(representative_image, representative.getImageBytes(), getActivity());
        else new ImageDownloaderTask(representativeId).execute();
        if(representative.getDescription() != null) {
            try {
                String representativeDescription = "<html style='background-color:#eeeeee;'>" +
                        new String(Base64.decode(representative.getDescription().getBytes(), Base64.NO_WRAP), "UTF-8") + "</html>";
                ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                        representativeDescription, "text/html; charset=UTF-8", "UTF-8");
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        selectButton.setVisibility(View.VISIBLE);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.representative, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class ImageDownloaderTask extends AsyncTask<String, Void, ResponseDto> {

        Long representativeId;

        public ImageDownloaderTask(Long representativeId) {
            this.representativeId = representativeId;
        }

        @Override protected void onPreExecute() { }

        @Override protected ResponseDto doInBackground(String... urls) {
            return  HttpConnection.getInstance().getData(
                    ((App) getActivity().getApplicationContext()).getAccessControl().
                    getRepresentativeImageURL(representativeId), null);
        }

        @Override  protected void onPostExecute(ResponseDto responseDto) {
            LOGD(TAG + "ImageDownloaderTask.onPostExecute() ", "statusCode: " +
                    responseDto.getStatusCode());
            if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
                UIUtils.setImage(representative_image, responseDto.getMessageBytes(), getActivity());
                representative.setImageBytes(responseDto.getMessageBytes());
                getActivity().getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
            } else MessageDialogFragment.showDialog(responseDto, getFragmentManager());
        }

    }

}