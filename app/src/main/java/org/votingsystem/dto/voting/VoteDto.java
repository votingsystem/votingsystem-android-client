package org.votingsystem.dto.voting;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = VoteDto.class.getSimpleName();

    public enum State {OK, CANCELLED, ERROR}

    private OperationType operation;
    private State state;

    private String originHashAccessRequest;
    private String revocationHashBase64;
    private String originRevocationHash;
    private String indentityServiceEntity;
    private String votingServiceEntity;
    private String electionUUID;
    private ElectionOptionDto optionSelected;

    private transient TimeStampToken timeStampToken;
    private X509Certificate x509Certificate;


    public VoteDto() {  }

    public VoteDto(String indentityServiceEntity, String votingServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ElectionOptionDto getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(ElectionOptionDto optionSelected) {
        this.optionSelected = optionSelected;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public void setIndentityServiceEntity(String indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
    }

    public String getVotingServiceEntity() {
        return votingServiceEntity;
    }

    public void setVotingServiceEntity(String votingServiceEntity) {
        this.votingServiceEntity = votingServiceEntity;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

}