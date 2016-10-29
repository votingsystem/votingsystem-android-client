package org.votingsystem.dto.currency;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.Currency;
import org.votingsystem.model.CurrencyBatch;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.crypto.CertUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperationType operation = OperationType.CURRENCY_SEND;
    private Set<String> currencySet;
    private String leftOverCSR;
    private String currencyChangeCSR;
    private String toUserIBAN;
    private String toUserName;
    private String subject;
    private String currencyCode;
    private String tag;
    private String batchUUID;
    private Boolean timeLimited = Boolean.FALSE;
    private BigDecimal batchAmount;
    private BigDecimal leftOver = BigDecimal.ZERO;
    @JsonIgnore private Currency leftOverCurrency;
    @JsonIgnore private Collection<Currency> currencyCollection;


    public CurrencyBatchDto() {}


    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUser().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.tag = currencyBatch.getTag();
        this.timeLimited = currencyBatch.isTimeLimited();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    @JsonIgnore
    public void checkCurrencyData(Currency currency) throws ExceptionVS {
        String currencyData = "Currency with hash '" + currency.getRevocationHash() + "' ";
        if(getOperation() != currency.getOperation()) throw new ValidationExceptionVS(
                currencyData + "expected operation " + getOperation() + " found " + currency.getOperation());
        if(!getSubject().equals(currency.getSubject())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + getSubject() + " found " + currency.getSubject());
        if(!getToUserIBAN().equals(currency.getToUserIBAN())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + getToUserIBAN() + " found " + currency.getToUserIBAN());
        if(getBatchAmount().compareTo(currency.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                currencyData + "expected batchAmount " + getBatchAmount().toString() + " found " + currency.getBatchAmount().toString());
        if(!getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                currencyData + "expected currencyCode " + getCurrencyCode() + " found " + currency.getCurrencyCode());
        if(!getTag().equals(currency.getTag())) throw new ValidationExceptionVS(
                currencyData + "expected tag " + getTag() + " found " + currency.getTag());
        if(!getBatchUUID().equals(currency.getBatchUUID())) throw new ValidationExceptionVS(
                currencyData + "expected batchUUID " + getBatchUUID() + " found " + currency.getBatchUUID());
    }

    @JsonIgnore
    public CMSSignedMessage validateResponse(CurrencyBatchResponseDto responseDto,
                                             Set<TrustAnchor> trustAnchor) throws Exception {
        CMSSignedMessage receipt = new CMSSignedMessage(Base64.decode(responseDto.getReceipt().getBytes(), Base64.NO_WRAP));
        receipt.isValidSignature();
        CertUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(receipt.getSignersCerts()));
        if(responseDto.getLeftOverCert() != null) {
            leftOverCurrency.initSigner(responseDto.getLeftOverCert().getBytes());
            leftOverCurrency.setState(Currency.State.OK);
        }
        CurrencyBatchDto signedDto = receipt.getSignedContent(CurrencyBatchDto.class);
        if(signedDto.getBatchAmount().compareTo(batchAmount) != 0) throw new ValidationExceptionVS(MessageFormat.format(
                "ERROR - batchAmount ''{0}'' - receipt amount ''{1}''", batchAmount, signedDto.getBatchAmount()));
        if(!signedDto.getCurrencyCode().equals(signedDto.getCurrencyCode())) throw new ValidationExceptionVS(MessageFormat.format(
                "ERROR - batch currencyCode ''{0}'' - receipt currencyCode ''{1}''",  currencyCode, signedDto.getCurrencyCode()));
        if(timeLimited.booleanValue() != signedDto.timeLimited().booleanValue()) throw new ValidationExceptionVS(MessageFormat.format(
                "ERROR - batch timeLimited ''{0}'' - receipt timeLimited ''{1}''",  timeLimited, signedDto.timeLimited()));
        if(!tag.equals(signedDto.getTag())) throw new ValidationExceptionVS(MessageFormat.format(
                "ERROR - batch tag ''{0}'' - receipt tag ''{1}''",  tag, signedDto.getTag()));
        if(!currencySet.equals(signedDto.getCurrencySet())) throw new ValidationExceptionVS("ERROR - currencySet mismatch");
        return receipt;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) throws Exception {
        this.leftOver = leftOverCurrency.getAmount();
        this.leftOverCurrency = leftOverCurrency;
        this.leftOverCSR = new String(leftOverCurrency.getCertificationRequest().getCsrPEM(), "UTF-8");
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public Boolean timeLimited() {
        return timeLimited;
    }

    public void setIsTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Set<String> getCurrencySet() {
        return currencySet;
    }

    public void setCurrencySet(Set<String> currencySet) {
        this.currencySet = currencySet;
    }

    public String getLeftOverCSR() {
        return leftOverCSR;
    }

    public void setLeftOverCSR(String leftOverCSR) {
        this.leftOverCSR = leftOverCSR;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public Collection<Currency> getCurrencyCollection() {
        return currencyCollection;
    }

    public void setCurrencyCollection(Collection<Currency> currencyCollection) {
        this.currencyCollection = currencyCollection;
    }

    public String getCurrencyChangeCSR() {
        return currencyChangeCSR;
    }

    public void setCurrencyChangeCSR(String currencyChangeCSR) {
        this.currencyChangeCSR = currencyChangeCSR;
    }
}