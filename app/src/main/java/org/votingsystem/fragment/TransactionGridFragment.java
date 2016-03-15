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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.activity.TransactionPagerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.TransactionContentProvider;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.service.PaymentService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

public class TransactionGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = TransactionGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private TransactionListAdapter adapter = null;
    private AppVS appVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private String broadCastId = TransactionGridFragment.class.getName();
    private static final int loaderId = 0;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        }
    };

    public static Fragment newInstance(Bundle args) {
        TransactionGridFragment fragment = new TransactionGridFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void launchUpdateUserInfoService() {
        LOGD(TAG + ".launchUpdateUserInfoService", "");
        try {
            Intent startIntent = new Intent(getActivity(), PaymentService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CURRENCY_ACCOUNTS_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            setProgressDialogVisible(true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        LOGD(TAG +  ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.grid_container, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new TransactionListAdapter(getActivity(), null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        if(savedInstanceState == null) launchUpdateUserInfoService();
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
        }
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler(){
            @Override public void handleMessage(Message msg) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(getString(R.string.loading_info_msg),
                            getString(R.string.loading_data_msg), getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) { }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.currency_accounts, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
        List<String> transactionWeekList = TransactionContentProvider.getTransactionWeekList (
                (AppVS)getActivity().getApplicationContext());
        for(final String weekLbl: transactionWeekList) {
            MenuItem item = menu.add (weekLbl);
            item.setOnMenuItemClickListener (new MenuItem.OnMenuItemClickListener(){
                @Override public boolean onMenuItemClick (MenuItem item){
                    LOGD(TAG +  ".onMenuItemClick(..)", "click on weekLbl: " + weekLbl);
                    return true;
                }
            });
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "position: " + position + " - id: " + id);
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(), TransactionPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "");
        String selection = TransactionContentProvider.WEEK_LAPSE_COL + " =? ";
        CursorLoader loader = new CursorLoader(getActivity(),
                TransactionContentProvider.CONTENT_URI, null, selection,
                new String[]{appVS.getCurrentWeekLapseId()}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        setProgressDialogVisible(false);
        if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
        firstVisiblePosition = null;
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
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

    public class TransactionListAdapter extends CursorAdapter {

        private LayoutInflater inflater = null;

        public TransactionListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.transaction_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] jsonBytes = cursor.getBlob(cursor.getColumnIndex(
                        TransactionContentProvider.JSON_COL));
                if(jsonBytes != null) {
                    TransactionDto transaction = null;
                    try {
                        transaction = JSON.readValue(jsonBytes, TransactionDto.class);
                    } catch (IOException e) { e.printStackTrace(); }
                    String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                            TransactionContentProvider.WEEK_LAPSE_COL));
                    Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
                    TextView transaction_type = (TextView) view.findViewById(R.id.transaction_type);
                    transaction_type.setText(transaction.getDescription(getActivity(),
                            transaction.getType()));
                    TextView week_lapse = (TextView) view.findViewById(R.id.week_lapse);
                    week_lapse.setText(DateUtils.getDayWeekDateStr(transaction.getDateCreated(), "HH:mm"));
                    TextView amount = (TextView) view.findViewById(R.id.amount);
                    amount.setText(transaction.getAmount().toPlainString());
                    TextView currency = (TextView) view.findViewById(R.id.currencyCode);
                    currency.setText(transaction.getCurrencyCode());
                    ((ImageView)view.findViewById(R.id.transaction_icon)).setImageResource(
                            transaction.getIconId(getActivity()));
                }
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
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