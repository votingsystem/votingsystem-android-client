package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.model.VoteVS;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteVSDto {

    private Long id;
    private Long cancelerId;
    private Long eventVSId;
    private String certificateURL;
    private String hashAccessRequestBase64;
    private String hashAccessRequestHex;
    private String hashCertVSBase64;
    private String hashCertVoteHex;
    private String messageSMIMEURL;
    private String cancelationMessageSMIMEURL;
    private String eventVSURL;
    private VoteVS.State state;
    private FieldEventVSDto optionSelected;

    public VoteVSDto() {}


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

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public String getHashCertVoteHex() {
        return hashCertVoteHex;
    }

    public void setHashCertVoteHex(String hashCertVoteHex) {
        this.hashCertVoteHex = hashCertVoteHex;
    }

    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public String getCancelationMessageSMIMEURL() {
        return cancelationMessageSMIMEURL;
    }

    public void setCancelationMessageSMIMEURL(String cancelationMessageSMIMEURL) {
        this.cancelationMessageSMIMEURL = cancelationMessageSMIMEURL;
    }

    public String getEventVSURL() {
        return eventVSURL;
    }

    public void setEventVSURL(String eventVSURL) {
        this.eventVSURL = eventVSURL;
    }

    public VoteVS.State getState() {
        return state;
    }

    public void setState(VoteVS.State state) {
        this.state = state;
    }

    public FieldEventVSDto getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEventVSDto optionSelected) {
        this.optionSelected = optionSelected;
    }

}
