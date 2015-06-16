package org.votingsystem.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.contentprovider.OperationVSContentProvider;
import org.votingsystem.fragment.MessageDialogFragment;
import org.votingsystem.fragment.PinDialogFragment;
import org.votingsystem.fragment.ProgressDialogFragment;
import org.votingsystem.service.OperationVSService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.OperationVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Wallet;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVSGridActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = OperationVSGridActivity.class.getSimpleName();

    private static final int loaderId = 0;

    private String broadCastId = OperationVSGridActivity.class.getSimpleName();
    private GridView gridView;
    private int currentItemPosition = -1;
    private OperationVS operationVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(responseVS.getTypeVS()) {
                    case CURRENCY:
                        try {
                            Wallet.getCurrencySet((String) responseVS.getData());
                            launchService();
                        } catch(Exception ex) { ex.printStackTrace(); }
                        break;
                }
            } else {
                switch(responseVS.getTypeVS()) {
                    default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                            responseVS.getCaption(), responseVS.getNotificationMessage(),
                            getSupportFragmentManager());
                }
                setProgressDialogVisible(false, null, null);
            }
        }
    };

    private void launchService() {
        if(operationVS == null) {
            LOGD(TAG + ".launchService", "launchService - OperationVS NULL");
            return;
        }
        Intent startIntent = new Intent(this, OperationVSService.class);
        startIntent.putExtra(ContextVS.OPERATIONVS_KEY, operationVS);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.check_operationvs));
        startService(startIntent);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_grid);
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
        if(savedInstanceState != null) {
            operationVS = (OperationVS) savedInstanceState.getSerializable(ContextVS.OPERATIONVS_KEY);
        }
        //gridView.setOnScrollListener(this);
        registerForContextMenu(gridView);
        getLoaderManager().initLoader(loaderId, null, this);
        getSupportActionBar().setTitle("OperationsGridActivity");
    }

    private void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "position: " + position + " - id: " + id);
        currentItemPosition = position;
        view.showContextMenu();
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
    public void onLoaderReset(android.content.Loader<Cursor> loader) {  }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.operationvs_grid_context_menu, menu);
    }

    @Override public boolean onContextItemSelected(MenuItem item) {
        LOGD(TAG + ".onContextItemSelected", "item: " + item.getTitle() + " - currentItemPosition: "
                + currentItemPosition);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(currentItemPosition));
        operationVS = OperationVSContentProvider.getOperation(cursor);
        switch (item.getItemId()) {
            case R.id.check:
                if(Wallet.getCurrencySet() == null) {
                    PinDialogFragment.showWalletScreen(getSupportFragmentManager(), broadCastId,
                            getString(R.string.enter_wallet_pin_msg), false,
                            TypeVS.CURRENCY);
                    return true;
                }
                launchService();
                return true;
            case R.id.details:
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        operationVS.getTypeVS().toString(),
                        MsgUtils.getOperationVSDescription(operationVS), this);
                UIUtils.showMessageDialog(builder);
                return true;
            case R.id.delete:
                getContentResolver().delete(OperationVSContentProvider.getURI(
                        operationVS.getLocalId()), null, null);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getSupportFragmentManager());
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.OPERATIONVS_KEY, operationVS);
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
                date_info.setText(DateUtils.getDayWeekDateStr(operation.getDateCreated())
                        + " - updated: " + DateUtils.getDayWeekDateStr(operation.getLastUpdated())) ;
            }
        }
    }

}