package org.votingsystem.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.model.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    public static final String TAG = QRMessageDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public static final int INIT_REMOTE_SIGNED_BROWSER_SESSION = 0;
    public static final int QR_MESSAGE_INFO                    = 1;
    public static final int CURRENCY_SEND                      = 2;

    public static final String WEB_SOCKET_SESSION_KEY = "wsid";
    public static final String DEVICE_ID_KEY          = "did";
    public static final String OPERATION_KEY          = "op";
    public static final String OPERATION_ID_KEY       = "opid";
    public static final String PUBLIC_KEY_KEY         = "pk";


    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private T data;
    @JsonIgnore private String origingHashCertVS;
    @JsonIgnore private Currency currency ;
    private TypeVS operation;
    private String operationId;
    private Long deviceId;
    private Date dateCreated;
    private String hashCertVS;
    private String sessionId;
    private String currencyChangeCert;
    private String keyBase64;
    private String url;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(String sessionId, TypeVS typeVS) {
        this.sessionId = sessionId;
        this.typeVS = typeVS;
        dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }
    
    public QRMessageDto(DeviceVSDto deviceVSDto, TypeVS typeVS){
        this.typeVS = typeVS;
        this.deviceId = deviceVSDto.getId();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceId(Long.valueOf(msg.split(DEVICE_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(OPERATION_KEY + "=")) {
            int operationCode = Integer.valueOf(msg.split(OPERATION_KEY + "=")[1].split(";")[0]);
            switch (operationCode) {
                case INIT_REMOTE_SIGNED_BROWSER_SESSION:
                    qrMessageDto.setOperation(TypeVS.INIT_REMOTE_SIGNED_BROWSER_SESSION);
                    break;
                case QR_MESSAGE_INFO:
                    qrMessageDto.setOperation(TypeVS.QR_MESSAGE_INFO);
                    break;
                case CURRENCY_SEND:
                    qrMessageDto.setOperation(TypeVS.CURRENCY_SEND);
                default:
                    LOGD(TAG, "unknown operation code: " + operationCode);
            }
        }
        if (msg.contains(OPERATION_ID_KEY + "="))
            qrMessageDto.setOperationId(msg.split(OPERATION_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(WEB_SOCKET_SESSION_KEY + "="))
            qrMessageDto.setSessionId(msg.split(WEB_SOCKET_SESSION_KEY + "=")[1].split(";")[0]);
        if (msg.contains(PUBLIC_KEY_KEY + "="))
            qrMessageDto.setKeyBase64(msg.split(PUBLIC_KEY_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    public PublicKey getRSAPublicKey() throws Exception {
        PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(Base64.decode(keyBase64, Base64.NO_WRAP));
        return publicKey;
    }

    public DeviceVSDto getDeviceVS() throws Exception {
        DeviceVSDto dto = new DeviceVSDto(deviceId);
        dto.setPublicKey(getRSAPublicKey());
        return dto;
    }

    public static String toQRCode(TypeVS operation, String operationId, String deviceId, String sessionId) {
        StringBuilder result = new StringBuilder();
        if(deviceId != null) result.append(DEVICE_ID_KEY + "=" + deviceId + ";");
        if(operation != null) result.append(OPERATION_KEY + "=" + operation + ";");
        if(operationId != null) result.append(OPERATION_ID_KEY + "=" + operationId + ";");
        if(sessionId != null) result.append(WEB_SOCKET_SESSION_KEY + "=" + sessionId + ";");
        return result.toString();
    }

    public static QRMessageDto FROM_URL(String url) throws NoSuchAlgorithmException {
        QRMessageDto result = new QRMessageDto();
        result.setUrl(url);
        result.createRequest();
        return result;
    }

    public void createRequest() throws NoSuchAlgorithmException {
        this.origingHashCertVS = java.util.UUID.randomUUID().toString();
        this.hashCertVS = StringUtils.getHashBase64(origingHashCertVS, ContextVS.VOTING_DATA_DIGEST);
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOrigingHashCertVS() {
        return origingHashCertVS;
    }

    public void setOrigingHashCertVS(String origingHashCertVS) {
        this.origingHashCertVS = origingHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

    public String getSessionId() {
            return sessionId;
    }

    public QRMessageDto setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getKeyBase64() {
        return keyBase64;
    }

    public void setKeyBase64(String key) {
        this.keyBase64 = key;
    }

}