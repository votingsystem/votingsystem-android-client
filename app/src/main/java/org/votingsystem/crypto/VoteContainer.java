package org.votingsystem.crypto;

import org.votingsystem.dto.identity.IdentityRequestDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteCancelerDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.HashUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.SignatureValidator;
import org.votingsystem.xades.XmlSignature;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteContainer extends ReceiptContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originHashIdentityRequest;
    private String hashIdentityRequestBase64;
    private String originRevocationHash;
    private String revocationHashBase64;
    private VoteDto voteDto;
    private ElectionDto election;
    private VoteCancelerDto cancelerDto;
    private IdentityRequestDto identityRequestDto;
    private CertificationRequest certificationRequest;

    private transient Set<XmlSignature> signatures;

    public static VoteContainer generate(ElectionDto election, ElectionOptionDto optionSelected,
                                         String identityServiceId) throws Exception {
        VoteContainer result = new VoteContainer();
        result.originHashIdentityRequest = UUID.randomUUID().toString();
        result.hashIdentityRequestBase64 = HashUtils.getHashBase64(
                result.originHashIdentityRequest.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        result.originRevocationHash = UUID.randomUUID().toString();
        result.revocationHashBase64 = HashUtils.getHashBase64(
                result.originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        result.election = election;

        IdentityRequestDto identityRequestDto = new IdentityRequestDto(OperationType.ANON_VOTE_CERT_REQUEST,
                identityServiceId, election.getEntityId()).setUUID(election.getUUID());
        identityRequestDto.setRevocationHashBase64(result.hashIdentityRequestBase64);
        result.identityRequestDto = identityRequestDto;

        VoteDto voteDto = new VoteDto(identityServiceId, election.getEntityId());
        voteDto.setIndentityServiceEntity(identityServiceId);
        voteDto.setOperation(OperationType.SEND_VOTE);
        voteDto.setRevocationHashBase64(result.revocationHashBase64);
        voteDto.setElectionUUID(election.getUUID());
        voteDto.setOptionSelected(optionSelected);
        result.voteDto = voteDto;
        result.certificationRequest = CertificationRequest.getVoteRequest(identityServiceId,
                election.getEntityId(), election.getUUID(), result.revocationHashBase64);
        return result;
    }

    public ElectionDto getElection() {
        return election;
    }

    public VoteDto getVote() {
        return voteDto;
    }

    public VoteCancelerDto getVoteCanceler() {
        if (cancelerDto == null) {
            cancelerDto = new VoteCancelerDto();
            cancelerDto.setOperation(OperationType.CANCEL_VOTE);
            cancelerDto.setOriginHashAccessRequest(originHashIdentityRequest);
            cancelerDto.setHashAccessRequestBase64(hashIdentityRequestBase64);
            cancelerDto.setOriginRevocationHash(originRevocationHash);
            cancelerDto.setRevocationHashBase64(revocationHashBase64);
            cancelerDto.setUUID(UUID.randomUUID().toString());
        }
        return cancelerDto;
    }

    public IdentityRequestDto getIdentityRequest() {
        return identityRequestDto;
    }

    public String getOriginHashIdentityRequest() {
        return originHashIdentityRequest;
    }

    public String getHashIdentityRequestBase64() {
        return hashIdentityRequestBase64;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public CertificationRequest getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequest certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public Date getVoteDate() throws Exception {
        if(signatures == null && getReceipt() != null)
            signatures = new SignatureValidator(getReceipt()).validate();
        if(signatures != null) {
            for(XmlSignature xmlSignature : signatures) {
                if(xmlSignature.isSigningCert())
                    return xmlSignature.getTimeStampDate();
            }
        }
        return null;
    }

    @Override
    public Set<XmlSignature> getSignatures() {
        try {
            if(signatures == null && getReceipt() != null)
                signatures = new SignatureValidator(getReceipt()).validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return signatures;
    }

    @Override
    public String getSubject() {
        if (election != null) return election.getSubject();
        else return null;
    }

}