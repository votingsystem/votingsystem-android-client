package org.votingsystem.callable;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.crypto.VoteVSHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = VoteSender.class.getSimpleName();

    private VoteVSHelper voteVSHelper;
    private CMSSignedMessage accessRequest;

    public VoteSender(VoteVSHelper voteVSHelper) {
        this.voteVSHelper = voteVSHelper;
    }

    public VoteSender(VoteVSHelper voteVSHelper, CMSSignedMessage accessRequest) {
        this.voteVSHelper = voteVSHelper;
        this.accessRequest = accessRequest;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "eventvs subject: " + voteVSHelper.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            String subject = AppVS.getInstance().getString(R.string.request_msg_subject,
                    voteVSHelper.getEventVS().getId());
            AccessRequestDto requestDto = voteVSHelper.getAccessRequest();
            if(accessRequest == null) accessRequest = AppVS.getInstance().signMessage(
                    AppVS.getInstance().getAccessControl().getName(),
                    JSON.writeValueAsString(requestDto), subject,
                    AppVS.getInstance().getTimeStampServiceURL());
            //send access request to fetch the anonymous certificate that signs the vote
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME,
                    voteVSHelper.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.ACCESS_REQUEST_FILE_NAME, accessRequest.toPEM());
            responseVS = HttpHelper.sendObjectMap(mapToSend,
                    AppVS.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            voteVSHelper.getCertificationRequest().initSigner(responseVS.getMessageBytes());
            CMSSignedMessage signedVote = new MessageTimeStamper(voteVSHelper.getCMSVote()).call();
            responseVS = HttpHelper.sendData(signedVote.toPEM(), ContentTypeVS.VOTE,
                    AppVS.getInstance().getControlCenter().getVoteServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                cancelAccessRequest(); //AccesRequest OK and Vote error -> Cancel access request
                return responseVS;
            } else {
                voteVSHelper.setVoteReceipt(responseVS.getCMS());
                responseVS.setData(voteVSHelper);
            }
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(AppVS.getInstance().getString(R.string.exception_lbl),
                    AppVS.getInstance().getString(R.string.password_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, AppVS.getInstance());
        } finally { return responseVS;}
    }

    private ResponseVS cancelAccessRequest() {
        LOGD(TAG + ".cancelAccessRequest", "cancelAccessRequest");
        try {
            String subject = AppVS.getInstance().getString(R.string.cancel_vote_msg_subject);
            String serviceURL = AppVS.getInstance().getAccessControl().getCancelVoteServiceURL();
            CMSSignedMessage cmsMessage = AppVS.getInstance().signMessage(AppVS.getInstance().getAccessControl().getName(),
                    JSON.writeValueAsString(voteVSHelper.getVoteCanceler()), subject,
                    AppVS.getInstance().getTimeStampServiceURL());
            return HttpHelper.sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED, serviceURL);
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseVS.EXCEPTION(ex.getMessage(),
                    AppVS.getInstance().getString(R.string.exception_lbl));
        }
    }

}