package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.ControlCenterDto;
import org.votingsystem.util.Constants;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.crypto.VoteHelper;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteService extends IntentService {

    public static final String TAG = VoteService.class.getSimpleName();

    public VoteService() { super(TAG); }

    private App app;

    @Override protected void onHandleIntent(Intent intent) {
        app = (App) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        byte[] cmsMessageBytes = arguments.getByteArray(Constants.CMS_MSG_KEY);
        OperationType operation = (OperationType)arguments.getSerializable(Constants.TYPEVS_KEY);
        VoteHelper voteHelper = (VoteHelper) intent.getSerializableExtra(Constants.VOTE_KEY);
        ResponseDto responseDto = null;
        String eventSubject = StringUtils.truncate(voteHelper.getSubject(), 50);
        Intent resultIntent = new Intent(serviceCaller);
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation);
            switch(operation) {
                case SEND_VOTE:
                    if(app.getControlCenter() == null) {
                        ControlCenterDto controlCenter = app.getActor(ControlCenterDto.class,
                                voteHelper.getEventVS().getControlCenter().getServerURL());
                        app.setControlCenter(controlCenter);
                    }
                    if(cmsMessageBytes != null) {
                        CMSSignedMessage accessRequest = new CMSSignedMessage(cmsMessageBytes);
                        responseDto = new VoteSender(voteHelper, accessRequest).call();
                    } else responseDto = new VoteSender(voteHelper).call();
                    if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                        voteHelper = (VoteHelper) responseDto.getData();
                        responseDto.setCaption(getString(R.string.vote_ok_caption)).
                                setNotificationMessage(getString(
                                        R.string.vote_ok_msg, eventSubject,
                                        voteHelper.getVote().getOptionSelected().getContent()));
                    } else if(ResponseDto.SC_ERROR_REQUEST_REPEATED == responseDto.getStatusCode()) {
                        responseDto.setCaption(getString(R.string.access_request_repeated_caption)).
                                setNotificationMessage(getString( R.string.access_request_repeated_msg,
                                eventSubject));
                    } else {
                        responseDto.setCaption(getString(R.string.vote_error_caption)).
                                setNotificationMessage(
                                Html.fromHtml(responseDto.getMessage()).toString());
                    }
                    break;
            }
            resultIntent.putExtra(Constants.VOTE_KEY, voteHelper);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            responseDto.setOperationType(operation).setServiceCaller(serviceCaller);
            resultIntent.putExtra(Constants.RESPONSEVS_KEY, responseDto);
            LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
        }
    }

}