package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteSignedSessionDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private byte[] csr;
    private Long deviceId;
    private String sessionId;

    public byte[] getCsr() {
        return csr;
    }

    public void setCsr(byte[] csr) {
        this.csr = csr;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}