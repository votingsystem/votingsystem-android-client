package org.votingsystem.activity;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.OperationVSContentProvider;
import org.votingsystem.contentprovider.TransactionVSContentProvider;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.OperationVS;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVSGridActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = OperationVSGridActivity.class.getSimpleName();

    private static final int loaderId = 0;

    private GridView gridView;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operationvs_grid);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        gridView = (GridView) findViewById(R.id.gridview);
        OperationListAdapter adapter = new OperationListAdapter(this, null,false);
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
        //Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
    }


    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, OperationVSContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount());
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {

    }

    public class OperationListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public OperationListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.operationvs_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                OperationVS operation = OperationVSContentProvider.getOperation(cursor);
                TextView operation_type = (TextView) view.findViewById(R.id.operation_type);
                TextView date_info = (TextView) view.findViewById(R.id.date_info);
                operation_type.setText(operation.getTypeVS() + " - " + operation.getState());
                date_info.setText("created: "+ DateUtils.getDayWeekDateStr(operation.getDateCreated())
                        + " - updated: " + DateUtils.getDayWeekDateStr(operation.getLastUpdated())) ;
            }
        }
    }

}