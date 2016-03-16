package org.votingsystem.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.AppVS;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Currency;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.Utils;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedContentDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private TypeVS operation;
    private TypeVS step;
    private Integer statusCode;
    private String subject;
    private String locale = Locale.getDefault().getLanguage().toLowerCase();
    private String message;
    private String from;
    private String deviceFromName;
    private Long deviceFromId;
    private String contentToSign;
    private String sessionId;
    private String toUser;
    private String deviceToName;
    private String hashCertVS;
    private String cmsMessage;
    private String x509CertificatePEM;
    private String publicKeyPEM;
    private boolean timeLimited = false;
    private Set<CurrencyDto> currencyList;
    private String URL;

    public EncryptedContentDto() { }

    public EncryptedContentDto(TypeVS operation, Integer statusCode, String message, String URL) {
        this.operation = operation;
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static EncryptedContentDto getSignRequest(String toUser,
                                                     String textToSign, String subject) throws Exception {
        EncryptedContentDto messageContentDto =  new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS_SIGN);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setToUser(toUser);
        messageContentDto.setContentToSign(textToSign);
        messageContentDto.setSubject(subject);
        return messageContentDto;
    }

    public static EncryptedContentDto getQRInfoRequest(QRMessageDto qrMessageDto) throws Exception {
        EncryptedContentDto messageContentDto =  new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.QR_MESSAGE_INFO);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setHashCertVS(qrMessageDto.getHashCertVS());
        messageContentDto.setX509CertificatePEM(
                new String(PEMUtils.getPEMEncoded(AppVS.getInstance().getX509UserCert())));
        messageContentDto.setMessage(qrMessageDto.getUUID());
        return messageContentDto;
    }

    public static EncryptedContentDto getCurrencyWalletChangeRequest(
            Collection<Currency> currencyList) throws Exception {
        EncryptedContentDto messageContentDto = new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.CURRENCY_WALLET_CHANGE);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setDeviceFromId(AppVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setCurrencyList(CurrencyDto.serializeCollection(currencyList));
        return messageContentDto;
    }

    public static EncryptedContentDto getMessageVSToDevice(
            UserDto user, String toUser, String message) throws Exception {
        EncryptedContentDto messageContentDto = new EncryptedContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS);
        messageContentDto.setFrom(user.getFullName());
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setDeviceFromId(AppVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setToUser(toUser);
        messageContentDto.setMessage(message);
        return messageContentDto;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
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

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public String getContentToSign() {
        return contentToSign;
    }

    public void setContentToSign(String contentToSign) {
        this.contentToSign = contentToSign;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public Set<CurrencyDto> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(Set<CurrencyDto> currencyList) {
        this.currencyList = currencyList;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getCMSMessage() {
        return cmsMessage;
    }

    public void setCMSMessage(String cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    @JsonIgnore
    CMSSignedMessage getCMS () throws Exception {
        byte[] cmsMessageBytes = Base64.decode(cmsMessage.getBytes(), Base64.NO_WRAP);
        return new CMSSignedMessage(cmsMessageBytes);
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public TypeVS getStep() {
        return step;
    }

    public void setStep(TypeVS step) {
        this.step = step;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }
}