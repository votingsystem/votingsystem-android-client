package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.OperationType;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;


    private OperationType operation = OperationType.ACCESS_REQUEST;
    private String eventURL;
    private Long eventId;
    private String hashAccessRequestBase64;
    private String revocationHashBase64;
    private String UUID;


    public AccessRequestDto() {}

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

}