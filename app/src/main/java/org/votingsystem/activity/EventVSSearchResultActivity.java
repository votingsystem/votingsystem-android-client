package org.votingsystem.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.android.R;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.fragment.EventVSFragment;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ResponseVS;

import java.util.List;

import static org.votingsystem.util.ContextVS.FRAGMENT_KEY;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSSearchResultActivity extends ActionBarActivity {

    public static final String TAG = EventVSSearchResultActivity.class.getSimpleName();

    private List<EventVSDto> eventVSList;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventvs_grid);
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_vs,
                (ViewGroup) findViewById(R.id.toolbar_container)).findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        GridView gridView = (GridView) findViewById(R.id.gridview);
        TextView textView = (TextView) findViewById(R.id.search_query);
        Bundle args = getIntent().getExtras();
        EventVSDto.State eventState = (EventVSDto.State) args.getSerializable(ContextVS.EVENT_STATE_KEY);
        String queryStr = args.getString(ContextVS.QUERY_KEY);
        String searchMsg = getString(R.string.eventvs_election_search_msg, queryStr,
                MsgUtils.getVotingStateMessage(eventState, this));
        textView.setText(searchMsg);
        findViewById(R.id.search_query_Container).setVisibility(View.VISIBLE);
        ResponseVS responseVS = args.getParcelable(ContextVS.RESPONSEVS_KEY);
        try {
            ResultListDto<EventVSDto> resultListDto = (ResultListDto<EventVSDto>)
                    responseVS.getMessage(new TypeReference<ResultListDto<EventVSDto>>() {});
            eventVSList = resultListDto.getResultList();
            ((TextView) findViewById(R.id.num_results)).setText(getString(R.string.num_matches,
                    eventVSList.size()));
            EventListAdapter eventListAdapter = new EventListAdapter(eventVSList, this);
            gridView.setAdapter(eventListAdapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onListItemClick(parent, v, position, id);
                }
            });
        } catch(Exception ex) {
            ex.printStackTrace();
        }
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
            intent.putExtra(ContextVS.EVENTVS_KEY, JSON.getMapper().writeValueAsString(eventVS));
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
            ((LinearLayout)itemView.findViewById(R.id.subject_layout)).setBackgroundColor(
                    getResources().getColor(state_color));
            ((TextView)itemView.findViewById(R.id.subject)).setText(eventVS.getSubject());
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