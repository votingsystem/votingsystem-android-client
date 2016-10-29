package org.votingsystem.fragment;

import android.app.Activity;
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

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.contentprovider.TransactionContentProvider;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.service.VoteService;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();

    public static final int RC_CANCEL_VOTE          = 0;

    private App app;
    private ReceiptWrapper receiptWrapper;
    private TransactionDto transactionDto;
    private TextView receiptSubject;
    private WebView receipt_content;
    private CMSSignedMessage receiptWrapperCMS;
    private String broadCastId;
    private String receiptURL;
    private Menu menu;


    public static Fragment newInstance(int cursorPosition) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(String receiptURL, OperationType type) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.TYPEVS_KEY, type);
        args.putString(Constants.URL_KEY, receiptURL);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
        if(responseDto.getOperationType() == OperationType.CANCEL_VOTE){
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        responseDto.getCaption(), responseDto.getNotificationMessage(), getActivity());
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
        MessageDialogFragment.showDialog(responseDto, getFragmentManager());
        }
    };

    private void launchVoteCancelation(VoteHelper voteHelper) {
        Intent startIntent = new Intent(getActivity(), VoteService.class);
        startIntent.putExtra(Constants.TYPEVS_KEY, OperationType.CANCEL_VOTE);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.VOTE_KEY, voteHelper);
        setProgressDialogVisible(getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg), true);
        getActivity().startService(startIntent);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(Constants.CURSOR_POSITION_KEY);
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
        OperationType type = (OperationType) getArguments().getSerializable(Constants.TYPEVS_KEY);
        receiptURL = getArguments().getString(Constants.URL_KEY);
        receiptWrapper = (ReceiptWrapper) getArguments().getSerializable(Constants.RECEIPT_KEY);
        transactionDto = (TransactionDto) getArguments().getSerializable(Constants.TRANSACTION_KEY);
        if(transactionDto != null) receiptWrapper = new ReceiptWrapper(transactionDto);
        if(receiptWrapper != null) {
            if(receiptWrapper.hashReceipt()) initReceiptScreen(receiptWrapper);
            else receiptURL = receiptWrapper.getURL();
        }
        if(savedInstanceState != null) {
            receiptWrapper = (ReceiptWrapper) savedInstanceState.getSerializable(
                    Constants.RECEIPT_KEY);
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
                Integer cursorPosition =  getArguments().getInt(Constants.CURSOR_POSITION_KEY);
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
        LOGD(TAG + ".initReceiptScreen", "type: " + receiptWrapper.getOperationType());
        try {
            String contentFormatted = "";
            String dateStr = null;
            receiptWrapperCMS = receiptWrapper.getReceipt();
            String receiptSubjectStr = null;
            switch(receiptWrapper.getOperationType()) {
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
                    VoteHelper voteHelper = (VoteHelper) receiptWrapper;
                    receiptSubjectStr = voteHelper.getEventVS().getSubject();
                    dateStr = DateUtils.getDayWeekDateStr(receiptWrapperCMS.getSigner().
                            getTimeStampToken().getTimeStampInfo().getGenTime(), "HH:mm");
                    VoteDto vsDto = receiptWrapper.getReceipt().getSignedContent(VoteDto.class);
                    contentFormatted = getString(R.string.vote_info_formatted,
                            dateStr, vsDto.getOptionSelected().getContent());
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
                    dateStr = DateUtils.getDayWeekDateStr(receiptWrapperCMS.getSigner().
                            getTimeStampToken().getTimeStampInfo().getGenTime(), "HH:mm");
                    contentFormatted = getString(R.string.access_request_info_formatted, dateStr,
                            requestDto.getEventURL());
                    receiptSubjectStr = getString(R.string.access_request_lbl);
                    break;
                default:
                    contentFormatted = receiptWrapper.getReceipt().getSignedContentStr();

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
        if(receiptWrapper == null || receiptWrapper.getOperationType() == null) {
            LOGD(TAG + ".receiptWrapper", "receiptWrapper undefined");
            return;
        }
        if(OperationType.SEND_VOTE != receiptWrapper.getOperationType()) {
            menu.removeItem(R.id.cancel_vote);
            menu.removeItem(R.id.check_receipt);
        }
        switch(receiptWrapper.getOperationType()) {
            case SEND_VOTE:
                if(((VoteHelper)receiptWrapper).getEventVS().getDateFinish().before(
                        new Date(System.currentTimeMillis()))) {
                    menu.removeItem(R.id.cancel_vote);
                }
                menu.setGroupVisible(R.id.vote_items, true);
                break;
            case CANCEL_VOTE:
                menu.removeItem(R.id.cancel_vote);
                break;
            default: LOGD(TAG + ".setActionBarMenu", "unprocessed type: " +
                    receiptWrapper.getOperationType());
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
        if(receiptWrapper != null) outState.putSerializable(Constants.RECEIPT_KEY,receiptWrapper);
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
                receiptWrapper.getOperationType());
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
                    UIUtils.showSignersInfoDialog(receiptWrapperCMS.getSigners(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(receiptWrapperCMS.getSigner().getTimeStampToken(),
                            app.getTimeStampCert(), getFragmentManager(), getActivity());
                    break;
                case R.id.share_receipt:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType(ContentType.TEXT.getName());
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile(
                            receiptWrapperCMS.toPEM(), getActivity()));
                    startActivity(sendIntent);
                    return true;
                case R.id.save_receipt:
                    ContentValues values = new ContentValues();
                    values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(receiptWrapper));
                    values.put(ReceiptContentProvider.TYPE_COL, receiptWrapper.getOperationType().toString());
                    values.put(ReceiptContentProvider.URL_COL, receiptWrapper.getURL());
                    values.put(ReceiptContentProvider.STATE_COL, ReceiptWrapper.State.ACTIVE.toString());
                    menu.removeItem(R.id.save_receipt);
                    break;
                case R.id.signature_content:
                    try {
                        MessageDialogFragment.showDialog(ResponseDto.SC_OK, getString(
                                        R.string.signature_content), receiptWrapperCMS.getSignedContentStr(),
                                getFragmentManager());
                    } catch(Exception ex) { ex.printStackTrace();}
                    break;
                case R.id.check_receipt:
                    if(receiptWrapper instanceof VoteHelper) {
                        new VoteChecker().execute(((VoteHelper)receiptWrapper).getRevocationHashBase64());
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
                                    ((VoteHelper) receiptWrapper).getEventVS().getSubject())))
                                    .setPositiveButton(getString(R.string.ok_lbl),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Utils.getProtectionPassword(RC_CANCEL_VOTE,
                                                            getString(R.string.cancel_vote_msg),
                                                            null, (AppCompatActivity)getActivity());
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

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_CANCEL_VOTE:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                    launchVoteCancelation((VoteHelper) receiptWrapper);
                }
                break;
        }
    }

    private class VoteChecker extends AsyncTask<String, Void, ResponseDto> {

        @Override protected void onPostExecute(ResponseDto responseDto) {
            super.onPostExecute(responseDto);
            setProgressDialogVisible(null, null, false);
            if(ResponseDto.SC_OK != responseDto.getStatusCode()) MessageDialogFragment.showDialog(
                    responseDto, getFragmentManager());
            else {
                try {
                    VoteDto voteDtoDto = (VoteDto) responseDto.getMessage(VoteDto.class);
                    MessageDialogFragment.showDialog(ResponseDto.SC_OK,
                            MsgUtils.getVoteStateMsg(voteDtoDto.getState(), getActivity()),
                            getString(R.string.votvs_value_msg, voteDtoDto.getOptionSelected().getContent()),
                            getFragmentManager());
                } catch (Exception ex) {ex.printStackTrace();}
            }
        }

        @Override protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialogVisible(getString(R.string.wait_msg),
                    getString(R.string.checking_vote_state_lbl), true);
        }

        @Override protected ResponseDto doInBackground(String... params) {
            ResponseDto responseDto = null;
            try {
                String hashHex = StringUtils.toHex(params[0]);
                responseDto = HttpConnection.getInstance().getData(app.getAccessControl().
                        getVoteCheckServiceURL(hashHex), ContentType.JSON);
            } catch(Exception ex) {
                responseDto = ResponseDto.EXCEPTION(ex, getActivity());
            } finally {return responseDto;}
        }
    }

    public class ReceiptFetcher extends AsyncTask<String, String, ResponseDto> {

        public ReceiptFetcher() { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(getString(R.string.fetching_receipt_lbl),
                    getString(R.string.wait_msg), true);
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            return HttpConnection.getInstance().getData(urls[0], ContentType.TEXT);
        }

        @Override  protected void onPostExecute(ResponseDto responseDto) {
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    receiptWrapper.setReceipt(CMSSignedMessage.FROM_PEM(responseDto.getMessageBytes()));
                    if(transactionDto != null) {
                        transactionDto.setCMSMessage(responseDto.getCMS());
                        TransactionContentProvider.updateTransaction(app, transactionDto);
                    }
                    initReceiptScreen(receiptWrapper);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.error_lbl),
                        responseDto.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(null, null, false);
        }
    }

}