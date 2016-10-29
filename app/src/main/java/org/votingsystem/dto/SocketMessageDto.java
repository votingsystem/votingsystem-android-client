package org.votingsystem.dto;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.App;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.util.crypto.Encryptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = SocketMessageDto.class.getSimpleName();

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}

    private OperationType operation;
    private String operationCode;
    private OperationType messageType;
    private OperationType step;
    private Integer statusCode;
    private Long deviceFromId;
    private Long deviceToId;
    private String subject;
    private String message;
    private String encryptedMessage;
    private String UUID;
    private String locale = Locale.getDefault().getLanguage().toLowerCase();
    private String remoteAddress;
    private String cmsMessagePEM;
    private AESParamsDto aesParams;
    private String x509CertificatePEM;
    private String publicKeyPEM;
    private String from;
    private String caption;
    private String deviceId;
    private Boolean timeLimited;
    private String deviceFromName;
    private String deviceToName;
    private String toUser;
    private String URL;
    private DeviceDto connectedDevice;
    private List<CurrencyDto> currencyList;
    private Date date;


    @JsonIgnore private UserDto user;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private QRMessageDto qrMessage;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private transient CMSSignedMessage cms;

    public SocketMessageDto () {}

    public SocketMessageDto(Integer statusCode, String message, OperationType operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message,
                        CMSSignedMessage cmsMessage, OperationType operation) throws Exception {
        WebSocketSession socketSession = App.getInstance().getWSSession(UUID);
        socketSession.setOperationType(operation);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(OperationType.MSG_TO_DEVICE);
        messageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        messageDto.setDeviceToId(this.deviceFromId);
        EncryptedContentDto encryptedDto = new EncryptedContentDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setOperation(operation);
        encryptedDto.setDeviceFromId(App.getInstance().getConnectedDevice().getId());
        encryptedDto.setMessage(message);
        if(cmsMessage != null) encryptedDto.setCMSMessage(cmsMessage.toPEMStr());
        encryptMessage(messageDto, encryptedDto, socketSession.getDevice());
        messageDto.setUUID(UUID);
        return messageDto;
    }

    public SocketMessageDto getPlainResponse(Integer statusCode, String message,
                    OperationType operation) throws Exception {
        WebSocketSession socketSession = App.getInstance().getWSSession(UUID);
        socketSession.setOperationType(operation);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(OperationType.MSG_TO_DEVICE);
        messageDto.setMessageType(operation);
        if(qrMessage != null) messageDto.setOperationCode(qrMessage.getOperationCode());
        messageDto.setStatusCode(statusCode);
        messageDto.setDeviceToId(this.deviceFromId);
        messageDto.setDeviceFromId(App.getInstance().getConnectedDevice().getId());
        messageDto.setMessage(message);
        messageDto.setUUID(UUID);
        return messageDto;
    }

    public static SocketMessageDto INIT_SIGNED_SESSION_REQUEST() throws NoSuchAlgorithmException {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(OperationType.INIT_SIGNED_SESSION);
        messageDto.setDeviceId(PrefUtils.getDeviceId());
        messageDto.setUUID(java.util.UUID.randomUUID().toString());
        return messageDto;
    }

    //message to server
    public static SocketMessageDto INIT_REMOTE_SIGNED_SESSION_REQUEST(
            CMSSignedMessage cmsSignedMessage) throws Exception {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(OperationType.INIT_REMOTE_SIGNED_SESSION);
        messageDto.setCMS(cmsSignedMessage);
        return messageDto;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }

    public OperationType getMessageType() {
        return messageType;
    }

    public SocketMessageDto setMessageType(OperationType messageType) {
        this.messageType = messageType;
        return this;
    }

    public OperationType getStep() {
        return step;
    }

    public SocketMessageDto setStep(OperationType step) {
        this.step = step;
        return this;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public List<CurrencyDto> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<CurrencyDto> currencyList) {
        this.currencyList = currencyList;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public DeviceDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @JsonIgnore public boolean isEncrypted() {
        return encryptedMessage != null;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public Long getDeviceToId() {
        return deviceToId;
    }

    public void setDeviceToId(Long deviceToId) {
        this.deviceToId = deviceToId;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
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

    public <T> T getMessage(Class<T> type) throws Exception {
        return JSON.getMapper().readValue(message, type);
    }

    public <T> T getMessage(TypeReference type) throws Exception {
        return JSON.getMapper().readValue(message, type);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @JsonIgnore
    public CMSSignedMessage getCMS() throws Exception {
        if(cms == null) cms = CMSSignedMessage.FROM_PEM(cmsMessagePEM);
        return cms;
    }

    public SocketMessageDto setCMS(CMSSignedMessage cmsMessage) throws Exception {
        this.cms = cmsMessage;
        this.cmsMessagePEM = cmsMessage.toPEMStr();
        return this;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyList != null) currencySet = CurrencyDto.deSerializeCollection(currencyList);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }


    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(String encryptedCMSPEM) {
        this.encryptedMessage = encryptedCMSPEM;
    }

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
    }

    public QRMessageDto getQrMessage() {
        return qrMessage;
    }

    public void setQrMessage(QRMessageDto qrMessage) {
        this.qrMessage = qrMessage;
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public static SocketMessageDto getQRInfoRequest(QRMessageDto qrMessage,
                                                    boolean encrypted) throws Exception {
        DeviceDto device = qrMessage.getDevice();
        WebSocketSession socketSession = checkWebSocketSession(device, null,
                qrMessage.getOperation());
        socketSession.setAesParams(qrMessage.getAesParams());
        qrMessage.setUUID(socketSession.getUUID()).createRequest();
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(OperationType.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToId(device.getId());
        if(encrypted) {
            EncryptedContentDto encryptedDto = EncryptedContentDto.getQRInfoRequest(qrMessage)
                    .setUUID(socketSession.getUUID());
            encryptedDto.setDeviceToName(device.getDeviceName());
            encryptedDto.setDeviceFromId(App.getInstance().getConnectedDevice().getId());
            encryptMessage(socketMessageDto, encryptedDto, device);
        } else {
            socketMessageDto.setOperationCode(qrMessage.getOperationCode());
            socketMessageDto.setMessageType(qrMessage.getOperation());
            socketMessageDto.setDeviceFromId(App.getInstance().getConnectedDevice().getId());
            socketMessageDto.setUUID(socketSession.getUUID());
        }
        socketSession.setLastMessage(socketMessageDto);
        socketSession.setQrMessage(qrMessage);
        return socketMessageDto;
    }

    private static void encryptMessage(SocketMessageDto socketMessageDto,
               EncryptedContentDto encryptedDto, DeviceDto device) throws Exception {
        if(device.getX509Certificate() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.writeValueAsBytes(encryptedDto), device.getX509Certificate());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else if(device.getPublicKey() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.writeValueAsBytes(encryptedDto), device.getPublicKey());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else LOGD(TAG, "target device without public key data");
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(DeviceDto device,
                              List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(device, currencyList, OperationType.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(OperationType.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(device.getId());
        socketMessageDto.setDeviceToName(device.getDeviceName());
        EncryptedContentDto encryptedDto = EncryptedContentDto
                .getCurrencyWalletChangeRequest(currencyList);
        encryptMessage(socketMessageDto, encryptedDto, device);
        return socketMessageDto;
    }

    public SocketMessageDto getBanResponse(Context context) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(OperationType.WEB_SOCKET_BAN_SESSION);
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageVSToDevice(DeviceDto device, String toUser,
                                        String textToEncrypt, String broadCastId) throws Exception {
        UserDto user = App.getInstance().getUser();
        WebSocketSession socketSession = checkWebSocketSession(device, null,
                OperationType.MESSAGEVS);
        socketSession.setBroadCastId(broadCastId);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(OperationType.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToId(device.getId());
        socketMessageDto.setDeviceToName(device.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getMessageVSToDevice(
                user, toUser, textToEncrypt);
        encryptMessage(socketMessageDto, encryptedDto, device);
        return socketMessageDto;
    }

    public void decryptMessage() throws Exception {
        byte[] decryptedBytes = App.getInstance().decryptMessage(encryptedMessage.getBytes());
        EncryptedContentDto encryptedDto =
                JSON.readValue(decryptedBytes, EncryptedContentDto.class);
        if(encryptedDto.getOperation() != null) {
            this.messageType = operation;
            operation = encryptedDto.getOperation();
        }
        if(encryptedDto.getOperationCode() != null) operationCode = encryptedDto.getOperationCode();
        if(encryptedDto.getStep() != null) step = encryptedDto.getStep();
        if(encryptedDto.getStatusCode() != null) statusCode = encryptedDto.getStatusCode();
        if(encryptedDto.getDeviceFromName() != null) deviceFromName = encryptedDto.getDeviceFromName();
        if(encryptedDto.getFrom() != null) from = encryptedDto.getFrom();
        if(encryptedDto.getDeviceFromId() != null) deviceFromId = encryptedDto.getDeviceFromId();
        if(encryptedDto.getCMSMessage() != null) cms = encryptedDto.getCMS();
        if(encryptedDto.getCurrencyList() != null) currencySet = CurrencyDto.deSerializeCollection(
                encryptedDto.getCurrencyList());
        if(encryptedDto.getSubject() != null) subject = encryptedDto.getSubject();
        if(encryptedDto.getMessage() != null) message = encryptedDto.getMessage();
        if(encryptedDto.getToUser() != null) toUser = encryptedDto.getToUser();
        if(encryptedDto.getDeviceToName() != null) deviceToName = encryptedDto.getDeviceToName();
        if(encryptedDto.getURL()!= null) URL = encryptedDto.getURL();
        if(encryptedDto.getLocale() != null) locale = encryptedDto.getLocale();
        if(encryptedDto.getAesParams() != null) aesParams = encryptedDto.getAesParams();
        if(encryptedDto.getX509CertificatePEM() != null) x509CertificatePEM = encryptedDto.getX509CertificatePEM();
        if(encryptedDto.getPublicKeyPEM() != null) publicKeyPEM = encryptedDto.getPublicKeyPEM();
        if(encryptedDto.getUUID() != null) UUID = encryptedDto.getUUID();
        timeLimited = encryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceDto device, T data, OperationType operationType)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(device != null) webSocketSession = App.getInstance().getWSSession(device.getId());
        if(webSocketSession == null && device != null) {
            webSocketSession = new WebSocketSession(device).setUUID(
                    java.util.UUID.randomUUID().toString());
        }
        App.getInstance().putWSSession(webSocketSession.getUUID(), webSocketSession);
        webSocketSession.setData(data);
        webSocketSession.setOperationType(operationType);
        return webSocketSession;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(cms != null) s.writeObject(cms.getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] receiptBytes = (byte[]) s.readObject();
        if(receiptBytes != null) cms = new CMSSignedMessage(receiptBytes);
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + " - statusCode:" + statusCode + " - " +
                getOperation() + " - " + UUID;
    }

}