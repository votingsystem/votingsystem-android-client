package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.crypto.CertUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = VoteDto.class.getSimpleName();

    public enum State {OK, CANCELLED, ERROR}

    private OperationType operation;
    private Long id;
    private Long cancelerId;
    private Long eventVSId;
    private String certificateURL;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private String hashAccessRequestHex;
    private String originHashCertVote;
    private String revocationHashBase64;
    private String revocationHashHex;
    private String cmsMessageURL;
    private String cancelationCmsMessageURL;
    private String eventURL;
    private String UUID;
    private EventVSDto eventVS;
    private State state;
    private FieldEventDto optionSelected;

    private String accessControlURL;
    private String representativeURL;
    private Long accessControlEventVSId;

    @JsonIgnore private transient Set<X509Certificate> serverCerts = new HashSet<>();
    @JsonIgnore private transient TimeStampToken timeStampToken;
    @JsonIgnore private X509Certificate x509Certificate;


    public VoteDto() {}


    public VoteDto(EventVSDto eventVS, FieldEventDto optionSelected) {
        this.eventVS = eventVS;
        this.optionSelected = optionSelected;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCancelerId() {
        return cancelerId;
    }

    public void setCancelerId(Long cancelerId) {
        this.cancelerId = cancelerId;
    }

    public Long getEventVSId() {
        return eventVSId;
    }

    public void setEventVSId(Long eventVSId) {
        this.eventVSId = eventVSId;
    }

    public String getCertificateURL() {
        return certificateURL;
    }

    public void setCertificateURL(String certificateURL) {
        this.certificateURL = certificateURL;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getHashAccessRequestHex() {
        return hashAccessRequestHex;
    }

    public void setHashAccessRequestHex(String hashAccessRequestHex) {
        this.hashAccessRequestHex = hashAccessRequestHex;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public String getRevocationHashHex() {
        return revocationHashHex;
    }

    public void setRevocationHashHex(String revocationHashHex) {
        this.revocationHashHex = revocationHashHex;
    }

    public String getCmsMessageURL() {
        return cmsMessageURL;
    }

    public void setCmsMessageURL(String cmsMessageURL) {
        this.cmsMessageURL = cmsMessageURL;
    }

    public String getCancelationCmsMessageURL() {
        return cancelationCmsMessageURL;
    }

    public void setCancelationCmsMessageURL(String cancelationCmsMessageURL) {
        this.cancelationCmsMessageURL = cancelationCmsMessageURL;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public FieldEventDto getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEventDto optionSelected) {
        this.optionSelected = optionSelected;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
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

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public Long getAccessControlEventVSId() {
        return accessControlEventVSId;
    }

    public void setAccessControlEventVSId(Long accessControlEventVSId) {
        this.accessControlEventVSId = accessControlEventVSId;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public void setOriginHashCertVote(String originHashCertVote) {
        this.originHashCertVote = originHashCertVote;
    }

    public void setServerCerts(Set<X509Certificate> serverCerts) {
        this.serverCerts = serverCerts;
    }

    public VoteDto loadSignatureData(X509Certificate x509Certificate,
                                     TimeStampToken timeStampToken) throws Exception {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
        VoteCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(VoteCertExtensionDto.class,
                x509Certificate, Constants.VOTE_OID);
        this.accessControlEventVSId = certExtensionDto.getEventId();
        this.accessControlURL = certExtensionDto.getAccessControlURL();
        this.revocationHashBase64 = certExtensionDto.getRevocationHash();
        this.representativeURL = CertUtils.getCertExtensionData(x509Certificate,
                Constants.REPRESENTATIVE_VOTE_OID);
        return this;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

}