package org.votingsystem.fragment;

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.App;
import org.votingsystem.activity.ElectionPagerActivity;
import org.votingsystem.activity.ElectionSearchActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ElectionContentProvider;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.service.ElectionService;
import org.votingsystem.ui.ElectionSpinnerAdapter;
import org.votingsystem.ui.EndlessScrollListener;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.UIUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = ElectionGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private EventListAdapter mAdapter = null;
    private ElectionDto.State electionState;
    private App app = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = 0;
    private static final int loaderId = 0;
    private String broadCastId = null;
    private Long numElectionsInDb;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
        setProgressDialogVisible(false);
        if (ResponseDto.SC_CONNECTION_TIMEOUT == responseDto.getStatusCode()) {
            if (gridView.getAdapter().getCount() == 0)
                rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
        if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
            getLoaderManager().restartLoader(loaderId, null, ElectionGridFragment.this);
        } else {
            MessageDialogFragment.showDialog(
                    responseDto, getFragmentManager());
        }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        super.onCreate(savedInstanceState);

        View spinnerContainer = inflater.inflate(R.layout.spinner_election_actionbar, null);
        ElectionSpinnerAdapter mTopLevelSpinnerAdapter = new ElectionSpinnerAdapter(true, inflater);
        mTopLevelSpinnerAdapter.clear();
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.open_voting_lbl), false, 0);
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.pending_voting_lbl), false, 0);
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.closed_voting_lbl), false, 0);
        Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.actionbar_spinner);
        spinner.setAdapter(mTopLevelSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long itemId) {
                ElectionDto.State newElectionState = null;
                if (position == 0)
                    newElectionState = ElectionDto.State.ACTIVE;
                else if (position == 1)
                    newElectionState = ElectionDto.State.PENDING;
                else if (position == 2)
                    newElectionState = ElectionDto.State.TERMINATED;
                LOGD(TAG, "onItemSelected - position: " + position + " - newElectionState: " + newElectionState);
                if(newElectionState != electionState) {
                    electionState = newElectionState;
                    numElectionsInDb = null;
                    firstVisiblePosition = 0;
                    fetchItems(0L, false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setCustomView(spinnerContainer, lp);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(getString(R.string.polls_lbl));

        rootView = inflater.inflate(R.layout.election_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        mAdapter = new EventListAdapter(getActivity(), null, false);
        gridView.setAdapter(mAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });

        setHasOptionsMenu(true);
        broadCastId = ElectionGridFragment.class.getSimpleName();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        if (savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(Constants.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(Constants.OFFSET_KEY);
            firstVisiblePosition = savedInstanceState.getInt(Constants.CURSOR_POSITION_KEY);
            numElectionsInDb = savedInstanceState.getLong(Constants.NUM_ITEMS_KEY);
        } else numElectionsInDb = 0L;
        gridView.setOnScrollListener(new EndlessScrollListener() {
            @Override public boolean onLoadMore(int page, int totalItemsCount) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to your AdapterView
                LOGD(TAG, "EndlessScrollListener - onLoadMore - page: " + page +
                        " - totalItemsCount: " + totalItemsCount);
                fetchItems(Integer.valueOf(totalItemsCount).longValue(), false);
                return true; // ONLY if more data is actually being loaded; false otherwise.
            }
        });
        return rootView;
    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
        if (subTitle != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(subTitle);
        if (iconId != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setLogo(iconId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.election_grid, menu);
        if(PrefUtils.getVotingServiceProviders().size() < 2)
            menu.removeItem(R.id.select_voting_service);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler() {
            @Override
            public void handleMessage(Message msg) {
            if (getActivity() != null) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                            getString(R.string.loading_info_msg), getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    public ElectionDto.State getState() {
        return electionState;
    }

    public void fetchItems(Long offset, boolean forceReload) {
        Long numTotalElections = null;
        if(App.getInstance().getVotingServiceProvider() != null) {
            numTotalElections = ElectionContentProvider.getNumTotalElections(
                    App.getInstance().getVotingServiceProvider().getId(), electionState);
            if(numElectionsInDb == null)
                numElectionsInDb = ElectionContentProvider.getNumElectionsInDb(
                    App.getInstance().getVotingServiceProvider().getId(), electionState);
        }
        LOGD(TAG, "fetchItems - offset: " + offset + " - electionState: " + electionState +
                " - numElections: " + numTotalElections + " - numElectionsInDb: " + numElectionsInDb);
        if(forceReload || numTotalElections == null || ((numTotalElections > numElectionsInDb)) &&
                (offset + Constants.ELECTIONS_PAGE_SIZE > numElectionsInDb))  {
            setProgressDialogVisible(true);
            Intent startIntent = new Intent(getActivity(), ElectionService.class);
            startIntent.putExtra(Constants.STATE_KEY, electionState);
            startIntent.putExtra(Constants.OFFSET_KEY, offset);
            startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
            getActivity().startService(startIntent);
        } else {
            getLoaderManager().restartLoader(loaderId, null, this);
            ((CursorAdapter) gridView.getAdapter()).notifyDataSetChanged();
        }
        /*if (App.getInstance().isElectionStateLoaded(electionState) && offset < numElectionsInDb) {
            getLoaderManager().restartLoader(loaderId, null, this);
            ((CursorAdapter) gridView.getAdapter()).notifyDataSetChanged();
        } else if(isAdded()) {
            if(numElections == null || (numElections > offset)) {
                setProgressDialogVisible(true);
                Intent startIntent = new Intent(getActivity(), ElectionService.class);
                startIntent.putExtra(Constants.STATE_KEY, electionState);
                startIntent.putExtra(Constants.OFFSET_KEY, offset);
                startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
                getActivity().startService(startIntent);
            }
        }*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Constants.OFFSET_KEY, offset);
        outState.putLong(Constants.NUM_ITEMS_KEY, numElectionsInDb);
        outState.putParcelable(Constants.LIST_STATE_KEY,  gridView.onSaveInstanceState());
        outState.putInt(Constants.CURSOR_POSITION_KEY, firstVisiblePosition);
        LOGD(TAG + ".onSaveInstanceState", "outState: " + outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected(..)", " - Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId() + " - electionState: " + electionState);
        switch (item.getItemId()) {
            case R.id.update:
                fetchItems(offset, true);
                return true;
            case R.id.search_item:
                Intent searchStarter = new Intent(getActivity(), ElectionSearchActivity.class);
                searchStarter.putExtra(Constants.ELECTION_STATE_KEY, electionState);
                startActivityForResult(searchStarter, 0, null);
                return true;
            case R.id.select_voting_service:
                SelectVotingServiceDialogFragment.showDialog(broadCastId, this.getFragmentManager());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG + ".onListItemClick", "position:" + position + " - id: " + id);
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(), ElectionPagerActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION_KEY, position);
        intent.putExtra(Constants.ID_SERVICE_ENTITY_ID,
                App.getInstance().getVotingServiceProvider().getId());
        intent.putExtra(Constants.ELECTION_STATE_KEY, electionState.toString());
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG, "onCreateLoader - electionState: " + electionState);
        String selection = ElectionContentProvider.STATE_COL + "=? AND " +
                ElectionContentProvider.ENTITY_ID_COL + "= ? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                ElectionContentProvider.CONTENT_URI, null, selection,
                new String[]{electionState.toString(), app.getVotingServiceProvider().getId()}, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG, "onLoadFinished- electionState: " + electionState +
                " - numTotal: " + ElectionContentProvider.getNumTotalElections(
                        app.getVotingServiceProvider().getId(), electionState) +
                " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        setProgressDialogVisible(false);
        cursor.moveToPosition(firstVisiblePosition);
        ((CursorAdapter) gridView.getAdapter()).changeCursor(cursor);
        numElectionsInDb = Integer.valueOf(cursor.getCount()).longValue();
        if (numElectionsInDb == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "onLoaderReset");
        ((CursorAdapter) gridView.getAdapter()).swapCursor(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class EventListAdapter extends CursorAdapter {

        public EventListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.election_card, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            try {
                byte[] eventJSON = cursor.getBlob(cursor.getColumnIndex(
                        ElectionContentProvider.SERIALIZED_OBJECT_COL));
                ElectionDto election = (ElectionDto) ObjectUtils.deSerializeObject(eventJSON);
                int state_color = R.color.white_vs;
                String tameInfoMsg = null;
                switch (election.getState()) {
                    case ACTIVE:
                        state_color = R.color.active_vs;
                        tameInfoMsg = getString(R.string.remaining_lbl, DateUtils.
                                getElapsedTimeStr(election.getDateFinish()));
                        break;
                    case CANCELED:
                    case TERMINATED:
                        state_color = R.color.red_vs;
                        tameInfoMsg = getString(R.string.voting_closed_lbl);
                        break;
                    case PENDING:
                        state_color = R.color.orange_vs;
                        tameInfoMsg = getString(R.string.pending_lbl, DateUtils.
                                getElapsedTimeStr(election.getDateBegin()));
                        break;
                }
                ((TextView) view.findViewById(R.id.subject)).setTextColor(
                        getResources().getColor(state_color));
                ((TextView) view.findViewById(R.id.subject)).setText(election.getSubject());
                TextView time_info = ((TextView) view.findViewById(R.id.time_info));
                time_info.setText(tameInfoMsg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}