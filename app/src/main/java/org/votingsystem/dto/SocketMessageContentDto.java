package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.AppVS;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import javax.mail.MessagingException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageContentDto implements Serializable {

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
    private String textToSign;
    private String toUser;
    private String deviceToName;
    private String hashCertVS;
    private String smimeMessage;
    private Set<CurrencyDto> currencyList;
    private String URL;

    public SocketMessageContentDto() { }

    public SocketMessageContentDto(TypeVS operation, Integer statusCode, String message, String URL) {
        this.operation = operation;
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static SocketMessageContentDto getSignRequest(String toUser,
            String textToSign, String subject) throws Exception {
        SocketMessageContentDto messageContentDto =  new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS_SIGN);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setToUser(toUser);
        messageContentDto.setTextToSign(textToSign);
        messageContentDto.setSubject(subject);
        return messageContentDto;
    }

    public static SocketMessageContentDto getQRInfoRequest(QRMessageDto qrMessageDto) throws Exception {
        SocketMessageContentDto messageContentDto =  new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.QR_MESSAGE_INFO);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setHashCertVS(qrMessageDto.getHashCertVS());
        messageContentDto.setMessage(qrMessageDto.getUUID());
        return messageContentDto;
    }

    public static SocketMessageContentDto getSignResponse(Integer statusCode, String message,
              SMIMEMessage smimeMessage) throws IOException, MessagingException {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS_SIGN_RESPONSE);
        messageContentDto.setStatusCode(statusCode);
        messageContentDto.setMessage(message);
        messageContentDto.setSmimeMessage(new String(Base64.encode(smimeMessage.getBytes())));
        return messageContentDto;
    }

    public static SocketMessageContentDto getCurrencyWalletChangeRequest(
            Collection<Currency> currencyList) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.CURRENCY_WALLET_CHANGE);
        messageContentDto.setDeviceFromName(Utils.getDeviceName());
        messageContentDto.setDeviceFromId(AppVS.getInstance().getConnectedDevice().getId());
        messageContentDto.setCurrencyList(CurrencyDto.serializeCollection(currencyList));
        return messageContentDto;
    }

    public static SocketMessageContentDto getMessageVSToDevice(
            UserVSDto userVS, String toUser, String message) throws Exception {
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setOperation(TypeVS.MESSAGEVS);
        messageContentDto.setFrom(userVS.getFullName());
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

    public String getTextToSign() {
        return textToSign;
    }

    public void setTextToSign(String textToSign) {
        this.textToSign = textToSign;
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

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    @JsonIgnore
    SMIMEMessage getSMIME () throws Exception {
        byte[] smimeMessageBytes = Base64.decode(smimeMessage.getBytes());
        return new SMIMEMessage(smimeMessageBytes);
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

}
