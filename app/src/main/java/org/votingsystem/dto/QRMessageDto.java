package org.votingsystem.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.model.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    public static final String TAG = QRMessageDto.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public static final int INIT_REMOTE_SIGNED_SESSION         = 0;
    public static final int MESSAGE_INFO                       = 1;
    public static final int CURRENCY_SEND                      = 2;
    public static final int USER_INFO                          = 3;
    public static final int VOTE                               = 4;
    public static final int OPERATION_PROCESS                  = 5;
    public static final int ANONYMOUS_REPRESENTATIVE_SELECTION = 6;
    public static final int GET_AES_PARAMS                     = 7;


    public static final int CURRENCY_SYSTEM                  = 0;
    public static final int VOTING_SYSTEM                    = 1;

    public static final String DEVICE_ID_KEY          = "did";
    public static final String ITEM_ID_KEY            = "iid";
    public static final String OPERATION_KEY          = "op";
    public static final String OPERATION_CODE_KEY     = "opid";
    public static final String PUBLIC_KEY_KEY         = "pk";
    public static final String SERVER_KEY             = "srv";
    public static final String MSG_KEY                = "msg";


    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private TypeVS sessionType;
    @JsonIgnore private T data;
    @JsonIgnore private String origingHashCertVS;
    @JsonIgnore private Currency currency;
    @JsonIgnore private DeviceDto device;
    @JsonIgnore private AESParamsDto aesParams;;
    private TypeVS operation;
    private String operationCode;
    private Long deviceId;
    private Long itemId;
    private Date dateCreated;
    private String hashCertVS;
    private String sessionId;
    private String currencyChangeCert;
    private String publicKeyBase64;
    private String url;
    private String msg;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(String sessionId, TypeVS typeVS) {
        this.sessionId = sessionId;
        this.typeVS = typeVS;
        dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }
    
    public QRMessageDto(DeviceDto deviceDto, TypeVS typeVS){
        this.typeVS = typeVS;
        this.deviceId = deviceDto.getId();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceId(Long.valueOf(msg.split(DEVICE_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(ITEM_ID_KEY + "="))
            qrMessageDto.setItemId(Long.valueOf(msg.split(ITEM_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(OPERATION_KEY + "=")) {
            int operationCode = Integer.valueOf(msg.split(OPERATION_KEY + "=")[1].split(";")[0]);
            switch (operationCode) {
                case INIT_REMOTE_SIGNED_SESSION:
                    qrMessageDto.setOperation(TypeVS.INIT_REMOTE_SIGNED_SESSION);
                    break;
                case MESSAGE_INFO:
                    qrMessageDto.setOperation(TypeVS.MESSAGE_INFO);
                    break;
                case CURRENCY_SEND:
                    qrMessageDto.setOperation(TypeVS.CURRENCY_SEND);
                    break;
                case USER_INFO:
                    qrMessageDto.setOperation(TypeVS.USER_INFO);
                    break;
                case VOTE:
                    qrMessageDto.setOperation(TypeVS.SEND_VOTE);
                    break;
                case OPERATION_PROCESS:
                    qrMessageDto.setOperation(TypeVS.OPERATION_PROCESS);
                    break;
                case ANONYMOUS_REPRESENTATIVE_SELECTION:
                    qrMessageDto.setOperation(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
                    break;
                case GET_AES_PARAMS:
                    qrMessageDto.setOperation(TypeVS.GET_AES_PARAMS);
                    break;
                default:
                    LOGD(TAG, "unknown operation code: " + operationCode);
            }
        }
        if (msg.contains(SERVER_KEY + "=")) {
            int systemCode = Integer.valueOf(msg.split(SERVER_KEY + "=")[1].split(";")[0]);
            switch (systemCode) {
                case CURRENCY_SYSTEM:
                    qrMessageDto.setSessionType(TypeVS.CURRENCY_SYSTEM);
                    break;
                case VOTING_SYSTEM:
                    qrMessageDto.setSessionType(TypeVS.VOTING_SYSTEM);
                    break;
                default:
                    LOGD(TAG, "unknown system code: " + systemCode);
            }
        }
        if (msg.contains(OPERATION_CODE_KEY + "="))
            qrMessageDto.setOperationCode(msg.split(OPERATION_CODE_KEY + "=")[1].split(";")[0]);
        if (msg.contains(PUBLIC_KEY_KEY + "="))
            qrMessageDto.setPublicKeyBase64(msg.split(PUBLIC_KEY_KEY + "=")[1].split(";")[0]);
        if (msg.contains(MSG_KEY + "="))
            qrMessageDto.setMsg(msg.split(MSG_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    @JsonIgnore
    public PublicKey getRSAPublicKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        //fix qr codes replacements of '+' with spaces
        publicKeyBase64 = publicKeyBase64.replace(" ", "+");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.NO_WRAP));
        return factory.generatePublic(pubKeySpec);
    }

    @JsonIgnore
    public DeviceDto getDevice() throws Exception {
        if(device != null) return device;
        DeviceDto dto = new DeviceDto(deviceId);
        if(publicKeyBase64 != null) dto.setPublicKey(getRSAPublicKey());
        return dto;
    }
    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public static String toQRCode(TypeVS operation, String operationCode, String deviceId) {
        StringBuilder result = new StringBuilder();
        if(deviceId != null) result.append(DEVICE_ID_KEY + "=" + deviceId + ";");
        if(operation != null) result.append(OPERATION_KEY + "=" + operation + ";");
        if(operationCode != null) result.append(OPERATION_CODE_KEY + "=" + operationCode + ";");
        return result.toString();
    }

    public static QRMessageDto FROM_URL(String url) throws NoSuchAlgorithmException {
        QRMessageDto result = new QRMessageDto();
        result.setUrl(url);
        result.createRequest();
        return result;
    }

    public QRMessageDto createRequest() throws NoSuchAlgorithmException {
        this.origingHashCertVS = java.util.UUID.randomUUID().toString();
        this.hashCertVS = StringUtils.getHashBase64(origingHashCertVS, ContextVS.DATA_DIGEST_ALGORITHM);
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public QRMessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
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

    public QRMessageDto setOperation(TypeVS operation) {
        this.operation = operation;
        return this;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
        if(this.operationCode != null) this.operationCode = this.operationCode.toUpperCase();
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String key) {
        this.publicKeyBase64 = key;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public TypeVS getSessionType() {
        if(sessionType == null) return TypeVS.CURRENCY_SYSTEM;
        return sessionType;
    }

    public void setSessionType(TypeVS sessionType) {
        this.sessionType = sessionType;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}