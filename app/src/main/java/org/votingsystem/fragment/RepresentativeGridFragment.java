package org.votingsystem.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
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
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.votingsystem.App;
import org.votingsystem.activity.RepresentativePagerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserDto;
import org.votingsystem.service.RepresentativeService;
import org.votingsystem.util.Constants;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.util.LogUtils.LOGD;

public class RepresentativeGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = RepresentativeGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private RepresentativeListAdapter adapter = null;
    private String queryStr = null;
    private App app = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private String broadCastId = RepresentativeGridFragment.class.getSimpleName();
    private static final int loaderId = 0;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
        setProgressDialogVisible(false);
        if(ResponseDto.SC_CONNECTION_TIMEOUT == responseDto.getStatusCode()) {
            if(gridView.getAdapter().getCount() == 0)
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
        if(responseDto != null && responseDto.getOperationType() == OperationType.REPRESENTATIVE_REVOKE) {
            MessageDialogFragment.showDialog(responseDto.getStatusCode(), responseDto.getCaption(),
                    responseDto.getNotificationMessage(), getFragmentManager());
        } else if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            MessageDialogFragment.showDialog(responseDto, getFragmentManager());
        }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        Bundle data = getArguments();
        if (data != null && data.containsKey(SearchManager.QUERY)) {
            queryStr = data.getString(SearchManager.QUERY);
        }
        LOGD(TAG +  ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                getString(R.string.representative_list_lbl));
        rootView = inflater.inflate(R.layout.representative_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new RepresentativeListAdapter(getActivity(), null,false);
        gridView.setAdapter(adapter);
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
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(Constants.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(Constants.OFFSET_KEY);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) {
        if (gridView.getAdapter() == null || gridView.getAdapter().getCount() == 0) return ;
        /* maybe add a padding */
        boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;
        if(loadMore && !  isProgressDialogVisible.get() && offset <
                UserContentProvider.getNumTotalRepresentatives() &&
                totalItemCount < UserContentProvider.getNumTotalRepresentatives()) {
            LOGD(TAG +  ".onScroll", "loadMore - firstVisibleItem: " + firstVisibleItem +
                    " - visibleItemCount:" + visibleItemCount + " - totalItemCount:" + totalItemCount);
            firstVisiblePosition = firstVisibleItem;
            fetchItems(new Long(totalItemCount));
        }
    }

    public Long getOffset() {
        return this.offset;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        isProgressDialogVisible.set(isVisible);
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


    public void fetchItems(Long offset) {
        if(app.getAccessControl() == null) {
            Toast.makeText(app, app.getString(R.string.server_connection_error_msg,
                    app.getString(R.string.access_control_lbl)), Toast.LENGTH_LONG).show();
            return;
        }
        LOGD(TAG +  ".fetchItems", "offset: " + offset);
        if(isProgressDialogVisible.get()) return;
        else setProgressDialogVisible(true);
        Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
        startIntent.putExtra(Constants.URL_KEY, app.getAccessControl().
                getRepresentativesURL(offset, Constants.REPRESENTATIVE_PAGE_SIZE));
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.ITEMS_REQUEST);
        getActivity().startService(startIntent);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.representative_grid, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.update:
                fetchItems(offset);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "Clicked item - position:" + position + " -id: " + id);
        Intent intent = new Intent(getActivity(), RepresentativePagerActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "onCreateLoader");
        String selection = UserContentProvider.TYPE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                UserContentProvider.CONTENT_URI, null, selection,
                new String[]{UserDto.Type.REPRESENTATIVE.toString()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        if(UserContentProvider.getNumTotalRepresentatives() == null)
            fetchItems(offset);
        else {
            setProgressDialogVisible(false);
            if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
            firstVisiblePosition = null;
            ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
            if(cursor.getCount() == 0) {
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent intent = activity.getIntent();
        if(intent != null) {
            String query = null;
            if (Intent.ACTION_SEARCH.equals(intent)) {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            LOGD(TAG + ".onAttach()", "activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
    }

    public class RepresentativeListAdapter  extends CursorAdapter {

        public RepresentativeListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(
                    R.layout.representative_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                String fullName = cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL));
                int numRepresentations = cursor.getInt(cursor.getColumnIndex(
                        UserContentProvider.NUM_REPRESENTATIONS_COL));
                ((TextView)view.findViewById(R.id.representative_name)).setText(fullName);
                ((TextView) view.findViewById(R.id.representative_delegations)).setText(
                        context.getString(R.string.num_representations_lbl,
                        String.valueOf(numRepresentations)));
            }
        }

    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Constants.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(Constants.LIST_STATE_KEY, gridState);
        LOGD(TAG +  ".onSaveInstanceState", "outState: " + outState);
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

}