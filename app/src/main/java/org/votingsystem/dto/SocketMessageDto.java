package org.votingsystem.dto;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;
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

import javax.websocket.Session;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = SocketMessageDto.class.getSimpleName();

    public String getPemPublicKey() {
        return pemPublicKey;
    }

    public void setPemPublicKey(String pemPublicKey) {
        this.pemPublicKey = pemPublicKey;
    }

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}

    private TypeVS operation;
    private TypeVS messageType;
    private TypeVS messageSubType;
    private State state = State.PENDING;
    private Integer statusCode;
    private Long deviceFromId;
    private Long deviceToId;
    private String sessionId;
    private String subject;
    private String message;
    private String textToSign;
    private String encryptedMessage;
    private String UUID;
    private String locale = Locale.getDefault().getLanguage().toLowerCase();
    private String remoteAddress;
    private String cmsMessagePEM;
    private String pemCert;
    private String pemPublicKey;
    private String from;
    private String caption;
    private String deviceId;
    private boolean timeLimited;
    private String deviceFromName;
    private String deviceToName;
    private String toUser;
    private String URL;
    private DeviceDto connectedDevice;
    private List<CurrencyDto> currencyList;
    private Date date;


    @JsonIgnore private UserDto user;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private Session session;
    @JsonIgnore private transient CMSSignedMessage cms;

    public SocketMessageDto () {}

    public SocketMessageDto(Integer statusCode, String message, TypeVS operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message,
                                        CMSSignedMessage cmsMessage, TypeVS operation) throws Exception {
        WebSocketSession socketSession = AppVS.getInstance().getWSSession(UUID);
        socketSession.setTypeVS(operation);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_SESSION_ID);
        messageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        messageDto.setSessionId(sessionId);
        EncryptedContentDto encryptedDto = new EncryptedContentDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setOperation(operation);
        encryptedDto.setDeviceFromId(AppVS.getInstance().getConnectedDevice().getId());
        encryptedDto.setMessage(message);
        if(cmsMessage != null) encryptedDto.setCMSMessage(cmsMessage.toPEMStr());
        encryptMessage(messageDto, encryptedDto, socketSession.getDevice());
        messageDto.setUUID(UUID);
        return messageDto;
    }

    public static SocketMessageDto INIT_SIGNED_SESSION_REQUEST() throws NoSuchAlgorithmException {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(TypeVS.INIT_SIGNED_SESSION);
        messageDto.setDeviceId(PrefUtils.getDeviceId());
        messageDto.setUUID(java.util.UUID.randomUUID().toString());
        return messageDto;
    }

    public static SocketMessageDto INIT_REMOTE_SESSION_REQUEST_BY_TARGET_SESSION_ID(String sessionId)
            throws NoSuchAlgorithmException {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(TypeVS.INIT_REMOTE_SIGNED_SESSION);
        messageDto.setSessionId(sessionId);
        messageDto.setUUID(java.util.UUID.randomUUID().toString());
        return messageDto;
    }

    public TypeVS getMessageType() {
        return messageType;
    }

    public SocketMessageDto setMessageType(TypeVS messageType) {
        this.messageType = messageType;
        return this;
    }

    public TypeVS getMessageSubType() {
        return messageSubType;
    }

    public SocketMessageDto setMessageSubType(TypeVS messageSubType) {
        this.messageSubType = messageSubType;
        return this;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Session getSession() {
        return session;
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

    public <S> S getMessage(Class<S> type) throws Exception {
        return JSON.readValue(message, type);
    }

    public <T> T getMessage(TypeReference<T> type) throws Exception {
        return JSON.readValue(message, type);
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

    public String getCMSMessage() {
        return cmsMessagePEM;
    }

    public void setCMSMessage(String cmsMessage) {
        this.cmsMessagePEM = cmsMessage;
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(boolean timeLimited) {
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

    public void setSession(Session session) {
        this.session = session;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(String encryptedCMSPEM) {
        this.encryptedMessage = encryptedCMSPEM;
    }

    public String getPemCert() {
        return pemCert;
    }

    public void setPemCert(String pemCert) {
        this.pemCert = pemCert;
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public static SocketMessageDto getSignRequest(DeviceDto device, String toUser,
                              String textToSign, String subject) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(device, null, TypeVS.MESSAGEVS_SIGN);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(device.getId());
        socketMessageDto.setDeviceToName(device.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getSignRequest(
                toUser, textToSign, subject);
        encryptMessage(socketMessageDto, encryptedDto, device);
        return socketMessageDto;
    }

    public static SocketMessageDto getQRInfoRequestByTargetDeviceId(
            DeviceDto device, QRMessageDto qrMessageDto) throws Exception {
        qrMessageDto.createRequest();
        WebSocketSession socketSession = checkWebSocketSession(device, null, TypeVS.QR_MESSAGE_INFO);
        socketSession.setData(qrMessageDto);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setDeviceFromId(AppVS.getInstance().getConnectedDevice().getId());
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setMessage(qrMessageDto.getOperationId());
        socketMessageDto.setMessageType(qrMessageDto.getOperation());
        socketMessageDto.setDeviceToId(device.getId());
        socketMessageDto.setDeviceToName(device.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getQRInfoRequest(
                qrMessageDto);
        encryptMessage(socketMessageDto, encryptedDto, device);
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

    public static SocketMessageDto getPlainQRInfoRequestByTargetSessionId(String sessionId) throws Exception {
        DeviceDto deviceDto = new DeviceDto().setSessionId(sessionId);
        WebSocketSession socketSession = checkWebSocketSession(deviceDto, null, TypeVS.QR_MESSAGE_INFO);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_SESSION_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setSessionId(sessionId);
        socketMessageDto.setUUID(socketSession.getUUID());
        return socketMessageDto;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(DeviceDto device,
                              List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(device, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
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
        socketMessageDto.setOperation(TypeVS.WEB_SOCKET_BAN_SESSION);
        socketMessageDto.setSessionId(sessionId);
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageVSToDevice(DeviceDto device, String toUser,
                                        String textToEncrypt, String broadCastId) throws Exception {
        UserDto user = AppVS.getInstance().getUser();
        WebSocketSession socketSession = checkWebSocketSession(device, null,
                TypeVS.MESSAGEVS);
        socketSession.setBroadCastId(broadCastId);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(device.getId());
        socketMessageDto.setDeviceToName(device.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getMessageVSToDevice(
                user, toUser, textToEncrypt);
        encryptMessage(socketMessageDto, encryptedDto, device);
        return socketMessageDto;
    }

    public void decryptMessage() throws Exception {
        byte[] decryptedBytes = AppVS.getInstance().decryptMessage(encryptedMessage.getBytes());
        EncryptedContentDto encryptedDto =
                JSON.readValue(decryptedBytes, EncryptedContentDto.class);
        if(encryptedDto.getOperation() != null) {
            this.messageType = operation;
            operation = encryptedDto.getOperation();
        }
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
        if(encryptedDto.getTextToSign() != null) textToSign = encryptedDto.getTextToSign();
        if(encryptedDto.getLocale() != null) locale = encryptedDto.getLocale();
        if(encryptedDto.getPemCert() != null) pemCert = encryptedDto.getPemCert();
        if(encryptedDto.getPemPublicKey() != null) pemPublicKey = encryptedDto.getPemPublicKey();
        timeLimited = encryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceDto device, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(device != null) webSocketSession = AppVS.getInstance().getWSSession(device.getId());
        if(webSocketSession == null && device != null) {
            webSocketSession = new WebSocketSession(device).setUUID(
                    java.util.UUID.randomUUID().toString());
        }
        AppVS.getInstance().putWSSession(webSocketSession.getUUID(), webSocketSession);
        webSocketSession.setData(data);
        webSocketSession.setTypeVS(typeVS);
        return webSocketSession;
    }


    public ResponseVS getNotificationResponse(Context context) {
        ResponseVS responseVS = new ResponseVS(statusCode, message);
        switch (operation) {
            case MESSAGEVS_SIGN_RESPONSE:
                if(ResponseVS.SC_WS_MESSAGE_SEND_OK == statusCode) {
                    responseVS.setCaption(context.getString(R.string.sign_document_lbl));
                    responseVS.setMessage(context.getString(R.string.sign_document_result_ok_msg));
                }
                break;
        }
        return responseVS;
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