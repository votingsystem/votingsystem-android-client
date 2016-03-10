package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.VoteHelper;

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
        byte[] cmsMessageBytes = arguments.getByteArray(ContextVS.CMS_MSG_KEY);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        VoteHelper voteHelper = (VoteHelper) intent.getSerializableExtra(ContextVS.VOTE_KEY);
        ResponseVS responseVS = null;
        String eventSubject = StringUtils.truncate(voteHelper.getSubject(), 50);
        Intent resultIntent = new Intent(serviceCaller);
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation);
            switch(operation) {
                case SEND_VOTE:
                    if(appVS.getControlCenter() == null) {
                        ControlCenterDto controlCenter = appVS.getActorVS(ControlCenterDto.class,
                                voteHelper.getEventVS().getControlCenter().getServerURL());
                        appVS.setControlCenter(controlCenter);
                    }
                    if(cmsMessageBytes != null) {
                        CMSSignedMessage accessRequest = new CMSSignedMessage(cmsMessageBytes);
                        responseVS = new VoteSender(voteHelper, accessRequest).call();
                    } else responseVS = new VoteSender(voteHelper).call();
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        voteHelper = (VoteHelper)responseVS.getData();
                        responseVS.setCaption(getString(R.string.vote_ok_caption)).
                                setNotificationMessage(getString(
                                        R.string.vote_ok_msg, eventSubject,
                                        voteHelper.getVote().getOptionSelected().getContent()));
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
            }
            resultIntent.putExtra(ContextVS.VOTE_KEY, voteHelper);
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