package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitSessionDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String deviceId;
    private String httpSessionId;

    public InitSessionDto() {}

    public InitSessionDto(String deviceId, String httpSessionId) {
        this.deviceId = deviceId;
        this.httpSessionId = httpSessionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }
}
