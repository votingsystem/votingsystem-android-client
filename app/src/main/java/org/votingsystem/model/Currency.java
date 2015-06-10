package org.votingsystem.model;

import android.content.Context;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Currency extends ReceiptWrapper {

    public static final String TAG = Currency.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State { OK, EXPENDED, LAPSED, UNKNOWN, ERROR;}

    private Long localId = -1L;
    private Long id;
    private TypeVS operation;
    private BigDecimal batchAmount;
    private transient SMIMEMessage receipt;
    private transient SMIMEMessage cancellationReceipt;
    private transient SMIMEMessage smimeMessage;
    private transient X509Certificate x509AnonymousCert;
    private CertificationRequestVS certificationRequest;
    private byte[] receiptBytes;
    private byte[] cancellationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVS;
    private BigDecimal amount;
    private String subject;
    private State state;
    private Date cancellationDate;
    private String currencyCode;
    private String url;
    private String tag;
    private String currencyServerURL;
    private Boolean timeLimited = Boolean.FALSE;
    private Date validFrom;
    private Date validTo;
    private Date dateCreated;
    private String toUserIBAN;
    private String toUserName;
    private String batchUUID;
    private CurrencyCertExtensionDto certExtensionDto;
    private Long serialNumber;
    private byte[] content;


    public Currency() {}

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode,
                    Boolean timeLimited, String hashCertVS, String tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.timeLimited = timeLimited;
        try {
            this.hashCertVS = hashCertVS;
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM, ContextVS.PROVIDER,
                    currencyServerURL, hashCertVS, amount, this.currencyCode, timeLimited, tag);
        } catch(Exception ex) {  ex.printStackTrace(); }
    }


    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode,
                    Boolean timeLimited, String tag) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.timeLimited = timeLimited;
        try {
            this.originHashCertVS = UUID.randomUUID().toString();
            this.hashCertVS = CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST);
            certificationRequest = CertificationRequestVS.getCurrencyRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM, ContextVS.PROVIDER,
                    currencyServerURL, hashCertVS, amount, this.currencyCode, timeLimited, tag);
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
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

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public void validateReceipt(SMIMEMessage smimeReceipt, Set<TrustAnchor> trustAnchor)
            throws Exception {
        if(!smimeMessage.getSigner().getSignedContentDigestBase64().equals(
                smimeReceipt.getSigner().getSignedContentDigestBase64())){
            throw new ExceptionVS("Signer content digest mismatch");
        }
        for(X509Certificate cert : smimeReceipt.getSignersCerts()) {
            CertUtils.verifyCertificate(trustAnchor, false, Arrays.asList(cert));
            Log.d(TAG, "validateReceipt - Cert validated: " + cert.getSubjectDN().toString());
        }
        this.smimeMessage = smimeReceipt;
    }

    public Currency(SMIMEMessage smimeMessage) throws Exception {
        smimeMessage.isValidSignature();
        this.smimeMessage = smimeMessage;
        initCertData(smimeMessage.getCurrencyCert());
        CurrencyDto batchItemDto = JSON.getMapper().readValue(smimeMessage.getSignedContent(), CurrencyDto.class);
        this.batchUUID = batchItemDto.getBatchUUID();
        this.batchAmount = batchItemDto.getBatchAmount();
        if(TypeVS.CURRENCY_SEND != batchItemDto.getOperation())
            throw new ExceptionVS("Expected operation 'CURRENCY_SEND' - found: " + batchItemDto.getOperation() + "'");
        if(!this.currencyCode.equals(batchItemDto.getCurrencyCode())) {
            throw new ExceptionVS(getErrorPrefix() +
                    "expected currencyCode '" + currencyCode + "' - found: '" + batchItemDto.getCurrencyCode());
        }
        tag = batchItemDto.getTag();
        if(!TagVSDto.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(tag))
            throw new ExceptionVS("expected tag '" + certExtensionDto.getTag() + "' - found: '" +
                    batchItemDto.getTag());

        Date signatureTime = smimeMessage.getTimeStampToken(smimeMessage.getCurrencyCert())
                .getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
        this.timeLimited = batchItemDto.isTimeLimited();
    }

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

    public void setSMIME(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public void initSigner (byte[] signedCsr) throws Exception {
        certificationRequest.initSigner(signedCsr);
        initCertData(certificationRequest.getCertificate());
    }

    public static Currency fromCertificationRequestVS(CertificationRequestVS certificationRequest)
            throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
        currency.initSigner(certificationRequest.getSignedCsr());
        return currency;
    }

    public byte[] getIssuedCertPEM() throws IOException {
        return CertUtils.getPEMEncoded(x509AnonymousCert);
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public void setCertificationRequest(CertificationRequestVS certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public String getStateMsg(Context context) {
        if(state == null) return null;
        switch (state) {
            case OK: return context.getString(R.string.active_lbl);
            case EXPENDED: return context.getString(R.string.expended_lbl);
            case LAPSED: return context.getString(R.string.lapsed_lbl);
            default: return state.toString();
        }
    }

    public Integer getStateColor(Context context) {
        if(state == null) return context.getResources().getColor(R.color.unknown_vs);
        switch (state) {
            case OK: return context.getResources().getColor(R.color.active_vs);
            default: return context.getResources().getColor(R.color.terminated_vs);
        }
    }

    public static boolean checkIfTimeLimited(Date notBefore, Date notAfter) {
        long diff = notAfter.getTime() - notBefore.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) <= 7;//one week
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

    public String getTag() {
        if(tag != null) return tag.trim();
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with hash: " + hashCertVS + " - ";
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override public String getSubject() {
        return subject;
    }

    @Override public Date getDateFrom() {
        return certificationRequest.getCertificate().getNotBefore();
    }

    @Override public Date getDateTo() {
        return certificationRequest.getCertificate().getNotAfter();
    }

    @Override public Long getLocalId() {
        return localId;
    }

    @Override public void setLocalId(Long localId) {
        this.localId = localId;
    }

    @Override public SMIMEMessage getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) receipt = new SMIMEMessage(receiptBytes);
        return receipt;
    }

    public SMIMEMessage getCancellationReceipt() throws Exception {
        if(cancellationReceipt == null && cancellationReceiptBytes != null) cancellationReceipt =
                new SMIMEMessage(cancellationReceiptBytes);
        return cancellationReceipt;
    }

    public State getState() {
        return state;
    }

    public Currency setState(State state) {
        this.state = state;
        return this;
    }

    public void setReceiptBytes(byte[] receiptBytes) {
        this.state = State.EXPENDED;
        this.receiptBytes = receiptBytes;
    }

    public void setCancellationReceiptBytes(byte[] receiptBytes) {
        this.cancellationReceiptBytes = receiptBytes;
    }

    public void setCancellationReceipt(SMIMEMessage receipt) {
        try {
            this.cancellationReceiptBytes = receipt.getBytes();
            this.cancellationReceipt = receipt;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public Date getCancellationDate() {
        try {
            if(cancellationDate == null && getCancellationReceipt() != null)
                cancellationDate = getCancellationReceipt().getSigner().getTimeStampToken().
                        getTimeStampInfo().getGenTime();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return cancellationDate;
    }

    @Override public String getMessageId() {
        String result = null;
        try {
            SMIMEMessage receipt = getReceipt();
            if(receipt == null) return null;
            String[] headers = receipt.getHeader("Message-ID");
            if(headers != null && headers.length >0) return headers[0];
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency initCertData(X509Certificate x509AnonymousCert) throws Exception {
        this.x509AnonymousCert = x509AnonymousCert;
        content = x509AnonymousCert.getEncoded();
        serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        hashCertVS = certExtensionDto.getHashCertVS();
        timeLimited = certExtensionDto.getTimeLimited();
        tag = certExtensionDto.getTag().trim();
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        validFrom = x509AnonymousCert.getNotBefore();
        validTo = x509AnonymousCert.getNotAfter();
        String subjectDN = x509AnonymousCert.getSubjectDN().toString();
        CurrencyDto certSubjectDto = CurrencyDto.getCertSubjectDto(subjectDN, hashCertVS);
        if(!certSubjectDto.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(certExtensionDto.getTag()))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(smimeMessage != null) s.writeObject(smimeMessage.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] smimeMessageBytes = (byte[]) s.readObject();
        if(smimeMessageBytes != null) smimeMessage = new SMIMEMessage(smimeMessageBytes);
        if(certificationRequest != null) fromCertificationRequestVS(certificationRequest);
    }

}