package org.votingsystem.dto;

import android.net.Uri;

import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Header;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVS {

	public static final String TAG = OperationVS.class.getSimpleName();

    private static final long serialVersionUID = 1L;
    
    private TypeVS typeVS;
    private Integer statusCode;
    private String caption;
    private String callerCallback;
    private String message;
    private String timeStampServerURL;
    private String serviceURL;
    private String serverURL;
    private String receiverName;
    private String signedMessageSubject;
    private EventVSDto eventVS;
    private String sessionId;
    private String publicKeyBase64;
    private Uri uriData;
    private String[] args;
    private List<String> targetCertList;
    private Map signedContent;
    private Map documentToEncrypt;
    private Map documentToDecrypt;
    private Map document;
    private String deviceFromName;
    private String toUser;
    private String textToSign;
    private String subject;
    private String UUID;
    private List<Header> headerList;

    public OperationVS() {}


    public static JSONArray getHeadersJSONArray(List<Header> headerList) throws JSONException {
        JSONArray headersArray = new JSONArray();
        if(headerList != null) {
            for(Header header : headerList) {
                if (header != null) {
                    JSONObject headerJSON = new JSONObject();
                    headerJSON.put("name", header.getName());
                    headerJSON.put("value", header.getValue());
                    headersArray.put(headerJSON);
                }
            }
        }
        return headersArray;
    }

    public static List<Header> getHeadersList(JSONArray jsonArray) throws JSONException {
        List<Header> headerList = new ArrayList<Header>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject headerJSON = (JSONObject) jsonArray.get(i);
            Header header = new Header(headerJSON.getString("name"), headerJSON.getString("value"));
            headerList.add(header);
        }
        return headerList;
    }

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

    public String[] getArgs() {
        return this.args;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public OperationVS setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
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


    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.decode(publicKeyBase64)));
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

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getTextToSign() {
        return textToSign;
    }

    public void setTextToSign(String textToSign) {
        this.textToSign = textToSign;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Header> getHeaderList() {
        return headerList;
    }

    public void setHeaderList(List<Header> headerList) {
        this.headerList = headerList;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    public List<String> getTargetCertList() {
        return targetCertList;
    }

    public void setTargetCertList(List<String> targetCertList) {
        this.targetCertList = targetCertList;
    }

    public Map getSignedContent() {
        return signedContent;
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        if(signedContent == null) return null;
        return JSON.getMapper().readValue(JSON.getMapper().writeValueAsString(signedContent), type);
    }

    public <T> T getSignedContent(TypeReference<T> type) throws Exception {
        if(signedContent == null) return null;
        return JSON.getMapper().readValue(JSON.getMapper().writeValueAsString(signedContent), type);
    }

    public void setSignedContent(Map signedContent) {
        this.signedContent = signedContent;
    }

    public Map getDocumentToEncrypt() {
        return documentToEncrypt;
    }

    public void setDocumentToEncrypt(Map documentToEncrypt) {
        this.documentToEncrypt = documentToEncrypt;
    }

    public Map getDocumentToDecrypt() {
        return documentToDecrypt;
    }

    public void setDocumentToDecrypt(Map documentToDecrypt) {
        this.documentToDecrypt = documentToDecrypt;
    }

    public Map getDocument() {
        return document;
    }

    public void setDocument(Map document) {
        this.document = document;
    }
}


