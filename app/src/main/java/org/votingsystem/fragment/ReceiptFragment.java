package org.votingsystem.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.contentprovider.TransactionVSContentProvider;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.service.VoteService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();

    private AppVS appVS;
    private ReceiptWrapper receiptWrapper;
    private TransactionVSDto transactionDto;
    private TextView receiptSubject;
    private WebView receipt_content;
    private SMIMEMessage receiptWrapperSMIME;
    private String broadCastId;
    private String receiptURL;
    private Menu menu;


    public static Fragment newInstance(int cursorPosition) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(String receiptURL, TypeVS type) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        args.putString(ContextVS.URL_KEY, receiptURL);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case CANCEL_VOTE:
                    launchVoteCancelation((VoteVSHelper) receiptWrapper);
                    break;
            }
        } else {
            if(responseVS.getTypeVS() == TypeVS.CANCEL_VOTE){
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        responseVS.getCaption(), responseVS.getNotificationMessage(), getActivity());
                    builder.setPositiveButton(getString(R.string.accept_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getActivity().onBackPressed();
                            }
                        });
                    UIUtils.showMessageDialog(builder);
                }
            }
            setProgressDialogVisible(null, null, false);
            MessageDialogFragment.showDialog(responseVS, getFragmentManager());
        }
        }
    };

    private void launchVoteCancelation(VoteVSHelper voteVSHelper) {
        Intent startIntent = new Intent(getActivity(), VoteService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CANCEL_VOTE);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.VOTE_KEY, voteVSHelper);
        setProgressDialogVisible(getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg), true);
        getActivity().startService(startIntent);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = ReceiptFragment.class.getSimpleName() + "_" + cursorPosition;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        receipt_content = (WebView)rootView.findViewById(R.id.receipt_content);
        receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TypeVS type = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        receiptURL = getArguments().getString(ContextVS.URL_KEY);
        receiptWrapper = (ReceiptWrapper) getArguments().getSerializable(ContextVS.RECEIPT_KEY);
        transactionDto = (TransactionVSDto) getArguments().getSerializable(ContextVS.TRANSACTION_KEY);
        if(transactionDto != null) receiptWrapper = new ReceiptWrapper(transactionDto);
        if(receiptWrapper != null) {
            if(receiptWrapper.hashReceipt()) initReceiptScreen(receiptWrapper);
            else receiptURL = receiptWrapper.getURL();
        }
        if(savedInstanceState != null) {
            receiptWrapper = (ReceiptWrapper) savedInstanceState.getSerializable(
                    ContextVS.RECEIPT_KEY);
            initReceiptScreen(receiptWrapper);
        } else {
            if(receiptURL != null) {
                receiptWrapper = new ReceiptWrapper(type, receiptURL);
                String selection = ReceiptContentProvider.URL_COL + "=? ";
                String[] selectionArgs = new String[]{receiptURL};
                Cursor cursor = getActivity().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
                if(cursor.getCount() > 0 ) {
                    cursor.moveToFirst();
                    byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                            ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                    Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                    try {
                        receiptWrapper = (ReceiptWrapper) ObjectUtils.
                                deSerializeObject(serializedReceiptContainer);
                        receiptWrapper.setLocalId(receiptId);
                        initReceiptScreen(receiptWrapper);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    receiptURL = null;
                }
            } else {
                Integer cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
                Cursor cursor = getActivity().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, null, null, null);
                cursor.moveToPosition(cursorPosition);
                byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                        ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                try {
                    receiptWrapper = (ReceiptWrapper) ObjectUtils.
                            deSerializeObject(serializedReceiptContainer);
                    receiptWrapper.setLocalId(receiptId);
                    initReceiptScreen(receiptWrapper);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initReceiptScreen (ReceiptWrapper receiptWrapper) {
        LOGD(TAG + ".initReceiptScreen", "type: " + receiptWrapper.getTypeVS() +
                " - messageId: " + receiptWrapper.getMessageId());
        try {
            String contentFormatted = "";
            String dateStr = null;
            receiptWrapperSMIME = receiptWrapper.getReceipt();
            String receiptSubjectStr = receiptWrapperSMIME == null? null : receiptWrapperSMIME.getSubject();
            switch(receiptWrapper.getTypeVS()) {
                case REPRESENTATIVE_SELECTION:
                case ANONYMOUS_SELECTION_CERT_REQUEST:
                    RepresentativeDelegationDto delegationDto = receiptWrapper.getReceipt()
                            .getSignedContent(org.votingsystem.dto.voting.RepresentativeDelegationDto.class);
                    contentFormatted = getString(R.string.anonymous_representative_request_formatted,
                            delegationDto.getWeeksOperationActive(),
                        DateUtils.getDateStr(delegationDto.getDateFrom()),
                        DateUtils.getDateStr(delegationDto.getDateTo()),
                        delegationDto.getServerURL());
                    break;
                case CURRENCY_REQUEST:
                    CurrencyDto currencyDto = receiptWrapper.getReceipt()
                            .getSignedContent(CurrencyDto.class);
                    contentFormatted = getString(R.string.currency_request_formatted,
                            currencyDto.getAmount(), currencyDto.getCurrencyCode(),
                            currencyDto.getCurrencyServerURL());
                    break;
                case SEND_VOTE:
                    VoteVSHelper voteVSHelper = (VoteVSHelper) receiptWrapper;
                    receiptSubjectStr = voteVSHelper.getEventVS().getSubject();
                    dateStr = DateUtils.getDayWeekDateStr(receiptWrapperSMIME.getSigner().
                            getTimeStampToken().getTimeStampInfo().getGenTime(), "HH:mm");
                    contentFormatted = getString(R.string.votevs_info_formatted,
                            voteVSHelper.getVote().getOptionSelected().getContent(),
                            receiptWrapper.getReceipt().getSignedContent());
                    break;
                case ANONYMOUS_REPRESENTATIVE_SELECTION:
                    RepresentativeDelegationDto delegation =  receiptWrapper.getReceipt()
                            .getSignedContent(RepresentativeDelegationDto.class);
                    contentFormatted = getString(R.string.anonymous_representative_selection_formatted,
                            delegation.getWeeksOperationActive(),
                            DateUtils.getDateStr(delegation.getDateFrom(), "EEE dd MMM yyyy' 'HH:mm"),
                            DateUtils.getDateStr(delegation.getDateTo(), "EEE dd MMM yyyy' 'HH:mm"));
                    break;
                case ACCESS_REQUEST:
                    AccessRequestDto requestDto =  receiptWrapper.getReceipt()
                            .getSignedContent(AccessRequestDto.class);
                    dateStr = DateUtils.getDayWeekDateStr(receiptWrapperSMIME.getSigner().
                            getTimeStampToken().getTimeStampInfo().getGenTime(), "HH:mm");
                    contentFormatted = getString(R.string.access_request_info_formatted, dateStr,
                            requestDto.getEventURL());
                    receiptSubjectStr = getString(R.string.access_request_lbl);
                    break;
                case FROM_GROUP_TO_ALL_MEMBERS:
                    TransactionVSDto transactionVSDto = receiptWrapperSMIME.getSignedContent(
                            TransactionVSDto.class);
                    contentFormatted = transactionVSDto.getFormatted(getActivity());
                    break;
                default:
                    contentFormatted = receiptWrapper.getReceipt().getSignedContent();

            }
            receiptSubject.setText(receiptSubjectStr);
            contentFormatted = "<html><body style='background-color:#eeeeee;margin:0 auto;font-size:1.2em;'>" +
                    contentFormatted + "</body></html>";
            receipt_content.loadData(contentFormatted, "text/html; charset=UTF-8", null);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setLogo(
                    UIUtils.getEmptyLogo(getActivity()));
            ((AppCompatActivity)getActivity()).setTitle(getString(R.string.receipt_lbl));
            setActionBarMenu(menu);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setActionBarMenu(Menu menu) {
        if(menu == null) {
            LOGD(TAG + ".setActionBarMenu", "menu null");
            return;
        }
        if(receiptWrapper == null || receiptWrapper.getTypeVS() == null) {
            LOGD(TAG + ".receiptWrapper", "receiptWrapper undefined");
            return;
        }
        if(TypeVS.SEND_VOTE != receiptWrapper.getTypeVS()) {
            menu.removeItem(R.id.cancel_vote);
            menu.removeItem(R.id.check_receipt);
        }
        switch(receiptWrapper.getTypeVS()) {
            case SEND_VOTE:
                if(((VoteVSHelper)receiptWrapper).getEventVS().getDateFinish().before(
                        new Date(System.currentTimeMillis()))) {
                    menu.removeItem(R.id.cancel_vote);
                }
                menu.setGroupVisible(R.id.vote_items, true);
                break;
            case CANCEL_VOTE:
                menu.removeItem(R.id.cancel_vote);
                break;
            default: LOGD(TAG + ".setActionBarMenu", "unprocessed type: " +
                    receiptWrapper.getTypeVS());
        }
        if(receiptWrapper.getLocalId() < 0) menu.removeItem(R.id.delete_item);
        else menu.removeItem(R.id.save_receipt);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                receiptWrapper.getTypeDescription(getActivity()));
    }

    @Override public void onStart() {
        LOGD(TAG + ".onStart", "onStart");
        super.onStart();
        if(receiptURL != null) new ReceiptFetcher().execute(receiptURL);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(receiptWrapper != null) outState.putSerializable(ContextVS.RECEIPT_KEY,receiptWrapper);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected receipt type:" +
                receiptWrapper.getTypeVS());
        this.menu = menu;
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        if(receiptWrapper != null) setActionBarMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    break;
                case R.id.show_signers_info:
                    UIUtils.showSignersInfoDialog(receiptWrapperSMIME.getSigners(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(receiptWrapperSMIME.getSigner().getTimeStampToken(),
                            appVS.getTimeStampCert(), getFragmentManager(), getActivity());
                    break;
                case R.id.share_receipt:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType(ContentTypeVS.TEXT.getName());
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile(
                            receiptWrapperSMIME.getBytes(), getActivity()));
                    startActivity(sendIntent);
                    return true;
                case R.id.save_receipt:
                    ContentValues values = new ContentValues();
                    values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(receiptWrapper));
                    values.put(ReceiptContentProvider.TYPE_COL, receiptWrapper.getTypeVS().toString());
                    values.put(ReceiptContentProvider.URL_COL, receiptWrapper.getMessageId());
                    values.put(ReceiptContentProvider.STATE_COL, ReceiptWrapper.State.ACTIVE.toString());
                    menu.removeItem(R.id.save_receipt);
                    break;
                case R.id.signature_content:
                    try {
                        MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(
                                        R.string.signature_content), receiptWrapperSMIME.getSignedContent(),
                                getFragmentManager());
                    } catch(Exception ex) { ex.printStackTrace();}
                    break;
                case R.id.check_receipt:
                    if(receiptWrapper instanceof VoteVSHelper) {
                        new VoteVSChecker().execute(((VoteVSHelper)receiptWrapper).getHashCertVSBase64());
                    }
                    return true;
                case R.id.delete_item:
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(
                            getString(R.string.delete_receipt_lbl)).setMessage(Html.fromHtml(
                            getString(R.string.delete_receipt_msg, receiptWrapper.getSubject()))).
                            setPositiveButton(getString(R.string.ok_lbl), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().getContentResolver().delete(ReceiptContentProvider.
                                            getReceiptURI(receiptWrapper.getLocalId()), null, null);
                                    getActivity().onBackPressed();
                                }
                            }).setNegativeButton(getString(R.string.cancel_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    return true;
                case R.id.cancel_vote:
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.cancel_vote_lbl)).
                            setMessage(Html.fromHtml(getString(R.string.cancel_vote_from_receipt_msg,
                                    ((VoteVSHelper) receiptWrapper).getEventVS().getSubject())))
                                    .setPositiveButton(getString(R.string.ok_lbl),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                                            getString(R.string.cancel_vote_msg), false, TypeVS.CANCEL_VOTE);
                                                }
                                            }).setNegativeButton(getString(R.string.cancel_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    return true;
            }
        } catch(Exception ex) { ex.printStackTrace();}
        return super.onOptionsItemSelected(item);
    }

    private class VoteVSChecker extends AsyncTask<String, Void, ResponseVS> {

        @Override protected void onPostExecute(ResponseVS responseVS) {
            super.onPostExecute(responseVS);
            setProgressDialogVisible(null, null, false);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) MessageDialogFragment.showDialog(
                    responseVS, getFragmentManager());
            else {
                try {
                    VoteVSDto voteVSDtoDto = (VoteVSDto) responseVS.getMessage(org.votingsystem.dto.voting.VoteVSDto.class);
                    MessageDialogFragment.showDialog(ResponseVS.SC_OK,
                            MsgUtils.getVoteVSStateMsg(voteVSDtoDto.getState(), getActivity()),
                            getString(R.string.votvs_value_msg, voteVSDtoDto.getOptionSelected().getContent()),
                            getFragmentManager());
                } catch (Exception ex) {ex.printStackTrace();}
            }
        }

        @Override protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialogVisible(getString(R.string.wait_msg),
                    getString(R.string.checking_vote_state_lbl), true);
        }

        @Override protected ResponseVS doInBackground(String... params) {
            ResponseVS responseVS = null;
            try {
                String hashHex = StringUtils.toHex(params[0]);
                responseVS = HttpHelper.getData(appVS.getAccessControl().
                        getVoteVSCheckServiceURL(hashHex), ContentTypeVS.JSON);
            } catch(Exception ex) {
                responseVS = ResponseVS.EXCEPTION(ex, getActivity());
            } finally {return responseVS;}
        }
    }

    public class ReceiptFetcher extends AsyncTask<String, String, ResponseVS> {

        public ReceiptFetcher() { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(getString(R.string.fetching_receipt_lbl),
                    getString(R.string.wait_msg), true);
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            return HttpHelper.getData(urls[0], ContentTypeVS.TEXT);
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    receiptWrapper.setReceiptBytes(responseVS.getMessageBytes());
                    if(transactionDto != null) {
                        transactionDto.setSmimeMessage(responseVS.getSMIME());
                        TransactionVSContentProvider.updateTransaction(appVS, transactionDto);
                    }
                    initReceiptScreen(receiptWrapper);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(null, null, false);
        }
    }

}