package org.votingsystem.callable;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = VoteSender.class.getSimpleName();

    private VoteHelper voteHelper;
    private CMSSignedMessage accessRequest;

    public VoteSender(VoteHelper voteHelper) {
        this.voteHelper = voteHelper;
    }

    public VoteSender(VoteHelper voteHelper, CMSSignedMessage accessRequest) {
        this.voteHelper = voteHelper;
        this.accessRequest = accessRequest;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "eventvs subject: " + voteHelper.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            String subject = AppVS.getInstance().getString(R.string.request_msg_subject,
                    voteHelper.getEventVS().getId());
            AccessRequestDto requestDto = voteHelper.getAccessRequest();
            if(accessRequest == null) accessRequest = AppVS.getInstance().signMessage(
                    JSON.writeValueAsBytes(requestDto));
            //send access request to fetch the anonymous certificate that signs the vote
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME,
                    voteHelper.getCertificationRequest().getCsrPEM());
            mapToSend.put(ContextVS.ACCESS_REQUEST_FILE_NAME, accessRequest.toPEM());
            responseVS = HttpHelper.sendObjectMap(mapToSend,
                    AppVS.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            voteHelper.getCertificationRequest().initSigner(responseVS.getMessageBytes());


            CMSSignedMessage signedVote = voteHelper.getCMSVote();
            responseVS = HttpHelper.sendData(signedVote.toPEM(), ContentType.VOTE,
                    AppVS.getInstance().getControlCenter().getVoteServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                cancelAccessRequest(); //AccesRequest OK and Vote error -> Cancel access request
                return responseVS;
            } else {
                voteHelper.setVoteReceipt(responseVS.getCMS());
                responseVS.setData(voteHelper);
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
            String serviceURL = AppVS.getInstance().getAccessControl().getCancelVoteServiceURL();
            CMSSignedMessage cmsMessage = AppVS.getInstance().signMessage(
                    JSON.writeValueAsBytes(voteHelper.getVoteCanceler()));
            return HttpHelper.sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED, serviceURL);
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseVS.EXCEPTION(ex.getMessage(),
                    AppVS.getInstance().getString(R.string.exception_lbl));
        }
    }

}