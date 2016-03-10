package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.TransactionVSContentProvider;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSFragment extends Fragment {

    public static final String TAG = TransactionVSFragment.class.getSimpleName();

    private TransactionVSDto selectedTransaction;
    private Menu menu;
    private TextView transactionvsSubject;
    private TextView transactionvs_content;
    private TextView to_user;
    private TextView from_user;
    private Button receipt;
    private CMSSignedMessage cmsMessage;
    private String broadCastId = null;
    private AppVS appVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(responseVS != null) {
            switch(responseVS.getTypeVS()) {
                case RECEIPT:
                    byte[] receiptBytes = (byte[]) responseVS.getData();
                    try {
                        selectedTransaction.setCMSMessage(new CMSSignedMessage(receiptBytes));
                    } catch (Exception e) { e.printStackTrace();  }
                    try {
                        selectedTransaction.setCMSMessage(new CMSSignedMessage(receiptBytes));
                    } catch (Exception e) { e.printStackTrace(); }
                    TransactionVSContentProvider.updateTransaction(appVS, selectedTransaction);
                    break;
            }
        }
        }
    };

    public static Fragment newInstance(int cursorPosition) {
        TransactionVSFragment fragment = new TransactionVSFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        appVS = (AppVS) getActivity().getApplicationContext();
        String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + "= ? ";
        Cursor cursor = getActivity().getContentResolver().query(
                TransactionVSContentProvider.CONTENT_URI, null, selection,
                new String[]{appVS.getCurrentWeekLapseId()}, null);
        cursor.moveToPosition(cursorPosition);
        Long transactionId = cursor.getLong(cursor.getColumnIndex(
                TransactionVSContentProvider.ID_COL));
        try {
            byte[] jsonBytes = cursor.getBlob(
                    cursor.getColumnIndex(TransactionVSContentProvider.JSON_COL));
            selectedTransaction = JSON.readValue(jsonBytes, TransactionVSDto.class);
            selectedTransaction.setLocalId(transactionId);
        } catch (Exception ex) {ex.printStackTrace();}
        /*String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                TransactionVSContentProvider.WEEK_LAPSE_COL));
        String currencyStr = cursor.getString(cursor.getColumnIndex(
                TransactionVSContentProvider.CURRENCY_COL));
        LOGD(TAG + ".onCreateView", "weekLapse: " + weekLapseStr + " - currency:" + currencyStr);*/
        broadCastId = TransactionVSFragment.class.getSimpleName() + "_" + cursorPosition;
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments() + " - transactionId: " + transactionId);
        View rootView = inflater.inflate(R.layout.transactionvs, container, false);
        to_user = (TextView)rootView.findViewById(R.id.to_user);
        from_user = (TextView)rootView.findViewById(R.id.from_user);
        receipt = (Button) rootView.findViewById(R.id.receipt_button);
        transactionvs_content = (TextView)rootView.findViewById(R.id.transactionvs_content);
        transactionvs_content.setMovementMethod(LinkMovementMethod.getInstance());
        transactionvsSubject = (TextView)rootView.findViewById(R.id.transactionvs_subject);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            selectedTransaction = (TransactionVSDto) savedInstanceState.getSerializable(
                    ContextVS.TRANSACTION_KEY);
        }
        if(selectedTransaction != null) initTransactionVSScreen(selectedTransaction);
    }

    private void initTransactionVSScreen (final TransactionVSDto transactionvs) {
        LOGD(TAG + ".initTransactionVSScreen", "transactionvsId: " + transactionvs.getId());
        if(transactionvs.getFromUserVS() != null) {
            from_user.setText(Html.fromHtml(getString(R.string.transactionvs_from_user_lbl,
                    transactionvs.getFromUserVS().getNIF(),
                    transactionvs.getFromUserVS().getName())));
            from_user.setVisibility(View.VISIBLE);
        }
        if(transactionvs.getToUserVS() != null) {
            to_user.setText(Html.fromHtml(getString(R.string.transactionvs_to_user_lbl,
                    transactionvs.getToUserVS().getNIF(),
                    transactionvs.getToUserVS().getName())));
            to_user.setVisibility(View.VISIBLE);
        }
        String transactionHtml = getString(R.string.transactionvs_formatted,
                DateUtils.getDayWeekDateStr(transactionvs.getDateCreated(), "HH:mm"),
                transactionvs.getAmount().toPlainString(), transactionvs.getCurrencyCode());
        try {
            cmsMessage = transactionvs.getCMSMessage();
        } catch (Exception e) { e.printStackTrace(); }
        transactionvsSubject.setText(selectedTransaction.getSubject());
        transactionvs_content.setText(Html.fromHtml(transactionHtml));
        receipt.setText(getString(R.string.transactionvs_receipt));
        from_user.setVisibility(View.GONE);
        transactionvsSubject.setVisibility(View.GONE);
        receipt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.TRANSACTION_KEY, transactionvs);
                intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                startActivity(intent);
            }
        });
    }

    private void setActionBar() {
        if(selectedTransaction == null) return;
        switch(selectedTransaction.getType()) {
            case CURRENCY_REQUEST:
                //menu.removeItem(R.id.cancel_vote);
                break;
            case CURRENCY_SEND:
                //menu.removeItem(R.id.cancel_vote);
                break;
            default: LOGD(TAG + ".onCreateOptionsMenu", "unprocessed type: " +
                    selectedTransaction.getType());
        }
        if(getActivity() instanceof FragmentActivity) {
            ((AppCompatActivity)getActivity()).setTitle(getString(R.string.movement_lbl));
            ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                    selectedTransaction.getDescription(getActivity(), selectedTransaction.getType()));
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedTransaction != null) outState.putSerializable(
                ContextVS.TRANSACTION_KEY, selectedTransaction);
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

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected transactionvs type:");
        menuInflater.inflate(R.menu.transactionvs_fragment, menu);
        this.menu = menu;
        setActionBar();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {
            case R.id.show_signers_info:
                break;
            /*case R.id.delete_transactionvs:
                String selection = TransactionVSContentProvider.ID_COL + " = ?";
                String[] selectionArgs = { selectedTransaction.getId().toString() };
                getActivity().getContentResolver().delete(TransactionVSContentProvider.CONTENT_URI,
                        selection, selectionArgs);
                return true;*/
        }
        return super.onOptionsItemSelected(item);
    }
}