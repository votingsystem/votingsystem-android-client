package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;

import org.json.JSONObject;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.contentprovider.ReceiptContentProvider;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.util.ArrayList;
import java.util.List;

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
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        VoteVS vote = (VoteVS) intent.getSerializableExtra(ContextVS.VOTE_KEY);
        ResponseVS responseVS = null;
        String eventSubject = null;
        Intent resultIntent = new Intent(responseVS.getServiceCaller());
        if(vote != null) {
            eventSubject = vote.getEventVS().getSubject();
            if(eventSubject.length() > 50) eventSubject = eventSubject.substring(0, 50) + "...";
        }
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation);
            switch(operation) {

                case VOTEVS:
                    if(appVS.getControlCenter() == null) {
                        ControlCenterDto controlCenter = appVS.getActorVS(ControlCenterDto.class,
                                vote.getEventVS().getControlCenter().getServerURL());
                        appVS.setControlCenter(controlCenter);
                    }
                    VoteSender voteSender = new VoteSender(vote, appVS);
                    responseVS = voteSender.call();
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        VoteVS voteReceipt = (VoteVS)responseVS.getData();
                        responseVS.setCaption(getString(R.string.vote_ok_caption)).
                                setNotificationMessage(getString(
                                R.string.vote_ok_msg, eventSubject,
                                vote.getOptionSelected().getContent()));
                    } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                        responseVS.setUrl(responseVS.getMessage());
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
                    JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
                    SMIMEMessage smime = appVS.signMessage(appVS.getAccessControl().getName(),
                            cancelDataJSON.toString(), getString(R.string.cancel_vote_msg_subject));
                    responseVS = HttpHelper.sendData(smime.getBytes(),
                            ContentTypeVS.JSON_SIGNED,
                            appVS.getAccessControl().getCancelVoteServiceURL());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        SMIMEMessage cancelReceipt = responseVS.getSMIME();
                        vote.setCancelVoteReceipt(cancelReceipt);
                        if(vote.getLocalId() > 0) {//Update local receipt database
                            ContentValues values = new ContentValues();
                            vote.setTypeVS(TypeVS.VOTEVS_CANCELLED);
                            values.put(ReceiptContentProvider.URL_COL, cancelReceipt.getMessageID());
                            values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                                    ObjectUtils.serializeObject(vote));
                            values.put(ReceiptContentProvider.TYPE_COL, vote.getTypeVS().toString());
                            values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL,
                                    System.currentTimeMillis());
                            getContentResolver().update(ReceiptContentProvider.getReceiptURI(
                                    vote.getLocalId()), values, null, null);
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
            resultIntent.putExtra(ContextVS.VOTE_KEY, vote);
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