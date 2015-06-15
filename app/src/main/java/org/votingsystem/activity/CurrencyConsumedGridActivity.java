package org.votingsystem.activity;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.CurrencyContentProvider;
import org.votingsystem.model.Currency;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyConsumedGridActivity extends AppCompatActivity implements android.app.LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = CurrencyConsumedGridActivity.class.getSimpleName();

    private static final int loaderId = 0;
    private GridView gridView;
    private int currentItemPosition = -1;
    
    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_grid);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("CurrencyConsumedGridActivity");
        gridView = (GridView) findViewById(R.id.gridview);
        CurrencyListAdapter adapter = new CurrencyListAdapter(this, null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        //gridView.setOnScrollListener(this);
        getLoaderManager().initLoader(loaderId, null, this);
        getSupportActionBar().setTitle("OperationsGridActivity");
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "position: " + position + " - id: " + id);
        currentItemPosition = position;
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        openContextMenu(parent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
    }

    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, CurrencyContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount());
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {

    }

    public class CurrencyListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public CurrencyListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.currency_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                Currency currency = null;
                try {
                    currency = CurrencyContentProvider.getCurrency(cursor);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    getContentResolver().delete(CurrencyContentProvider.
                            getCurrencyURI(cursor.getLong(cursor.getColumnIndex(
                            CurrencyContentProvider.ID_COL))), null, null);
                    return;
                }
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView date_data = (TextView)view.findViewById(R.id.date_data);
                date_data.setText(DateUtils.getDayWeekDateSimpleStr(currency.getDateFrom()));

                TextView currency_state = (TextView) view.findViewById(R.id.currency_state);
                currency_state.setText(currency.getStateMsg(CurrencyConsumedGridActivity.this));
                currency_state.setTextColor(currency.getStateColor(CurrencyConsumedGridActivity.this));

                TextView amount = (TextView) view.findViewById(R.id.amount);
                amount.setText(currency.getAmount().toPlainString());
                amount.setTextColor(currency.getStateColor(CurrencyConsumedGridActivity.this));
                TextView currencyTextView = (TextView) view.findViewById(R.id.currencyCode);
                currencyTextView.setText(currency.getCurrencyCode().toString());
                currencyTextView.setTextColor(currency.getStateColor(CurrencyConsumedGridActivity.this));

                if(DateUtils.getCurrentWeekPeriod().inRange(currency.getDateTo())) {
                    TextView time_limited_msg = (TextView) view.findViewById(R.id.time_limited_msg);
                    time_limited_msg.setText(getString(R.string.lapse_lbl,
                            DateUtils.getDayWeekDateStr(currency.getDateTo())));
                }
                ((TextView) view.findViewById(R.id.tag_data)).setText(MsgUtils.getTagVSMessage(
                        currency.getTag()));
            }
        }
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}