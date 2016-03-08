package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.CertificationRequestVS;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private TypeVS operation = TypeVS.CURRENCY_SEND;
    private Long id;
    private BigDecimal amount;
    private BigDecimal batchAmount;
    private Currency.State state;
    private String currencyCode;
    private String currencyServerURL;
    private String hashCertVS;
    private String subject;
    private String toUserIBAN;
    private String toUserName;
    private String tag;
    private Boolean timeLimited;
    private String batchUUID;
    private String object;
    private Date notBefore;
    private Date notAfter;
    private Date dateCreated;

    @JsonIgnore private PKCS10CertificationRequest csrPKCS10;

    public CurrencyDto() {}

    public CurrencyDto(Currency currency) {
        this.id = currency.getId();
        this.hashCertVS = currency.getHashCertVS();
        this.amount = currency.getAmount();
        this.currencyCode = currency.getCurrencyCode();
        this.tag = currency.getTag();
        this.timeLimited = currency.isTimeLimited();
        this.dateCreated = currency.getDateCreated();
        this.notBefore = currency.getValidFrom();
        this.notAfter = currency.getValidTo();
    }

    public CurrencyDto(PKCS10CertificationRequest csrPKCS10) throws Exception {
        this.csrPKCS10 = csrPKCS10;
        CertificationRequestInfo info = csrPKCS10.getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                csrPKCS10, ContextVS.CURRENCY_TAG);
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        hashCertVS = certExtensionDto.getHashCertVS();
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        tag = certExtensionDto.getTag();
        CurrencyDto certSubjectDto = getCertSubjectDto(subjectDN, hashCertVS);
        if(!certSubjectDto.getCurrencyServerURL().equals(currencyServerURL))
            throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(tag)) throw new ValidationExceptionVS("tag: " + tag + " - certSubject: " + subjectDN);
    }

    public CurrencyDto(Boolean timeLimited, String object) {
        this.timeLimited = timeLimited;
        this.object = object;
    }

    public static CurrencyDto BATCH_ITEM(CurrencyBatchDto currencyBatchDto, Currency currency) throws ValidationExceptionVS {
        if(!currencyBatchDto.getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                "CurrencyBatch currencyCode: " + currencyBatchDto.getCurrencyCode()
                        + " - Currency currencyCode: " + currency.getCurrencyCode());
        if(currency.isTimeLimited() && !currencyBatchDto.getTimeLimited()) throw new ValidationExceptionVS(
                "TimeLimited currency cannot go inside NOT TimeLimited CurrencyBatch");
        if(!TagVSDto.WILDTAG.equals(currency.getTag()) && !currency.getTag().equals(
                currencyBatchDto.getTag())) throw new ValidationExceptionVS(MessageFormat.format(
                "''{0}'' Currency  cannot go inside ''{1}'' CurrencyBatch", currency.getTag(),
                currencyBatchDto.getTag()));
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyDto.subject = currencyBatchDto.getSubject();
        currencyDto.toUserIBAN = currencyBatchDto.getToUserIBAN();
        currencyDto.toUserName = currencyBatchDto.getToUserName();
        currencyDto.batchAmount = currencyBatchDto.getBatchAmount();
        currencyDto.batchUUID = currencyBatchDto.getBatchUUID();
        return currencyDto;
    }

    public static CurrencyDto serialize(Currency currency) throws Exception {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setAmount(currency.getAmount());
        currencyDto.setCurrencyCode(currency.getCurrencyCode());
        currencyDto.setHashCertVS(currency.getHashCertVS());
        currencyDto.setTag(currency.getTag());
        currencyDto.setState(currency.getState());
        currencyDto.setTimeLimited(currency.isTimeLimited());
        //CertificationRequestVS instead of Currency to make it easier deserialization on JavaFX
        currencyDto.setObject(ObjectUtils.serializeObjectToString(currency.getCertificationRequest()));
        return currencyDto;
    }

    public static Set<CurrencyDto> serializeCollection(Collection<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> result = new HashSet<>();
        for(Currency currency : currencyCollection) {
            result.add(CurrencyDto.serialize(currency));
        }
        return result;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Currency deSerialize() throws Exception {
        try {
            CertificationRequestVS certificationRequestVS =
                    (CertificationRequestVS) ObjectUtils.deSerializeObject(object.getBytes());
            Currency currency = Currency.fromCertificationRequestVS(certificationRequestVS);
            currency.setState(state);
            return currency;
        }catch (Exception ex) {
            return (Currency) ObjectUtils.deSerializeObject(object.getBytes());
        }
    }

    public static Set<Currency> deSerializeCollection(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(Collection<CurrencyDto> currencyDtoCollection) throws Exception {
        Set<Currency> currencySet = new HashSet<>();
        for(CurrencyDto currencyDto : currencyDtoCollection) {
            currencySet.add(currencyDto.deSerialize());
        }
        return currencySet;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public PKCS10CertificationRequest getCsrPKCS10() {
        return csrPKCS10;
    }

    public void setCsrPKCS10(PKCS10CertificationRequest csrPKCS10) {
        this.csrPKCS10 = csrPKCS10;
    }

    public static CurrencyDto getCertSubjectDto(String subjectDN, String hashCertVS) {
        CurrencyDto currencyDto = new CurrencyDto();
        if (subjectDN.contains("CURRENCY_CODE:"))
            currencyDto.setCurrencyCode(subjectDN.split("CURRENCY_CODE:")[1].split(",")[0]);
        if (subjectDN.contains("CURRENCY_VALUE:"))
            currencyDto.setAmount(new BigDecimal(subjectDN.split("CURRENCY_VALUE:")[1].split(",")[0]));
        if (subjectDN.contains("TAG:")) currencyDto.setTag(subjectDN.split("TAG:")[1].split(",")[0]);
        if (subjectDN.contains("currencyServerURL:"))
            currencyDto.setCurrencyServerURL(subjectDN.split("currencyServerURL:")[1].split(",")[0]);
        currencyDto.setHashCertVS(hashCertVS);
        return currencyDto;
    }

    public Currency.State getState() {
        return state;
    }

    public void setState(Currency.State state) {
        this.state = state;
    }
}
