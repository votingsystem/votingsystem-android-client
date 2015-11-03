package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.VoteVSHelper;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteService extends IntentService {

    public static final String TAG = VoteService.class.getSimpleName();

    public VoteService() { super(TAG); }

    private AppVS appVS;

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        byte[] smimeMessageBytes = arguments.getByteArray(ContextVS.SMIME_MSG_KEY);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        VoteVSHelper voteVSHelper = (VoteVSHelper) intent.getSerializableExtra(ContextVS.VOTE_KEY);
        ResponseVS responseVS = null;
        String eventSubject = StringUtils.truncate(voteVSHelper.getSubject(), 50);
        Intent resultIntent = new Intent(serviceCaller);
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation);
            switch(operation) {
                case SEND_VOTE:
                    if(appVS.getControlCenter() == null) {
                        ControlCenterDto controlCenter = appVS.getActorVS(ControlCenterDto.class,
                                voteVSHelper.getEventVS().getControlCenter().getServerURL());
                        appVS.setControlCenter(controlCenter);
                    }
                    if(smimeMessageBytes != null) {
                        SMIMEMessage accessRequest = new SMIMEMessage(smimeMessageBytes);
                        responseVS = new VoteSender(voteVSHelper, accessRequest).call();
                    } else responseVS = new VoteSender(voteVSHelper).call();
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        voteVSHelper = (VoteVSHelper)responseVS.getData();
                        responseVS.setCaption(getString(R.string.vote_ok_caption)).
                                setNotificationMessage(getString(
                                        R.string.vote_ok_msg, eventSubject,
                                        voteVSHelper.getVote().getOptionSelected().getContent()));
                    } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                        responseVS.setCaption(getString(R.string.access_request_repeated_caption)).
                                setNotificationMessage(getString( R.string.access_request_repeated_msg,
                                eventSubject));
                    } else {
                        responseVS.setCaption(getString(R.string.vote_error_caption)).
                                setNotificationMessage(
                                Html.fromHtml(responseVS.getMessage()).toString());
                    }
                    break;
                case CANCEL_VOTE:
                    if(smimeMessageBytes == null) {
                        SMIMEMessage cancelVoteRequest = appVS.signMessage(appVS.getAccessControl().getName(),
                                JSON.writeValueAsString(voteVSHelper.getVoteCanceler()),
                                getString(R.string.cancel_vote_msg_subject));
                        smimeMessageBytes = cancelVoteRequest.getBytes();
                    }
                    responseVS = HttpHelper.sendData(smimeMessageBytes,
                            ContentTypeVS.JSON_SIGNED,
                            appVS.getAccessControl().getCancelVoteServiceURL());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        voteVSHelper.setCancelVoteReceipt(responseVS.getSMIME());
                        if(voteVSHelper.getLocalId() > 0) {//Update local receipt database
                            voteVSHelper.setTypeVS(TypeVS.CANCEL_VOTE);
                            getContentResolver().delete(ReceiptContentProvider.getReceiptURI(
                                    voteVSHelper.getLocalId()), null, null);
                        }
                        responseVS.setCaption(getString(R.string.cancel_vote_ok_caption)).
                                setNotificationMessage(getString(R.string.cancel_vote_result_msg,
                                eventSubject));
                    } else {
                        responseVS.setCaption(getString(R.string.cancel_vote_error_caption)).
                                setNotificationMessage(responseVS.getMessage());
                    }
                    break;
            }
            resultIntent.putExtra(ContextVS.VOTE_KEY, voteVSHelper);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setTypeVS(operation).setServiceCaller(serviceCaller);
            resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
            LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
        }
    }

}