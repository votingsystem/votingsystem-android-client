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

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.fragment.EventVSFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;

import java.util.Collections;
import java.util.List;

import static org.votingsystem.util.ContextVS.FRAGMENT_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSSearchActivity extends AppCompatActivity {

    public static final String TAG = EventVSSearchActivity.class.getSimpleName();

    private List<EventVSDto> eventVSList;
    private TextView search_query;
    private TextView num_results;
    private GridView gridView;
    private SearchView searchView;
    private EventVSDto.State eventState;
    private String queryStr;
    private ResponseVS responseVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventvs_grid);
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_vs,
                (ViewGroup) findViewById(R.id.toolbar_container)).findViewById(R.id.toolbar_vs);
        searchView = (SearchView) toolbar.findViewById(R.id.search_view);
        searchView.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        gridView = (GridView) findViewById(R.id.gridview);
        search_query = (TextView) findViewById(R.id.search_query);
        num_results = (TextView) findViewById(R.id.num_results);
        eventState = (EventVSDto.State) getIntent().getExtras()
                .getSerializable(ContextVS.EVENT_STATE_KEY);
        findViewById(R.id.search_query_Container).setVisibility(View.VISIBLE);

        if (getIntent().hasExtra(SearchManager.QUERY)) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                searchView.setQuery(query, false);
                searchFor(query);
            }
        }
        if(savedInstanceState != null) {
            responseVS = savedInstanceState.getParcelable(ContextVS.RESPONSEVS_KEY);
            if(responseVS != null) {
                ResultListDto<EventVSDto> resultListDto = null;
                try {
                    resultListDto = (ResultListDto<EventVSDto>)
                            responseVS.getMessage(new TypeReference<ResultListDto<EventVSDto>>() {});
                    eventVSList = resultListDto.getResultList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        setupSearchView();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private String getSearchHintMsg(EventVSDto.State eventState) {
        if(eventState == null) return getString(R.string.search_hint);
        switch (eventState) {
            case ACTIVE: return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                    getString(R.string.open_voting_lbl);
            case PENDING: return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                    getString(R.string.pending_voting_lbl);
            case TERMINATED: return getString(R.string.search_hint) + " " + getString(R.string.polls_lbl) + " " +
                    getString(R.string.closed_voting_lbl);
            default: return getString(R.string.search_hint);
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
        ImageView searchCloseButton = (ImageView)searchView.findViewById(searchCloseButtonId);
        searchCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clearSearchResult();
            }
        });
        if (eventVSList != null) showSearchResult(eventVSList);
    }

    private void searchFor(String query) {
        searchView.clearFocus();
        this.queryStr = query;
        new SearchTask(query, eventState).execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ContextVS.RESPONSEVS_KEY, responseVS);
    }


    public class SearchTask extends AsyncTask<String, Void, Void> {

        public final String TAG = SearchTask.class.getSimpleName();

        private EventVSDto.State eventState;
        private String queryStr;

        public SearchTask(String queryStr, EventVSDto.State eventState) {
            this.queryStr = queryStr;
            this.eventState = eventState;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.connecting_caption),
                    getString(R.string.search_msg), getSupportFragmentManager());
        }

        @Override protected Void doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "doInBackground");
            String serviceURL = AppVS.getInstance().getAccessControl().getSearchServiceURL(null, null, queryStr,
                    EventVSDto.Type.ELECTION, eventState);
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            try {
                ResultListDto<EventVSDto> resultListDto = (ResultListDto<EventVSDto>)
                        responseVS.getMessage(new TypeReference<ResultListDto<EventVSDto>>() {});
                eventVSList = resultListDto.getResultList();

            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) { }

        @Override  protected void onPostExecute(Void responseVS) {
            LOGD(TAG + "SearchTask.onPostExecute() ", " - statusCode");
            if(eventVSList != null) showSearchResult(eventVSList);
            ProgressDialogFragment.hide(getSupportFragmentManager());
        }
    }

    private void clearSearchResult() {
        searchView.setQuery("", false);
        num_results.setText("");
        search_query.setText("");
    }

    private void showSearchResult(List<EventVSDto> eventVSList) {
        num_results.setText(getString(R.string.num_matches, eventVSList.size()));
        EventListAdapter eventListAdapter = new EventListAdapter(eventVSList, EventVSSearchActivity.this);
        gridView.setAdapter(eventListAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        String searchMsg = getString(R.string.eventvs_election_search_msg, queryStr,
                MsgUtils.getVotingStateMessage(eventState, EventVSSearchActivity.this));
        search_query.setText(searchMsg);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
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
        EventVSDto eventVS = eventVSList.get(position);
        try {
            Intent intent = new Intent(this, FragmentContainerActivity.class);
            intent.putExtra(FRAGMENT_KEY, EventVSFragment.class.getName());
            intent.putExtra(ContextVS.EVENTVS_KEY, JSON.writeValueAsString(eventVS));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class EventListAdapter extends ArrayAdapter<EventVSDto> {

        private List<EventVSDto> itemList;
        private Context context;

        public EventListAdapter(List<EventVSDto> itemList, Context ctx) {
            super(ctx, R.layout.eventvs_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public EventVSDto getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        public long getItemId(int position) {
            if (itemList != null) return itemList.get(position).getId();
            return 0;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            EventVSDto eventVS = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.eventvs_card, null);
            }
            int state_color = R.color.frg_vs;
            String tameInfoMsg = null;
            switch(eventVS.getState()) {
                case ACTIVE:
                    state_color = R.color.active_vs;
                    tameInfoMsg = getString(R.string.remaining_lbl, DateUtils.
                            getElapsedTimeStr(eventVS.getDateFinish()));
                    break;
                case CANCELLED:
                case TERMINATED:
                    state_color = R.color.terminated_vs;
                    tameInfoMsg = getString(R.string.voting_closed_lbl);
                    break;
                case PENDING:
                    state_color = R.color.pending_vs;
                    tameInfoMsg = getString(R.string.pending_lbl, DateUtils.
                            getElapsedTimeStr(eventVS.getDateBegin()));
                    break;
            }
            if(eventVS.getUserVS() != null) {
                ((TextView)itemView.findViewById(R.id.publisher)).setText(eventVS.getUserVS());
            }
            ((TextView)itemView.findViewById(R.id.subject)).setText(eventVS.getSubject());
            ((TextView)itemView.findViewById(R.id.subject)).setTextColor(getResources().getColor(state_color));
            TextView time_info = ((TextView)itemView.findViewById(R.id.time_info));
            time_info.setText(tameInfoMsg);
            time_info.setTextColor(getResources().getColor(state_color));
            return itemView;
        }

        public List<EventVSDto> getItemList() {
            return itemList;
        }

        public void setItemList(List<EventVSDto> itemList) {
            this.itemList = itemList;
        }

    }

}