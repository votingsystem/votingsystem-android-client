package org.votingsystem.dto;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.SessionVS;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.websocket.Session;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Base64.DEFAULT -> problems with Java 8 Base64
 *
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto {

    public static final String TAG = SocketMessageDto.class.getSimpleName();

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private TypeVS operation;
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
    private String smimeMessage;
    private String aesParams;
    private String from;
    private String caption;
    private String deviceId;
    private boolean timeLimited = false;
    private String deviceFromName;
    private String deviceToName;
    private String toUser;
    private String URL;
    private DeviceVSDto connectedDevice;
    private List<CurrencyDto> currencyDtoList;
    private Date date;

    private SocketMessageContentDto content;


    @JsonIgnore private UserVSDto userVS;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private AESParams aesEncryptParams;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private Session session;
    @JsonIgnore private SessionVS sessionVS;
    @JsonIgnore private SMIMEMessage smime;

    public SocketMessageDto () {}

    public SocketMessageDto(Integer statusCode, String message, TypeVS operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message,
                                TypeVS operation) throws Exception {
        WebSocketSession socketSession = AppVS.getInstance().getWSSession(UUID);
        socketSession.setTypeVS(operation);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(TypeVS.MESSAGEVS_FROM_DEVICE);
        messageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        messageDto.setSessionId(sessionId);
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setStatusCode(statusCode);
        messageContentDto.setMessage(message);
        messageContentDto.setOperation(operation);
        messageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), socketSession.getAESParams()));
        messageDto.setUUID(UUID);
        return messageDto;
    }

    public SocketMessageDto getSignResponse(Integer statusCode, String message,
                    SMIMEMessage smimeMessage) throws Exception {
        WebSocketSession socketSession = AppVS.getInstance().getWSSession(UUID);
        socketSession.setTypeVS(TypeVS.MESSAGEVS_SIGN_RESPONSE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_FROM_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setSessionId(sessionId);
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getSignResponse(
                statusCode, message, smimeMessage);
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper()
                .writeValueAsString(messageContentDto), socketSession.getAESParams()));
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public static SocketMessageDto INIT_SESSION_REQUEST() throws NoSuchAlgorithmException {
        WebSocketSession socketSession = checkWebSocketSession(null, null, TypeVS.INIT_SIGNED_SESSION);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(socketSession.getTypeVS());
        messageDto.setDeviceId(PrefUtils.getApplicationId());
        messageDto.setUUID(socketSession.getUUID());
        return messageDto;
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

    public List<CurrencyDto> getCurrencyDtoList() {
        return currencyDtoList;
    }

    public void setCurrencyDtoList(List<CurrencyDto> currencyDtoList) {
        this.currencyDtoList = currencyDtoList;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public DeviceVSDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceVSDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isEncrypted() {
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

    public SessionVS getSessionVS() {
        return sessionVS;
    }

    public void setSessionVS(SessionVS sessionVS) {
        this.sessionVS = sessionVS;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session, SessionVS sessionVS) throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        /*if(TypeVS.MESSAGEVS_SIGN == operation && deviceId == null) {
            throw new ValidationExceptionVS("missing message 'deviceId'");
        }*/
        /*this.remoteAddress = ((String)((AbstractServletOutputStream)((WsRemoteEndpointImplServer)((WsRemoteEndpointAsync)
                ((WsSession)session).remoteEndpointAsync).base).sos).socketWrapper.getRemoteAddr());*/
        this.session = session;
        this.sessionVS = sessionVS;
        if(sessionId == null) sessionId = session.getId();
        //Locale.forLanguageTag(locale)
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

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    @JsonIgnore
    public SMIMEMessage getSMIME() throws Exception {
        if(smime == null) smime = new SMIMEMessage(new ByteArrayInputStream(Base64.decode(smimeMessage)));
        return smime;
    }

    public SocketMessageDto setSMIME(SMIMEMessage smimeMessage) throws Exception {
        this.smime = smimeMessage;
        this.smimeMessage = new String(Base64.encode(smimeMessage.getBytes()));
        return this;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SocketMessageContentDto getContent() {
        return content;
    }

    public void setContent(SocketMessageContentDto content) {
        this.content = content;
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

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyDtoList != null) currencySet = CurrencyDto.deSerializeCollection(currencyDtoList);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public String getAesParams() {
        return aesParams;
    }

    public void setAesParams(String aesParams) {
        this.aesParams = aesParams;
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

    public void setEncryptedMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public AESParams getAesEncryptParams() {
        return aesEncryptParams;
    }

    public void setAesEncryptParams(AESParams aesEncryptParams) {
        this.aesEncryptParams = aesEncryptParams;
    }

    public static SocketMessageDto getSignRequest(DeviceVSDto deviceVS, String toUser,
                  String textToSign, String subject) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null, TypeVS.MESSAGEVS_SIGN);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceVS.getId());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getSignRequest(
                deviceVS, toUser, textToSign, subject);
        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                aesParams.getBytes(), deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper().writeValueAsString(messageContentDto),
                socketSession.getAESParams()));
        return socketMessageDto;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(DeviceVSDto deviceVS,
                              List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(deviceVS.getId());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto
                .getCurrencyWalletChangeRequest(currencyList);

        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] encryptedAESDataRequestBytes = Encryptor.encryptToCMS(aesParams.getBytes(),
                deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(Base64.encode(encryptedAESDataRequestBytes)));

        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper()
                .writeValueAsString(messageContentDto), socketSession.getAESParams()));
        return socketMessageDto;
    }

    public SocketMessageDto getBanResponse(Context context) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.WEB_SOCKET_BAN_SESSION);
        socketMessageDto.setSessionId(sessionId);
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageVSToDevice(DeviceVSDto deviceVS, String toUser,
                            String textToEncrypt) throws Exception {
        UserVSDto userVS = AppVS.getInstance().getUserVS();
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null,
                TypeVS.MESSAGEVS);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceVS.getId());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getMessageVSToDevice(
                userVS, toUser, textToEncrypt);
        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] encryptedAESDataRequestBytes = Encryptor.encryptToCMS(aesParams.getBytes(),
                deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(Base64.encode(encryptedAESDataRequestBytes)));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), socketSession.getAESParams()));
        return socketMessageDto;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(aesParams.getBytes(), privateKey);
        AESParamsDto aesDto = JSON.getMapper().readValue(new String(decryptedBytes), AESParamsDto.class);
        this.aesEncryptParams = AESParams.load(aesDto);
        decryptMessage(this.aesEncryptParams);
    }


    public void decryptMessage(AESParams aesParams) throws Exception {
        this.aesEncryptParams = aesParams;
        content = JSON.getMapper().readValue(Encryptor.decryptAES(encryptedMessage, aesParams),
                SocketMessageContentDto.class);
        if(content.getOperation() != null) operation = content.getOperation();
        if(content.getStatusCode() != null) statusCode = content.getStatusCode();
        if(content.getDeviceFromName() != null) deviceFromName = content.getDeviceFromName();
        if(content.getFrom() != null) from = content.getFrom();
        if(content.getDeviceFromId() != null) deviceFromId = content.getDeviceFromId();
        if(content.getSmimeMessage() != null) smime = content.getSMIME();
        if(content.getCurrencyList() != null) currencySet = CurrencyDto.deSerializeCollection(
                content.getCurrencyList());
        if(content.getSubject() != null) subject = content.getSubject();
        if(content.getMessage() != null) message = content.getMessage();
        if(content.getToUser() != null) toUser = content.getToUser();
        if(content.getDeviceToName() != null) deviceToName = content.getDeviceToName();
        if(content.getURL()!= null) URL = content.getURL();
        if(content.getTextToSign() != null) textToSign = content.getTextToSign();
        if(content.getLocale() != null) locale = content.getLocale();
        this.encryptedMessage = null;
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceVSDto deviceVS, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(deviceVS != null) webSocketSession = AppVS.getInstance().getWSSession(deviceVS.getId());
        if(webSocketSession == null) {
            AESParams aesParams = new AESParams();
            webSocketSession = new WebSocketSession(aesParams, deviceVS);
            AppVS.getInstance().putWSSession(java.util.UUID.randomUUID().toString(), webSocketSession);
        }
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

    @Override public String toString() {
        return this.getClass().getSimpleName() + " - statusCode:" + statusCode + " - " +
                getOperation() + " - " + UUID;
    }

}