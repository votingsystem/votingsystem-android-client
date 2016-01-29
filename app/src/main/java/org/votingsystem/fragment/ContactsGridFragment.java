package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.activity.ContactPagerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DBUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.util.LogUtils.LOGD;

public class ContactsGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = ContactsGridFragment.class.getSimpleName();

    public static final int CONTACT_PICKER = 1;

    public enum Mode {SEARCH, CONTACT}

    private View rootView;
    private GridView gridView;
    private String queryStr = null;
    private Mode mode = Mode.CONTACT;
    private AppVS appVS = null;
    private String broadCastId = ContactsGridFragment.class.getSimpleName();
    private static final int loaderId = 0;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);
    private UserVSDto contactUserVS;
    private List<UserVSDto> userVSList = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            setProgressDialogVisible(false);
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode()) {
                if(gridView.getAdapter().getCount() == 0)
                    rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }
            if(responseVS != null && responseVS.getTypeVS() == TypeVS.REPRESENTATIVE_REVOKE) {
                MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getNotificationMessage(), getFragmentManager());
            } else if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
            }
        }
    };


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        Bundle data = getArguments();
        if (data != null && data.containsKey(SearchManager.QUERY)) {
            queryStr = data.getString(SearchManager.QUERY);
        }
        if(savedInstanceState != null) {
            mode = (Mode) savedInstanceState.getSerializable(ContextVS.STATE_KEY);
        }
        LOGD(TAG + ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId +
                " - queryStr: " + queryStr + " - mode: " + mode);
        if(queryStr != null && mode != Mode.SEARCH) {
            mode = Mode.SEARCH;
            new ContactsFetcher(null).execute(queryStr);
        }
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        if(Mode.CONTACT == mode) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.contacts_lbl));
        } else {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.search_result));
        }
        rootView = inflater.inflate(R.layout.contacts_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        if(savedInstanceState != null) {
            String dtoStr = savedInstanceState.getString(ContextVS.DTO_KEY);
            if(dtoStr != null) {
                try {
                    userVSList = JSON.readValue(dtoStr, new TypeReference<List<UserVSDto>>(){});
                    ContactListAdapter adapter = new ContactListAdapter(userVSList, appVS);
                    gridView.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                contactUserVS = (UserVSDto) savedInstanceState.getSerializable(ContextVS.USER_KEY);
                Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
                gridView.onRestoreInstanceState(gridState);
            }
        } else {
            ContactDBListAdapter adapter = new ContactDBListAdapter(getActivity(), null,false);
            gridView.setAdapter(adapter);
        }
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        getLoaderManager().initLoader(loaderId, null, this);
        return rootView;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult(..)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode + " - data: " + data);
        if (resultCode != Activity.RESULT_OK) return;
        if (data == null) return;
        switch (requestCode) {
            case CONTACT_PICKER:
                final UserVSDto userVS = DBUtils.extractInfoFromContactPickerIntent(data, getActivity());
                if(userVS != null && userVS.getId() == null) {
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.error_lbl),
                        getString(R.string.contactvs_not_found_msg, userVS.getName()),
                        getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                fetchUserVS(userVS);
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl), null);
                    UIUtils.showMessageDialog(builder);
                } else launchPager(null, userVS);
                break;
        }
    }

    private void fetchUserVS(UserVSDto userVS) {
        this.contactUserVS = userVS;
        new ContactsFetcher(userVS).execute("");
    }

    @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) { }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if(isVisible && isProgressDialogVisible.get()) return;
        isProgressDialogVisible.set(isVisible);
        if(!isAdded()) return;
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler(){
            @Override public void handleMessage(Message msg) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(
                            getString(R.string.loading_data_msg),
                            getString(R.string.loading_info_msg),
                            getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putSerializable(ContextVS.STATE_KEY, mode);
        outState.putSerializable(ContextVS.USER_KEY, contactUserVS);
        if(userVSList != null) {
            try {
                outState.putString(ContextVS.DTO_KEY, JSON.writeValueAsString(userVSList));
            } catch (Exception ex) { ex.printStackTrace();}
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contacts_grid_fragment, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.open_contacts:
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI), CONTACT_PICKER);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG + ".onListItemClick", "Clicked item - position:" + position + " - id: " + id);
        if(gridView.getAdapter() instanceof CursorAdapter) {
            launchPager(position, null);
        } else {
            launchPager(null, userVSList.get(position));
        }
    }

    private void launchPager(Integer position, UserVSDto userVS) {
        Intent intent = new Intent(getActivity(), ContactPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        intent.putExtra(ContextVS.STATE_KEY, mode);
        try {
            intent.putExtra(ContextVS.DTO_KEY, JSON.writeValueAsString(userVSList));
        } catch (Exception ex) { ex.printStackTrace();}
        intent.putExtra(ContextVS.USER_KEY, userVS);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "onCreateLoader");
        return new CursorLoader(this.getActivity(),
                UserContentProvider.CONTENT_URI, null, UserContentProvider.TYPE_COL + " =? ",
                new String[]{UserVSDto.Type.CONTACT.toString()}, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount());
        if(!(gridView.getAdapter() instanceof CursorAdapter)) {
            ContactDBListAdapter adapter = new ContactDBListAdapter(getActivity(), null,false);
            gridView.setAdapter(adapter);
        }
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        if(gridView.getAdapter() instanceof CursorAdapter)
            ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    public class ContactDBListAdapter extends CursorAdapter {

        public ContactDBListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.contact_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                String fullName = cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL));
                String nif = cursor.getString(cursor.getColumnIndex(UserContentProvider.NIF_COL));
                ((TextView)view.findViewById(R.id.fullname)).setText(fullName);
                ((TextView) view.findViewById(R.id.nif)).setText(nif);
            }
        }
    }

    public class ContactListAdapter extends BaseAdapter {

        private List<UserVSDto> itemList;
        private Context context;

        public ContactListAdapter(List<UserVSDto> itemList, Context ctx) {
            this.itemList = itemList;
            this.context = ctx;
        }

        @Override public int getCount() {
            return itemList.size();
        }

        @Override public Object getItem(int position) {
            return itemList.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            UserVSDto userVS = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.contact_card, null);
            }
            ((TextView)itemView.findViewById(R.id.fullname)).setText(userVS.getName());
            ((TextView) itemView.findViewById(R.id.nif)).setText(userVS.getNIF());
            return itemView;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class ContactsFetcher extends AsyncTask<String, String, ResponseVS> {

        private String phone, email;

        public ContactsFetcher(UserVSDto userVS) {
            if(userVS != null) {
                this.phone = userVS.getPhone();
                this.email = userVS.getEmail();
            }
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected ResponseVS doInBackground(String... params) {
            String contactsURL = null;
            if(phone != null || email != null) {
                contactsURL = appVS.getCurrencyServer().getSearchServiceURL(phone, email);
            } else {
                contactsURL = appVS.getCurrencyServer().getSearchServiceURL(params[0]);
            }
            return HttpHelper.getData(contactsURL, ContentTypeVS.JSON);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    if(phone != null || email != null) {
                        userVSList = null;
                        UserVSDto userVS = (UserVSDto) responseVS.getMessage(UserVSDto.class);
                        if(contactUserVS != null) userVS.setContactURI(contactUserVS.getContactURI());
                        launchPager(null, userVS);
                    } else {
                        ResultListDto<UserVSDto> resultList = ((ResultListDto<UserVSDto>) responseVS
                                .getMessage(new TypeReference<ResultListDto<UserVSDto>>() {}));
                        userVSList = resultList.getResultList();
                        if(userVSList.size() > 0)
                            rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
                        ContactListAdapter adapter = new ContactListAdapter(userVSList, appVS);
                        gridView.setAdapter(adapter);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(false);
        }
    }
}