package org.votingsystem.util.crypto;

import org.votingsystem.AppVS;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.FieldEventVSDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteVSHelper extends ReceiptWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long eventVSId;
    private String eventVSURL;
    private String NIF;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private String originHashCertVote;
    private String hashCertVSBase64;
    private VoteVSDto voteVSDto;
    private EventVSDto eventVS;
    private VoteVSCancelerDto cancelerDto;
    private AccessRequestDto accessRequestDto;
    private transient CMSSignedMessage voteReceipt;
    private transient CMSSignedMessage cancelVoteReceipt;
    private CertificationRequestVS certificationRequest;

    public static VoteVSHelper load(VoteVSDto voteVSDto) throws Exception {
        VoteVSHelper voteVSHelper = new VoteVSHelper();
        voteVSHelper.originHashAccessRequest = UUID.randomUUID().toString();
        voteVSHelper.hashAccessRequestBase64 = StringUtils.getHashBase64(
                voteVSHelper.originHashAccessRequest, ContextVS.DATA_DIGEST_ALGORITHM);
        voteVSHelper.originHashCertVote = UUID.randomUUID().toString();
        voteVSHelper.hashCertVSBase64 = StringUtils.getHashBase64(
                voteVSHelper.originHashCertVote, ContextVS.DATA_DIGEST_ALGORITHM);
        voteVSHelper.eventVSId = voteVSDto.getEventVS().getId();
        voteVSHelper.eventVSURL = voteVSDto.getEventVS().getURL();
        voteVSHelper.eventVS = voteVSDto.getEventVS();
        voteVSHelper.genVote(voteVSDto.getOptionSelected());
        voteVSHelper.certificationRequest = CertificationRequestVS.getVoteRequest(
                SIGNATURE_ALGORITHM, PROVIDER,
                voteVSDto.getEventVS().getAccessControl().getServerURL(),
                voteVSDto.getEventVS().getId(),
                voteVSHelper.hashCertVSBase64);
        return voteVSHelper;
    }

    public static VoteVSHelper genRandomVote(Long eventVSId, String eventVSURL,
             Set<FieldEventVSDto> options) throws Exception {
        VoteVSDto voteVSDto =  new VoteVSDto();
        voteVSDto.setEventVSId(eventVSId);
        voteVSDto.setEventURL(eventVSURL);
        voteVSDto.setOptionSelected(getRandomOption(options));
        return VoteVSHelper.load(voteVSDto);
    }

    public static FieldEventVSDto getRandomOption (Set<FieldEventVSDto> options) {
        int item = new Random().nextInt(options.size()); // In real life, the Random object should be rather more shared than this
        return (FieldEventVSDto) options.toArray()[item];
    }

    public CMSSignedMessage getCMSVote() throws Exception {
        return certificationRequest.signData(JSON.writeValueAsString(voteVSDto));
    }

    private void genVote(FieldEventVSDto optionSelected) {
        genAccessRequest();
        voteVSDto = new VoteVSDto();
        voteVSDto.setOperation(TypeVS.SEND_VOTE);
        voteVSDto.setHashCertVSBase64(hashCertVSBase64);
        voteVSDto.setEventVSId(eventVSId);
        voteVSDto.setEventURL(eventVSURL);
        voteVSDto.setOptionSelected(optionSelected);
        voteVSDto.setUUID(UUID.randomUUID().toString());
    }


    private void genAccessRequest() {
        accessRequestDto = new AccessRequestDto();
        accessRequestDto.setEventId(eventVSId);
        accessRequestDto.setEventURL(eventVSURL);
        accessRequestDto.setHashAccessRequestBase64(hashAccessRequestBase64);
        accessRequestDto.setUUID(UUID.randomUUID().toString());
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public VoteVSDto getVote() {
        return voteVSDto;
    }

    public VoteVSCancelerDto getVoteCanceler() {
        if(cancelerDto == null) {
            cancelerDto = new VoteVSCancelerDto();
            cancelerDto.setOperation(TypeVS.CANCEL_VOTE);
            cancelerDto.setOriginHashAccessRequest(originHashAccessRequest);
            cancelerDto.setHashAccessRequestBase64(hashAccessRequestBase64);
            cancelerDto.setOriginHashCertVote(originHashCertVote);
            cancelerDto.setHashCertVSBase64(hashCertVSBase64);
            cancelerDto.setUUID(UUID.randomUUID().toString());
        }
        return cancelerDto;
    }

    public AccessRequestDto getAccessRequest() {
        return accessRequestDto;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public CMSSignedMessage getVoteReceipt() {
        return voteReceipt;
    }

    public void setVoteReceipt(CMSSignedMessage voteReceipt) {
        this.voteReceipt = voteReceipt;
    }

    public CMSSignedMessage getCancelVoteReceipt() {
        return cancelVoteReceipt;
    }

    public void setCancelVoteReceipt(CMSSignedMessage cancelVoteReceipt) {
        this.cancelVoteReceipt = cancelVoteReceipt;
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequestVS certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    @Override public String getSubject() {
        if(eventVS != null) return eventVS.getSubject();
        else return null;
    }

    @Override
    public CMSSignedMessage getReceipt() throws Exception {
        if(cancelVoteReceipt != null) return cancelVoteReceipt;
        else return voteReceipt;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(voteReceipt != null) s.writeObject(voteReceipt.getEncoded());
            else s.writeObject(null);
            if(cancelVoteReceipt != null) s.writeObject(cancelVoteReceipt.getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] voteReceiptBytes = (byte[]) s.readObject();
        if(voteReceiptBytes != null) voteReceipt = new CMSSignedMessage(voteReceiptBytes);
        byte[] cancelVoteReceiptBytes = (byte[]) s.readObject();
        if(cancelVoteReceiptBytes != null) cancelVoteReceipt = new CMSSignedMessage(cancelVoteReceiptBytes);
    }

    public String getCMSVoteURL() {
        String result = null;
        try {
            String hashHex = StringUtils.toHex(hashCertVSBase64);
            result = AppVS.getInstance().getAccessControl().getCMSVoteURL(hashHex);
        } catch (Exception ex) { ex.printStackTrace();}
        return result;
    }
}