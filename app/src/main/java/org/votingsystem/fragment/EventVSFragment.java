package org.votingsystem.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.votingsystem.App;
import org.votingsystem.activity.BrowserActivity;
import org.votingsystem.activity.EventVSPagerActivity;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.activity.ID_CardNFCReaderActivity;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.FieldEventDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.service.VoteService;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.votingsystem.util.Constants.MAX_SUBJECT_SIZE;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = EventVSFragment.class.getSimpleName();

    public static final int RC_ACCESS_REQUEST      = 0;
    public static final int RC_SIGN_ACCESS_REQUEST = 1;
    public static final int RC_SEND_VOTE           = 2;

    private EventVSDto eventVS;
    private VoteHelper voteHelper;
    private FieldEventDto optionSelected;
    private List<Button> voteOptionsButtonList;
    private Button saveReceiptButton;
    private App app;
    private View rootView;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
            final ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSEVS_KEY);
            voteHelper = (VoteHelper) intent.getSerializableExtra(Constants.VOTE_KEY);
            processVoteResult(voteHelper, responseDto);
        }
    };

    private void processVoteResult(VoteHelper voteHelper, ResponseDto responseDto) {
        if(responseDto.getOperationType() == OperationType.SEND_VOTE) {
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                showReceiptScreen(voteHelper);
                MessageDialogFragment.showDialog(null, getString(R.string.vote_ok_caption),
                        getString(R.string.result_ok_description), getFragmentManager());
            } else if(ResponseDto.SC_ERROR_REQUEST_REPEATED == responseDto.getStatusCode()){
                MessageDto messageDto = null;
                try {
                    messageDto = (MessageDto) responseDto.getMessage(MessageDto.class);
                } catch (Exception e) { e.printStackTrace(); }
                final String accessRequestReceiptURL = messageDto.getURL();
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        responseDto.getCaption(), responseDto.getNotificationMessage(),getActivity());
                builder.setPositiveButton(getString(R.string.open_receipt_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                intent.putExtra(Constants.URL_KEY, accessRequestReceiptURL);
                                intent.putExtra(Constants.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                startActivity(intent);
                            }
                        });
                UIUtils.showMessageDialog(builder);
            } else {
                setOptionButtonsEnabled(true);
                MessageDialogFragment.showDialog(responseDto, getFragmentManager());
            }
        }
        setProgressDialogVisible(false, null);
    }

    private void launchVoteService(OperationType operation, CMSSignedMessage cmsMessage) {
        LOGD(TAG + ".launchVoteService", "operation: " + operation.toString());
        try {
            Intent startIntent = new Intent(getActivity(), VoteService.class);
            startIntent.putExtra(Constants.TYPEVS_KEY, operation);
            startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
            if(cmsMessage != null) {
                try {
                    startIntent.putExtra(Constants.CMS_MSG_KEY, cmsMessage.getEncoded());
                } catch (Exception e) { e.printStackTrace(); }
            }
            voteHelper = VoteHelper.load(new VoteDto(eventVS, optionSelected));
            String caption = getString(R.string.sending_vote_lbl);
            startIntent.putExtra(Constants.VOTE_KEY, voteHelper);
            setProgressDialogVisible(true, caption);
            setOptionButtonsEnabled(false);
            getActivity().startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static EventVSFragment newInstance(String eventJSONStr) {
        EventVSFragment fragment = new EventVSFragment();
        Bundle args = new Bundle();
        args.putString(Constants.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        ResponseDto responseDto = null;
        try {
            if(getArguments().getString(Constants.EVENTVS_KEY) != null) {
                String dtoStr = getArguments().getString(Constants.EVENTVS_KEY);
                eventVS = JSON.readValue(dtoStr, EventVSDto.class);
            } else if (getArguments().getSerializable(Constants.VOTE_KEY) != null) {
                voteHelper = (VoteHelper) getArguments().getSerializable(Constants.VOTE_KEY);
                eventVS = voteHelper.getEventVS();
                responseDto = getArguments().getParcelable(Constants.RESPONSEVS_KEY);
            }
            eventVS.setAccessControl(app.getAccessControl());
        } catch(Exception ex) {  ex.printStackTrace(); }
        rootView = inflater.inflate(R.layout.eventvs_fragment, container, false);
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        saveReceiptButton.setOnClickListener(this);
        setHasOptionsMenu(true);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        broadCastId = EventVSFragment.class.getSimpleName() + "_" + eventVS.getId();
        String subtTitle = null;
        switch(eventVS.getState()) {
            case ACTIVE:
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_open_lbl,
                        DateUtils.getElapsedTimeStr(eventVS.getDateFinish())));
                break;
            case PENDING:
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                        getString(R.string.voting_pending_lbl,
                        DateUtils.getElapsedTimeStr(eventVS.getDateBegin())));
                subtTitle = getString(R.string.init_lbl) + ": " +
                        DateUtils.getDayWeekDateStr(eventVS.getDateBegin(), "HH:mm");
                break;
            default:
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.voting_closed_lbl));
        }
        if(subtTitle != null) ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(subtTitle);
        if(voteHelper != null) processVoteResult(voteHelper, responseDto);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if(bundle != null) {
            voteHelper = (VoteHelper) bundle.getSerializable(Constants.VOTE_KEY);
            optionSelected = (FieldEventDto) bundle.getSerializable(Constants.DTO_KEY);
        }
        if(voteHelper != null && voteHelper.getVoteReceipt() != null) showReceiptScreen(voteHelper);
        else setEventScreen(eventVS);
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_receipt_button:
                voteHelper.setOperationType(OperationType.SEND_VOTE);
                Uri uri = getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI,
                        ReceiptContentProvider.getContentValues(voteHelper, ReceiptWrapper.State.ACTIVE));
                LOGD(TAG + ".saveVote", "uri: " + uri.toString());
                saveReceiptButton.setEnabled(false);
                break;
            case R.id.event_subject:
                if(eventVS != null && eventVS.getSubject() != null &&
                        eventVS.getSubject().length() > MAX_SUBJECT_SIZE) {
                    MessageDialogFragment.showDialog(null, getString(R.string.subject_lbl),
                            eventVS.getSubject(), getFragmentManager());
                }
                break;
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption,
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.eventvs, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(App.getInstance(), BrowserActivity.class);
                intent.putExtra(Constants.URL_KEY, eventVS.getStatsServiceURL());
                intent.putExtra(Constants.DOUBLE_BACK_KEY, false);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        try {
            switch (requestCode) {
                case RC_ACCESS_REQUEST:
                    if(Activity.RESULT_OK == resultCode) {
                        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                            Intent intent = new Intent(getActivity(), ID_CardNFCReaderActivity.class);
                            intent.putExtra(Constants.PASSWORD_KEY, new
                                    String(responseDto.getMessageBytes()).toCharArray());
                            intent.putExtra(Constants.USER_KEY,
                                    App.getInstance().getAccessControl().getName());
                            intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.vote_selected_msg,
                                    optionSelected.getContent()));
                            voteHelper = VoteHelper.load(new VoteDto(eventVS, optionSelected));
                            AccessRequestDto requestDto = voteHelper.getAccessRequest();
                            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, JSON.writeValueAsString(requestDto));
                            intent.putExtra(Constants.MESSAGE_SUBJECT_KEY, getString(R.string.request_msg_subject,
                                    voteHelper.getEventVS().getId()));
                            startActivityForResult(intent, RC_SIGN_ACCESS_REQUEST);
                        }
                    } else {
                        setOptionButtonsEnabled(true);
                        setProgressDialogVisible(false, null);
                    }
                    break;
                case RC_SIGN_ACCESS_REQUEST:
                    if(Activity.RESULT_OK == resultCode) {
                        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSEVS_KEY);
                        launchVoteService(OperationType.SEND_VOTE, responseDto.getCMS());
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showReceiptScreen(final VoteHelper voteHelper) {
        LOGD(TAG, "showReceiptScreen");
        rootView.findViewById(R.id.receipt_buttons).setVisibility(View.VISIBLE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = voteHelper.getEventVS().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        try {
            String eventContent = new String(Base64.decode(
                    voteHelper.getEventVS().getContent().getBytes(), Base64.NO_WRAP), "UTF-8");
            contentTextView.setText(Html.fromHtml(eventContent));
        } catch (Exception ex) { ex.printStackTrace(); }
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventDto> fieldsEventVS = voteHelper.getEventVS().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList == null) {
            voteOptionsButtonList = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 25, 15);
            for (final FieldEventDto option:fieldsEventVS) {
                Button optionButton = new Button(getActivity());
                optionButton.setText(option.getContent());
                voteOptionsButtonList.add(optionButton);
                optionButton.setEnabled(false);
                linearLayout.addView(optionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }

    private void setEventScreen(final EventVSDto event) {
        LOGD(TAG + ".setEventScreen", "setEventScreen");
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        saveReceiptButton.setEnabled(true);
        String subject = event.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        String eventContent = null;
        try {
            eventContent = new String(Base64.decode(event.getContent().getBytes(), Base64.NO_WRAP), "UTF-8");
        } catch (Exception ex) { ex.printStackTrace(); }
        contentTextView.setText(Html.fromHtml(eventContent));
        //contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventDto> fieldsEventVS = event.getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList != null) linearLayout.removeAllViews();
        voteOptionsButtonList = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final FieldEventDto option:fieldsEventVS) {
            Button optionButton = new Button(getActivity());
            optionButton.setText(option.getContent());
            optionButton.setTextAppearance(getActivity(), R.style.electionOptionText);
            optionButton.setOnClickListener(new Button.OnClickListener() {
                FieldEventDto optionSelected = option;
                public void onClick(View v) {
                    LOGD(TAG, "- optionButton - optionId: " + optionSelected.getId());
                    processSelectedOption(optionSelected);
                }
            });
            voteOptionsButtonList.add(optionButton);
            if (!event.isActive()) optionButton.setEnabled(false);
            linearLayout.addView(optionButton, paramsButton);
        }
    }

    private void processSelectedOption(FieldEventDto optionSelected) {
        LOGD(TAG + ".processSelectedOption", "processSelectedOption");
        try {
            this.optionSelected = optionSelected;
            String passwMsg= optionSelected.getContent().length() >
                    Constants.SELECTED_OPTION_MAX_LENGTH ? optionSelected.getContent().substring(0,
                    Constants.SELECTED_OPTION_MAX_LENGTH) + "..." : optionSelected.getContent();
            Utils.getProtectionPassword(RC_ACCESS_REQUEST, passwMsg,
                    null, (AppCompatActivity)getActivity());
        } catch (Exception ex) { ex.printStackTrace(); }
    }


    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        if(getActivity() instanceof EventVSPagerActivity) {
            ActivityResult activityResult = ((EventVSPagerActivity)getActivity()).getActivityResult();
            if(activityResult != null) {
                onActivityResult(activityResult.getRequestCode(),
                        activityResult.getResultCode(), activityResult.getData());
            }
        }
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if(voteOptionsButtonList == null) return;
        for(Button button: voteOptionsButtonList) {
            button.setEnabled(areEnabled);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.VOTE_KEY, voteHelper);
        outState.putSerializable(Constants.DTO_KEY, optionSelected);
    }

}