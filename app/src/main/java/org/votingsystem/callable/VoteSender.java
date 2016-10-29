package org.votingsystem.callable;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.crypto.VoteHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseDto> {

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

    @Override public ResponseDto call() {
        LOGD(TAG + ".call", "eventvs subject: " + voteHelper.getEventVS().getSubject());
        ResponseDto responseDto = null;
        try {
            String subject = App.getInstance().getString(R.string.request_msg_subject,
                    voteHelper.getEventVS().getId());
            AccessRequestDto requestDto = voteHelper.getAccessRequest();
            if(accessRequest == null) accessRequest = App.getInstance().signMessage(
                    JSON.writeValueAsBytes(requestDto));
            //send access request to fetch the anonymous certificate that signs the vote
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(Constants.CSR_FILE_NAME,
                    voteHelper.getCertificationRequest().getCsrPEM());
            mapToSend.put(Constants.ACCESS_REQUEST_FILE_NAME, accessRequest.toPEM());
            responseDto = HttpConnection.getInstance().sendObjectMap(mapToSend,
                    App.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseDto.SC_OK != responseDto.getStatusCode()) return responseDto;
            voteHelper.getCertificationRequest().initSigner(responseDto.getMessageBytes());


            CMSSignedMessage signedVote = voteHelper.getCMSVote();
            responseDto = HttpConnection.getInstance().sendData(signedVote.toPEM(), ContentType.VOTE,
                    App.getInstance().getControlCenter().getVoteServiceURL());
            if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
                cancelAccessRequest(); //AccesRequest OK and Vote error -> Cancel access request
                return responseDto;
            } else {
                voteHelper.setVoteReceipt(responseDto.getCMS());
                responseDto.setData(voteHelper);
            }
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(App.getInstance().getString(R.string.exception_lbl),
                    App.getInstance().getString(R.string.password_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, App.getInstance());
        } finally { return responseDto;}
    }

    private ResponseDto cancelAccessRequest() {
        LOGD(TAG + ".cancelAccessRequest", "cancelAccessRequest");
        try {
            String serviceURL = App.getInstance().getAccessControl().getCancelVoteServiceURL();
            CMSSignedMessage cmsMessage = App.getInstance().signMessage(
                    JSON.writeValueAsBytes(voteHelper.getVoteCanceler()));
            return HttpConnection.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED, serviceURL);
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseDto.EXCEPTION(ex.getMessage(),
                    App.getInstance().getString(R.string.exception_lbl));
        }
    }

}