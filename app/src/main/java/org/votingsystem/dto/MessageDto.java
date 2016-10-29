package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    private Integer statusCode;
    private OperationType operation;
    private String message;
    private String cmsMessagePEM;
    private String URL;
    private String deviceId;
    private String httpSessionId;
    private String UUID;

    public MessageDto() {}

    public MessageDto(String deviceId, String httpSessionId) {
        this.deviceId = deviceId;
        this.httpSessionId = httpSessionId;
    }

    public MessageDto(Integer statusCode, String message, String URL) {
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static MessageDto OK(String message, String URL) {
        return new MessageDto(ResponseDto.SC_OK, message, URL);
    }

    public static MessageDto REQUEST_REPEATED(String message, String URL) {
        return new MessageDto(ResponseDto.SC_ERROR_REQUEST_REPEATED, message, URL);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }


    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public String getUUID() {
        return UUID;
    }

    public static MessageDto REQUEST(String deviceId, String httpSessionId) {
        MessageDto result = new MessageDto(deviceId, httpSessionId);
        result.UUID = java.util.UUID.randomUUID().toString();
        return result;
    }
}
