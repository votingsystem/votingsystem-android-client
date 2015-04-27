package org.votingsystem.callable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.util.ContextVS.KEY_SIZE;
import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIG_NAME;
import static org.votingsystem.util.ContextVS.VOTE_SIGN_MECHANISM;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = VoteSender.class.getSimpleName();

    private VoteVS vote;
    private AppVS appVS = null;

    public VoteSender(VoteVS vote, AppVS context) {
        this.vote = vote;
        this.appVS = context;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "eventvs subject: " + vote.getEventVS().getSubject());
        ResponseVS responseVS = null;
        try {
            vote.genVote();
            String serviceURL = appVS.getControlCenter().getVoteServiceURL();
            String subject = appVS.getString(R.string.request_msg_subject,
                    vote.getEventVS().getEventVSId());
            AccessRequestDto requestDto = vote.getAccessRequest();
            SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                    JSON.getMapper().writeValueAsString(requestDto), subject, appVS.getTimeStampServiceURL());
            //send access request to fetch the anonymous certificate that signs the vote
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.TEXT.getName();
            CertificationRequestVS certificationRequest = CertificationRequestVS.getVoteRequest(
                    KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER,
                    vote.getEventVS().getAccessControl().getServerURL(),
                    vote.getEventVS().getEventVSId(), vote.getHashCertVSBase64());
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" + MediaTypeVS.JSON_SIGNED;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, certificationRequest.getCsrPEM());
            mapToSend.put(accessRequestFileName, smimeMessage.getBytes());
            responseVS = HttpHelper.sendObjectMap(mapToSend,
                    appVS.getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            certificationRequest.initSigner(responseVS.getMessageBytes());
            JSONObject voteJSON = new JSONObject(vote.getVoteDataMap());
            SMIMEMessage signedVote = certificationRequest.getSMIME(
                    vote.getHashCertVSBase64(), vote.getEventVS().getControlCenter().getName(),
                    voteJSON.toString(), appVS.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, appVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedVote = timeStamper.getSMIME();
            responseVS = HttpHelper.sendData(signedVote.getBytes(), ContentTypeVS.VOTE,serviceURL);
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                cancelAccessRequest(); //AccesRequest OK and Vote error -> Cancel access request
                return responseVS;
            } else {
                SMIMEMessage voteReceipt = new SMIMEMessage(new ByteArrayInputStream(
                        responseVS.getMessageBytes()));
                try {
                    vote.setVoteReceipt(voteReceipt);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    cancelAccessRequest();
                    return new ResponseVS(ResponseVS.SC_ERROR,
                            appVS.getString(R.string.vote_option_mismatch));
                }
                byte[] base64EncodedKey = Base64.encode(
                        certificationRequest.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(
                        base64EncodedKey, appVS.getX509UserCert());
                vote.setCertificationRequest(certificationRequest);
                vote.setEncryptedKey(encryptedKey);
                responseVS.setData(vote);
            }
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(appVS.getString(R.string.exception_lbl),
                    appVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, appVS);
        } finally { return responseVS;}
    }

    private ResponseVS cancelAccessRequest() {
        LOGD(TAG + ".cancelAccessRequest", "cancelAccessRequest");
        try {
            String subject = appVS.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = appVS.getAccessControl().getCancelVoteServiceURL();
            JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
            SMIMEMessage smimeMessage = appVS.signMessage(appVS.getAccessControl().getName(),
                    cancelDataJSON.toString(), subject, appVS.getTimeStampServiceURL());
            return HttpHelper.sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED, serviceURL);
        } catch(Exception ex) {
            ex.printStackTrace();
            return ResponseVS.EXCEPTION(ex.getMessage(),
                    appVS.getString(R.string.exception_lbl));
        }
    }

}