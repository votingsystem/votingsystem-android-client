package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.MessageContentProvider;
import org.votingsystem.android.dto.SocketMessageDto;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageFragment extends Fragment {

    public static final String TAG = MessageFragment.class.getSimpleName();

    private static final int CONTENT_VIEW_ID = 1000000;

    private WeakReference<CurrencyFragment> currencyRef;
    private AppVS contextVS;
    private SocketMessageDto socketMessage;
    private TypeVS typeVS;
    private Currency currency;
    private MessageContentProvider.State messageState;
    private TextView message_content;
    private LinearLayout fragment_container;
    private Long messageId;
    private String broadCastId;
    private boolean isVisibleToUser = false;
    private Cursor cursor;

    public static Fragment newInstance(int cursorPosition) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        TypeVS typeVS = (TypeVS)intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case CURRENCY:
                    try {
                        Set<Currency> currencyList = Wallet.getCurrencySet((String) responseVS.getData(),
                                (AppVS) getActivity().getApplicationContext());
                        if(currencyList != null) updateWallet();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                    }
                    break;
            }
        }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppVS) getActivity().getApplicationContext();
        View rootView = inflater.inflate(R.layout.message_fragment, container, false);
        fragment_container = (LinearLayout)rootView.findViewById(R.id.fragment_container);
        message_content = (TextView)rootView.findViewById(R.id.message_content);
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        cursor = getActivity().getContentResolver().query(
                MessageContentProvider.CONTENT_URI, null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        Long createdMillis = cursor.getLong(cursor.getColumnIndex(
                MessageContentProvider.TIMESTAMP_CREATED_COL));
        String dateInfoStr = DateUtils.getDayWeekDateStr(new Date(createdMillis));
        ((TextView)rootView.findViewById(R.id.date)).setText(dateInfoStr);
        try {
            socketMessage = JSON.getMapper().readValue(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.JSON_COL)), SocketMessageDto.class);
            messageId = cursor.getLong(cursor.getColumnIndex(MessageContentProvider.ID_COL));
            typeVS =  TypeVS.valueOf(cursor.getString(cursor.getColumnIndex(
                    MessageContentProvider.TYPE_COL)));
            switch (typeVS) {
                case MESSAGEVS:
                     if(isVisibleToUser) ((ActionBarActivity)getActivity()).getSupportActionBar().
                             setTitle(getString(R.string.message_lbl) +
                            " - " + socketMessage.getFrom());
                    message_content.setText(socketMessage.getMessage());
                    break;
                case CURRENCY_WALLET_CHANGE:
                    loadCurrencyWalletChangeData();
                    break;
            }
            messageState =  MessageContentProvider.State.valueOf(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.STATE_COL)));
            broadCastId = MessageFragment.class.getSimpleName() + "_" + cursorPosition;
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    broadcastReceiver, new IntentFilter(broadCastId));
            LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                    " - arguments: " + getArguments() + " - isVisibleToUser: " + isVisibleToUser);
            if (isVisibleToUser) {
                if(messageState == MessageContentProvider.State.NOT_READED) {
                    getActivity().getContentResolver().update(MessageContentProvider.getMessageURI(
                            messageId), MessageContentProvider.getContentValues(socketMessage,
                            MessageContentProvider.State.READED), null, null);
                    PrefUtils.addNumMessagesNotReaded(contextVS, -1);
                }
            }
        } catch(Exception ex) { ex.printStackTrace(); }
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        LOGD(TAG + ".setUserVisibleHint", "setUserVisibleHint: " + isVisibleToUser +
                " - messageState: " + messageState);
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        try {
            if(isVisibleToUser && typeVS != null) {
                switch (typeVS) {//to avoid problems with the pager
                    case MESSAGEVS:
                        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(
                                R.string.message_lbl) + " - " + socketMessage.getFrom());
                        break;
                    case CURRENCY_WALLET_CHANGE:
                        loadCurrencyWalletChangeData();
                        break;
                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadCurrencyWalletChangeData() throws Exception {
        if(isVisibleToUser) {
            currency = socketMessage.getCurrencySet().iterator().next();
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(
                    R.string.wallet_change_lbl));
            String fragmentTag = CurrencyFragment.class.getSimpleName() + messageId;
            message_content.setVisibility(View.GONE);
            if(currencyRef == null || currencyRef.get() == null) {
                fragment_container.removeAllViews();
                LinearLayout tempView = new LinearLayout(getActivity());
                tempView.setId(CONTENT_VIEW_ID + messageId.intValue());
                fragment_container.addView(tempView);
                currencyRef = new WeakReference<CurrencyFragment>(new CurrencyFragment());
                Bundle args = new Bundle();
                args.putSerializable(ContextVS.CURRENCY_KEY, currency);
                currencyRef.get().setArguments(args);
                getFragmentManager().beginTransaction().add(tempView.getId(),
                        currencyRef.get(), fragmentTag).commit();
            }
        }
    }

    @Override public void onPause() {
        super.onPause();
        if(cursor != null && !cursor.isClosed()) cursor.close();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected message type:" + socketMessage.getOperation());
        menuInflater.inflate(R.menu.message_fragment, menu);
        switch (socketMessage.getOperation()) {
            case MESSAGEVS:
                menu.setGroupVisible(R.id.message_items, true);
                break;
            case CURRENCY_WALLET_CHANGE:
                menu.setGroupVisible(R.id.currency_change_items, true);
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                break;
            case R.id.delete_message:
                MessageContentProvider.deleteById(messageId, getActivity());
                getActivity().onBackPressed();
                return true;
            case R.id.save_to_wallet:
                updateWallet();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWallet() {
        LOGD(TAG + ".updateWallet", "updateWallet");
        if(Wallet.getCurrencySet() == null) {
            PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                    getString(R.string.enter_wallet_pin_msg), false, TypeVS.CURRENCY);
        } else {
            try {
                SocketMessageDto socketMessageDto = null;
                try {
                    Wallet.updateWallet(new HashSet(Arrays.asList(currency)), contextVS);
                    String msg = getString(R.string.save_to_wallet_ok_msg, currency.getAmount().toString() + " " +
                            currency.getCurrencyCode()) + " " + getString(R.string.for_lbl)  + " " +
                            MsgUtils.getTagVSMessage(currency.getSignedTagVS(), contextVS);
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            getString(R.string.save_to_wallet_lbl), msg, getActivity());
                    builder.setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().onBackPressed();
                                }
                            });
                    UIUtils.showMessageDialog(builder);
                    socketMessageDto = socketMessage.getResponse(ResponseVS.SC_OK, currency.getHashCertVS(),
                            TypeVS.CURRENCY_WALLET_CHANGE);
                } catch (ValidationExceptionVS ex) {
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.error_lbl), ex.getMessage(),
                            getFragmentManager());
                    socketMessageDto = socketMessage.getResponse(ResponseVS.SC_ERROR, ex.getMessage(),
                            TypeVS.CURRENCY_WALLET_CHANGE);
                }
                if(socketMessageDto != null) {
                    Intent startIntent = new Intent(getActivity(), WebSocketService.class);
                    startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.WEB_SOCKET_RESPONSE);
                    startIntent.putExtra(ContextVS.WEBSOCKET_MSG_KEY,
                            JSON.getMapper().writeValueAsString(socketMessageDto));
                    startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                    getActivity().startService(startIntent);
                }
                MessageContentProvider.deleteById(messageId, getActivity());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}