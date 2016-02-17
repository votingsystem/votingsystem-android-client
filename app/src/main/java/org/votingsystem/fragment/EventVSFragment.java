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

import org.votingsystem.AppVS;
import org.votingsystem.activity.BrowserVSActivity;
import org.votingsystem.activity.DNIeSigningActivity;
import org.votingsystem.activity.EventVSPagerActivity;
import org.votingsystem.activity.FragmentContainerActivity;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.FieldEventVSDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.service.VoteService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ActivityResult;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.UIUtils;
import org.votingsystem.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.votingsystem.util.ContextVS.MAX_SUBJECT_SIZE;
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
    private VoteVSHelper voteVSHelper;
    private FieldEventVSDto optionSelected;
    private List<Button> voteOptionsButtonList;
    private Button saveReceiptButton;
    private AppVS appVS;
    private View rootView;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
            final ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            voteVSHelper = (VoteVSHelper) intent.getSerializableExtra(ContextVS.VOTE_KEY);
            processVoteResult(voteVSHelper, responseVS);
        }
    };

    private void processVoteResult(VoteVSHelper voteVSHelper, ResponseVS responseVS) {
        if(responseVS.getTypeVS() == TypeVS.SEND_VOTE) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                showReceiptScreen(voteVSHelper);
                MessageDialogFragment.showDialog(null, getString(R.string.vote_ok_caption),
                        getString(R.string.result_ok_description), getFragmentManager());
            } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()){
                MessageDto messageDto = null;
                try {
                    messageDto = (MessageDto) responseVS.getMessage(MessageDto.class);
                } catch (Exception e) { e.printStackTrace(); }
                final String accessRequestReceiptURL = messageDto.getURL();
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        responseVS.getCaption(), responseVS.getNotificationMessage(),getActivity());
                builder.setPositiveButton(getString(R.string.open_receipt_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                intent.putExtra(ContextVS.URL_KEY, accessRequestReceiptURL);
                                intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                                startActivity(intent);
                            }
                        });
                UIUtils.showMessageDialog(builder);
            } else {
                setOptionButtonsEnabled(true);
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
            }
        }
        setProgressDialogVisible(false, null);
    }

    private void launchVoteService(TypeVS operation, SMIMEMessage smimeMessage) {
        LOGD(TAG + ".launchVoteService", "operation: " + operation.toString());
        try {
            Intent startIntent = new Intent(getActivity(), VoteService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, operation);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            if(smimeMessage != null) {
                try {
                    startIntent.putExtra(ContextVS.SMIME_MSG_KEY, smimeMessage.getBytes());
                } catch (Exception e) { e.printStackTrace(); }
            }
            voteVSHelper = VoteVSHelper.load(new VoteVSDto(eventVS, optionSelected));
            String caption = getString(R.string.sending_vote_lbl);
            startIntent.putExtra(ContextVS.VOTE_KEY, voteVSHelper);
            setProgressDialogVisible(true, caption);
            setOptionButtonsEnabled(false);
            getActivity().startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static EventVSFragment newInstance(String eventJSONStr) {
        EventVSFragment fragment = new EventVSFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        appVS = (AppVS) getActivity().getApplicationContext();
        ResponseVS responseVS = null;
        try {
            if(getArguments().getString(ContextVS.EVENTVS_KEY) != null) {
                String dtoStr = getArguments().getString(ContextVS.EVENTVS_KEY);
                eventVS = JSON.readValue(dtoStr, EventVSDto.class);
            } else if (getArguments().getSerializable(ContextVS.VOTE_KEY) != null) {
                voteVSHelper = (VoteVSHelper) getArguments().getSerializable(ContextVS.VOTE_KEY);
                eventVS = voteVSHelper.getEventVS();
                responseVS = getArguments().getParcelable(ContextVS.RESPONSEVS_KEY);
            }
            eventVS.setAccessControlVS(appVS.getAccessControl());
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
        if(voteVSHelper != null) processVoteResult(voteVSHelper, responseVS);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if(bundle != null) {
            voteVSHelper = (VoteVSHelper) bundle.getSerializable(ContextVS.VOTE_KEY);
            optionSelected = (FieldEventVSDto) bundle.getSerializable(ContextVS.DTO_KEY);
        }
        if(voteVSHelper != null && voteVSHelper.getVoteReceipt() != null) showReceiptScreen(voteVSHelper);
        else setEventScreen(eventVS);
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_receipt_button:
                voteVSHelper.setTypeVS(TypeVS.SEND_VOTE);
                Uri uri = getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI,
                        ReceiptContentProvider.getContentValues(voteVSHelper, ReceiptWrapper.State.ACTIVE));
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
                Intent intent = new Intent(AppVS.getInstance(), BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, eventVS.getStatsServiceURL());
                intent.putExtra(ContextVS.DOUBLE_BACK_KEY, false);
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
                        ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            Intent intent = new Intent(getActivity(), DNIeSigningActivity.class);
                            intent.putExtra(ContextVS.PASSWORD_KEY, new
                                    String(responseVS.getMessageBytes()).toCharArray());
                            intent.putExtra(ContextVS.USER_KEY,
                                    AppVS.getInstance().getAccessControl().getName());
                            intent.putExtra(ContextVS.MESSAGE_KEY, getString(R.string.vote_selected_msg,
                                    optionSelected.getContent()));
                            voteVSHelper = VoteVSHelper.load(new VoteVSDto(eventVS, optionSelected));
                            AccessRequestDto requestDto = voteVSHelper.getAccessRequest();
                            intent.putExtra(ContextVS.MESSAGE_CONTENT_KEY, JSON.writeValueAsString(requestDto));
                            intent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, getString(R.string.request_msg_subject,
                                    voteVSHelper.getEventVS().getId()));
                            startActivityForResult(intent, RC_SIGN_ACCESS_REQUEST);
                        }
                    } else {
                        setOptionButtonsEnabled(true);
                        setProgressDialogVisible(false, null);
                    }
                    break;
                case RC_SIGN_ACCESS_REQUEST:
                    if(Activity.RESULT_OK == resultCode) {
                        ResponseVS responseVS = data.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
                        launchVoteService(TypeVS.SEND_VOTE, responseVS.getSMIME());
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void showReceiptScreen(final VoteVSHelper voteVSHelper) {
        LOGD(TAG, "showReceiptScreen");
        rootView.findViewById(R.id.receipt_buttons).setVisibility(View.VISIBLE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = voteVSHelper.getEventVS().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        try {
            String eventContent = new String(Base64.decode(
                    voteVSHelper.getEventVS().getContent().getBytes(), Base64.NO_WRAP), "UTF-8");
            contentTextView.setText(Html.fromHtml(eventContent));
        } catch (Exception ex) { ex.printStackTrace(); }
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<FieldEventVSDto> fieldsEventVS = voteVSHelper.getEventVS().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList == null) {
            voteOptionsButtonList = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 25, 15);
            for (final FieldEventVSDto option:fieldsEventVS) {
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
        Set<FieldEventVSDto> fieldsEventVS = event.getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(voteOptionsButtonList != null) linearLayout.removeAllViews();
        voteOptionsButtonList = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final FieldEventVSDto option:fieldsEventVS) {
            Button optionButton = new Button(getActivity());
            optionButton.setText(option.getContent());
            optionButton.setTextAppearance(getActivity(), R.style.electionOptionText);
            optionButton.setOnClickListener(new Button.OnClickListener() {
                FieldEventVSDto optionSelected = option;
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

    private void processSelectedOption(FieldEventVSDto optionSelected) {
        LOGD(TAG + ".processSelectedOption", "processSelectedOption");
        try {
            this.optionSelected = optionSelected;
            String passwMsg= optionSelected.getContent().length() >
                    ContextVS.SELECTED_OPTION_MAX_LENGTH ? optionSelected.getContent().substring(0,
                    ContextVS.SELECTED_OPTION_MAX_LENGTH) + "..." : optionSelected.getContent();
            Utils.initConnection(RC_ACCESS_REQUEST, passwMsg,
                    null, (AppCompatActivity)getActivity());
        } catch (Exception ex) { ex.printStackTrace(); }
    }


    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        ActivityResult activityResult = ((EventVSPagerActivity)getActivity()).getActivityResult();
        if(activityResult != null) {
            onActivityResult(activityResult.getRequestCode(),
                    activityResult.getResultCode(), activityResult.getData());
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
        outState.putSerializable(ContextVS.VOTE_KEY, voteVSHelper);
        outState.putSerializable(ContextVS.DTO_KEY, optionSelected);
    }

}