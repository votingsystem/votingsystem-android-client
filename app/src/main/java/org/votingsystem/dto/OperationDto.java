package org.votingsystem.dto;

import android.net.Uri;

import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.util.OperationType;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationDto implements Serializable {

    public static final String TAG = OperationDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    private OperationType operation;
    private Integer statusCode;
    private String caption;
    private String message;
    private String email;
    private String serviceURL;
    private String serverURL;
    private String receiverName;
    private ElectionDto election;
    private Uri uriData;
    private String toUser;
    private String subject;
    private String UUID;


    private String callerCallback;

    public OperationDto() {
    }

    public OperationDto(int statusCode) {
        this.statusCode = statusCode;
    }

    public OperationDto(OperationType operationType) {
        this.operation = operationType;
    }

    public OperationDto(OperationType operation, Uri uriData) {
        this.operation = operation;
        this.uriData = uriData;
    }


    public OperationDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public OperationDto(int statusCode, String message, OperationType operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public OperationDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ElectionDto getElection() {
        return election;
    }

    public void setElection(ElectionDto election) {
        this.election = election;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public Uri getUriData() {
        return uriData;
    }

    public void setUriData(Uri uriData) {
        this.uriData = uriData;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}


