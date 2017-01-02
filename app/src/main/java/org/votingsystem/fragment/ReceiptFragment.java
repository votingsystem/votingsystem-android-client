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

import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.crypto.ReceiptContainer;
import org.votingsystem.crypto.VoteContainer;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.identity.IdentityRequestDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.MsgUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.xml.XmlReader;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();

    public static final int RC_CANCEL_VOTE = 0;


    private ReceiptContainer receiptContainer;
    private TextView receiptSubject;
    private WebView receipt_content;
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

    public static Fragment newInstance(String receiptURL, org.votingsystem.util.OperationType type) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.OPERATION_TYPE, type);
        args.putString(Constants.URL_KEY, receiptURL);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            if (responseDto.getOperationType() == org.votingsystem.util.OperationType.CANCEL_VOTE) {
                if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
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

    private void launchVoteCancelation(VoteContainer voteWrapper) {
        LOGD(TAG + ".launchVoteCancelation", "TODO");
        /*Intent startIntent = new Intent(getActivity(), VoteService.class);
        startIntent.putExtra(Constants.OPERATION_TYPE, org.votingsystem.util.OperationType.CANCEL_VOTE);
        startIntent.putExtra(Constants.CALLER_KEY, BROADCAST_ID);
        startIntent.putExtra(Constants.VOTE_KEY, voteWrapper);
        setProgressDialogVisible(getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg), true);
        getActivity().startService(startIntent);*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cursorPosition = getArguments().getInt(Constants.CURSOR_POSITION_KEY);
        broadCastId = ReceiptFragment.class.getSimpleName() + "_" + cursorPosition;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        receipt_content = (WebView) rootView.findViewById(R.id.receipt_content);
        receipt_content.setVisibility(View.GONE);
        receiptSubject = (TextView) rootView.findViewById(R.id.receipt_subject);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        OperationType type = (OperationType) getArguments().getSerializable(Constants.OPERATION_TYPE);
        receiptURL = getArguments().getString(Constants.URL_KEY);
        receiptContainer = (ReceiptContainer) getArguments().getSerializable(Constants.RECEIPT_KEY);
        if (receiptContainer != null) {
            if (receiptContainer.getReceipt() != null)
                initReceiptScreen(receiptContainer);
            else receiptURL = receiptContainer.getURL();
        }
        if (savedInstanceState != null) {
            receiptContainer = (ReceiptContainer) savedInstanceState.getSerializable(
                    Constants.RECEIPT_KEY);
            initReceiptScreen(receiptContainer);
        } else {
            if (receiptURL != null) {
                receiptContainer = new ReceiptContainer(type, receiptURL);
                String selection = ReceiptContentProvider.URL_COL + "=? ";
                String[] selectionArgs = new String[]{receiptURL};
                Cursor cursor = getActivity().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                            ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                    Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                    try {
                        receiptContainer = (ReceiptContainer) ObjectUtils.
                                deSerializeObject(serializedReceiptContainer);
                        receiptContainer.setLocalId(receiptId);
                        initReceiptScreen(receiptContainer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    receiptURL = null;
                }
            } else {
                Integer cursorPosition = getArguments().getInt(Constants.CURSOR_POSITION_KEY);
                Cursor cursor = getActivity().getContentResolver().query(
                        ReceiptContentProvider.CONTENT_URI, null, null, null, null);
                cursor.moveToPosition(cursorPosition);
                byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                        ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                try {
                    receiptContainer = (ReceiptContainer) ObjectUtils.
                            deSerializeObject(serializedReceiptContainer);
                    receiptContainer.setLocalId(receiptId);
                    initReceiptScreen(receiptContainer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initReceiptScreen(ReceiptContainer receiptContainer) {
        LOGD(TAG + ".initReceiptScreen", "type: " + receiptContainer.getOperationType());
        try {
            String contentFormatted = "";
            String dateStr = null;
            String receiptSubjectStr = null;
            switch (receiptContainer.getOperationType()) {
                case SEND_VOTE:
                    VoteContainer voteContainer = (VoteContainer) receiptContainer;
                    receiptSubjectStr = voteContainer.getElection().getSubject();
                    dateStr = DateUtils.getDayWeekDateStr(voteContainer.getVoteDate(), "HH:mm");
                    VoteDto vsDto = voteContainer.getVote();
                    contentFormatted = getString(R.string.vote_info_formatted,
                            dateStr, vsDto.getOptionSelected().getContent());
                    break;
                case ANON_VOTE_CERT_REQUEST:
                    IdentityRequestDto idRequest = XmlReader.readIdentityRequest(receiptContainer.getReceipt());
                    dateStr = DateUtils.getDayWeekDateStr(receiptContainer.getSigner().
                            getTimeStampToken().getTimeStampInfo().getGenTime(), "HH:mm");
                    String eventURL = OperationType.getElectionURL(
                            idRequest.getIndentityServiceEntity(), idRequest.getUUID());
                    contentFormatted = getString(R.string.access_request_info_formatted, dateStr,
                            eventURL);
                    receiptSubjectStr = getString(R.string.access_request_lbl);
                    break;
                default:
                    contentFormatted = new String(receiptContainer.getReceipt());

            }
            receiptSubject.setText(receiptSubjectStr);
            contentFormatted = "<html><body style='background-color:#eeeeee;margin:0 auto;font-size:1.2em;'>" +
                    contentFormatted + "</body></html>";
            receipt_content.loadData(contentFormatted, "text/html; charset=UTF-8", null);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setLogo(
                    UIUtils.getEmptyLogo(getActivity()));
            ((AppCompatActivity) getActivity()).setTitle(getString(R.string.receipt_lbl));
            setActionBarMenu(menu);
            receipt_content.setVisibility(View.VISIBLE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setActionBarMenu(Menu menu) {
        if (menu == null) {
            LOGD(TAG + ".setActionBarMenu", "menu null");
            return;
        }
        if (receiptContainer == null || receiptContainer.getOperationType() == null) {
            LOGD(TAG + ".receiptContainer", "receiptContainer undefined");
            return;
        }
        menu.removeItem(R.id.show_timestamp_info);
        menu.removeItem(R.id.signature_content);
        if (org.votingsystem.util.OperationType.SEND_VOTE != receiptContainer.getOperationType()) {
            //menu.removeItem(R.id.cancel_vote);
            menu.removeItem(R.id.check_receipt);
        }
        switch (receiptContainer.getOperationType()) {
            case SEND_VOTE:
                /*if (((VoteContainer) receiptContainer).getElection().getDateFinish().before(
                        new Date(System.currentTimeMillis()))) {
                    menu.removeItem(R.id.cancel_vote);
                }*/
                menu.setGroupVisible(R.id.vote_items, true);
                break;
            case CANCEL_VOTE:
                //menu.removeItem(R.id.cancel_vote);
                break;
            default:
                LOGD(TAG + ".setActionBarMenu", "unprocessed type: " +
                        receiptContainer.getOperationType());
        }
        if (receiptContainer.getLocalId() < 0) menu.removeItem(R.id.delete_item);
        else menu.removeItem(R.id.save_receipt);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(
                receiptContainer.getTypeDescription(getActivity()));
    }

    @Override
    public void onStart() {
        LOGD(TAG + ".onStart", "onStart");
        super.onStart();
        if (receiptURL != null) new ReceiptFetcher().execute(receiptURL);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (receiptContainer != null) outState.putSerializable(Constants.RECEIPT_KEY, receiptContainer);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected receipt type:" +
                receiptContainer.getOperationType());
        this.menu = menu;
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        if (receiptContainer != null) setActionBarMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    break;
                case R.id.show_signers_info:
                    UIUtils.showSignersInfoDialog(receiptContainer.getSignatures(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.show_timestamp_info:
                    //UIUtils.showTimeStampInfoDialog();
                    break;
                case R.id.share_receipt:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType(ContentType.TEXT.getName());
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile(
                            getString(R.string.receipt_lbl), receiptContainer.getReceipt(), getActivity()));
                    startActivity(sendIntent);
                    return true;
                case R.id.save_receipt:
                    ContentValues values = new ContentValues();
                    values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(receiptContainer));
                    values.put(ReceiptContentProvider.TYPE_COL, receiptContainer.getOperationType().toString());
                    values.put(ReceiptContentProvider.URL_COL, receiptContainer.getURL());
                    values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
                    menu.removeItem(R.id.save_receipt);
                    break;
                case R.id.signature_content:
                    try {
                        //MessageDialogFragment.showDialog(ResponseDto.SC_OK, getString(
                        //R.string.signature_content),receiptWrapperCMS.getSignedContentStr(), getFragmentManager());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case R.id.check_receipt:
                    if (receiptContainer instanceof VoteContainer) {
                        new VoteChecker(receiptContainer).execute();
                    }
                    return true;
                case R.id.delete_item:
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(
                            getString(R.string.delete_receipt_lbl)).setMessage(Html.fromHtml(
                            getString(R.string.delete_receipt_msg, receiptContainer.getSubject()))).
                            setPositiveButton(getString(R.string.ok_lbl), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().getContentResolver().delete(ReceiptContentProvider.
                                            getReceiptURI(receiptContainer.getLocalId()), null, null);
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
                /*case R.id.cancel_vote:
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.cancel_vote_lbl)).
                            setMessage(Html.fromHtml(getString(R.string.cancel_vote_from_receipt_msg,
                                    ((VoteContainer) receiptContainer).getElection().getSubject())))
                            .setPositiveButton(getString(R.string.ok_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Utils.getProtectionPassword(RC_CANCEL_VOTE,
                                                    getString(R.string.cancel_vote_msg),
                                                    null, (AppCompatActivity) getActivity());
                                        }
                                    }).setNegativeButton(getString(R.string.cancel_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    return true;*/
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_CANCEL_VOTE:
                if (Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    launchVoteCancelation((VoteContainer) receiptContainer);
                }
                break;
        }
    }

    private class VoteChecker extends AsyncTask<String, Void, ResponseDto> {

        private String systemEntityId, hashCert;

        public VoteChecker(ReceiptContainer receiptContainer) {
            this.systemEntityId = ((VoteContainer)receiptContainer).getVote().getVotingServiceEntity();
            this.hashCert = ((VoteContainer)receiptContainer).getRevocationHashBase64();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialogVisible(getString(R.string.wait_msg),
                    getString(R.string.checking_vote_state_lbl), true);
        }

        @Override
        protected ResponseDto doInBackground(String... params) {
            ResponseDto responseDto = null;
            try {
                responseDto = HttpConn.getInstance().doPostRequest(hashCert.getBytes(),
                        ContentType.XML, OperationType.VOTE_REPOSITORY.getUrl(systemEntityId));
            } catch (Exception ex) {
                responseDto = ResponseDto.EXCEPTION(ex, getActivity());
            } finally {
                return responseDto;
            }
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            super.onPostExecute(response);
            setProgressDialogVisible(null, null, false);
            if (ResponseDto.SC_OK != response.getStatusCode())
                MessageDialogFragment.showDialog(response, getFragmentManager());
            else {
                try {
                    VoteDto voteDto = XmlReader.readVote(response.getMessageBytes());
                    MessageDialogFragment.showDialog(ResponseDto.SC_OK,
                            MsgUtils.getVoteStateMsg(VoteDto.State.OK, getActivity()),
                            getString(R.string.votvs_value_msg, voteDto.getOptionSelected().getContent()),
                            getFragmentManager());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                }
            }
        }
    }

    public class ReceiptFetcher extends AsyncTask<String, String, ResponseDto> {

        public ReceiptFetcher() {
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(getString(R.string.fetching_receipt_lbl),
                    getString(R.string.wait_msg), true);
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            return HttpConn.getInstance().doGetRequest(urls[0], ContentType.TEXT);
        }

        @Override
        protected void onPostExecute(ResponseDto responseDto) {
            if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    receiptContainer.setReceipt(responseDto.getMessageBytes());
                    initReceiptScreen(receiptContainer);
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