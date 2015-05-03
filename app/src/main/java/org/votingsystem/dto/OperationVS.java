package org.votingsystem.dto;

import android.net.Uri;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVS {

	public static final String TAG = OperationVS.class.getSimpleName();

    private static final long serialVersionUID = 1L;
    
    private TypeVS typeVS;
    private Integer statusCode;
    private String caption;
    private String message;
    private String serviceURL;
    private String serverURL;
    private String receiverName;
    private String signedMessageSubject;
    private EventVSDto eventVS;
    private Uri uriData;
    private String toUser;
    private String jsonStr;
    private String subject;
    private String UUID;

    @JsonProperty("objectId") private String callerCallback;


    public OperationVS() {}

    public OperationVS(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public OperationVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public OperationVS(TypeVS typeVS, Uri uriData) {
        this.typeVS = typeVS;
        this.uriData = uriData;
    }

    public OperationVS(String typeVS) {
        this.typeVS = TypeVS.valueOf(typeVS);
    }
    
    public OperationVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public OperationVS(int statusCode, String message, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.message = message;
        this.typeVS = typeVS;
    }

    public String getUUID() {
        return UUID;
    }

    public OperationVS setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSignedMessageSubject() {
        return signedMessageSubject;
    }

    public void setSignedMessageSubject(String signedMessageSubject) {
        this.signedMessageSubject = signedMessageSubject;
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

    public <T> T getSignedContent(Class<T> type) throws Exception {
        if(jsonStr == null) return null;
        return JSON.readValue(JSON.writeValueAsString(jsonStr), type);
    }

    public <T> T getSignedContent(TypeReference<T> type) throws Exception {
        if(jsonStr == null) return null;
        return JSON.readValue(JSON.writeValueAsString(jsonStr), type);
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }
}


