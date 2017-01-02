package org.votingsystem.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.fragment.ElectionFragment;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XmlReader;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.Constants.FRAGMENT_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionSearchActivity extends AppCompatActivity {

    public static final String TAG = ElectionSearchActivity.class.getSimpleName();

    private List<ElectionDto> elections;
    private TextView search_query;
    private TextView num_results;
    private GridView gridView;
    private SearchView searchView;
    private ElectionDto.State eventState;
    private String queryStr;
    private ResponseDto response;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.election_grid);
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.app_toolbar,
                (ViewGroup) findViewById(R.id.toolbar_container)).findViewById(R.id.toolbar_vs);
        searchView = (SearchView) toolbar.findViewById(R.id.search_view);
        searchView.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        gridView = (GridView) findViewById(R.id.gridview);
        search_query = (TextView) findViewById(R.id.search_query);
        num_results = (TextView) findViewById(R.id.num_results);
        eventState = (ElectionDto.State) getIntent().getExtras()
                .getSerializable(Constants.ELECTION_STATE_KEY);
        findViewById(R.id.search_query_Container).setVisibility(View.VISIBLE);

        if (getIntent().hasExtra(SearchManager.QUERY)) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                searchView.setQuery(query, false);
                searchFor(query);
            }
        }
        if (savedInstanceState != null) {
            response = savedInstanceState.getParcelable(Constants.RESPONSE_KEY);
            if (response != null && ResponseDto.SC_OK == response.getStatusCode()) {
                ResultListDto<ElectionDto> resultListDto = XmlReader.readElections(response.getMessageBytes());
                elections = new ArrayList<>(resultListDto.getResultList());
            }
        }
        setupSearchView();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private String getSearchHintMsg(ElectionDto.State eventState) {
        if (eventState == null) return getString(R.string.search_hint);
        switch (eventState) {
            case ACTIVE:
                return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                        getString(R.string.open_voting_lbl);
            case PENDING:
                return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                        getString(R.string.pending_voting_lbl);
            case TERMINATED:
                return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                        getString(R.string.closed_voting_lbl);
            default:
                return getString(R.string.search_hint);
        }
    }

    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getSearchHintMsg(eventState));
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_ACTION_SEARCH |
                EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchFor(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        int searchCloseButtonId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_close_btn", null, null);
        ImageView searchCloseButton = (ImageView) searchView.findViewById(searchCloseButtonId);
        searchCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSearchResult();
            }
        });
        if (elections != null) showSearchResult(elections);
    }

    private void searchFor(String query) {
        searchView.clearFocus();
        this.queryStr = query;
        new SearchTask(query, eventState).execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Constants.RESPONSE_KEY, response);
    }


    public class SearchTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = SearchTask.class.getSimpleName();

        private ElectionDto.State eventState;
        private String queryStr;

        public SearchTask(String queryStr, ElectionDto.State eventState) {
            this.queryStr = queryStr;
            this.eventState = eventState;
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.connecting_caption),
                    getString(R.string.search_msg), getSupportFragmentManager());
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "doInBackground");
            SystemEntityDto votingServiceProvider = ((App) getApplicationContext()).getVotingServiceProvider();
            if (votingServiceProvider == null) {
                LOGD(TAG + ".doInBackground", "missing connection with voting service provider");
                return null;
            }
            String serviceURL = OperationType.getSearchServiceURL(votingServiceProvider.getId(),
                    0, null, queryStr, eventState);
            response = HttpConn.getInstance().doGetRequest(serviceURL, ContentType.XML);
            try {
                if(ResponseDto.SC_OK == response.getStatusCode()) {
                    ResultListDto<ElectionDto> resultListDto = XmlReader.readElections(response.getMessageBytes());
                    elections = new ArrayList<>(resultListDto.getResultList());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG + "SearchTask.onPostExecute() ", " - statusCode");
            if(ResponseDto.SC_OK != response.getStatusCode()) {
                MessageDialogFragment.showDialog(response, getSupportFragmentManager());
            } else if(elections != null) showSearchResult(elections);
            ProgressDialogFragment.hide(getSupportFragmentManager());
        }
    }

    private void clearSearchResult() {
        showSearchResult(new ArrayList<ElectionDto>());
        searchView.setQuery("", false);
        num_results.setText("");
        search_query.setText("");
    }

    private void showSearchResult(List<ElectionDto> electionList) {
        num_results.setText(getString(R.string.num_matches, electionList.size()));
        EventListAdapter eventListAdapter = new EventListAdapter(electionList, ElectionSearchActivity.this);
        gridView.setAdapter(eventListAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        String searchMsg = getString(R.string.election_search_msg, queryStr,
                MsgUtils.getVotingStateMessage(eventState, ElectionSearchActivity.this));
        search_query.setText(searchMsg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        ElectionDto election = elections.get(position);
        try {
            Intent intent = new Intent(this, FragmentContainerActivity.class);
            intent.putExtra(FRAGMENT_KEY, ElectionFragment.class.getName());
            intent.putExtra(Constants.ELECTION_KEY, ObjectUtils.serializeObject(election));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class EventListAdapter extends ArrayAdapter<ElectionDto> {

        private List<ElectionDto> itemList;
        private Context context;

        public EventListAdapter(List<ElectionDto> itemList, Context ctx) {
            super(ctx, R.layout.election_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public ElectionDto getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        public long getItemId(int position) {
            if (itemList != null) return itemList.get(position).getId();
            return 0;
        }

        @Override
        public View getView(int position, View itemView, ViewGroup parent) {
            ElectionDto election = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.election_card, null);
            }
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
            ((TextView) itemView.findViewById(R.id.subject)).setText(election.getSubject());
            ((TextView) itemView.findViewById(R.id.subject)).setTextColor(getResources().getColor(state_color));
            TextView time_info = ((TextView) itemView.findViewById(R.id.time_info));
            time_info.setText(tameInfoMsg);
            time_info.setTextColor(getResources().getColor(state_color));
            return itemView;
        }

        public List<ElectionDto> getItemList() {
            return itemList;
        }

        public void setItemList(List<ElectionDto> itemList) {
            this.itemList = itemList;
        }

    }

}