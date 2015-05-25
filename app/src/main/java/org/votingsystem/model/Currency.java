package org.votingsystem.model;

import android.content.Context;
import android.util.Log;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Payment;
import org.votingsystem.util.ReceiptWrapper;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Currency extends ReceiptWrapper {

    public static final String TAG = Currency.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State { OK, EXPENDED, LAPSED;}

    private Long localId = -1L;
    private TransactionVSDto transaction;
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
    private String signedTagVS;
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
    private String toUserIBAN;
    private String toUserName;
    private String batchUUID;
    private Payment paymentMethod;

    private PKCS10CertificationRequest csr;
    private CurrencyCertExtensionDto certExtensionDto;
    private CurrencyDto certSubjectDto;

    private TypeVS operation;
    private BigDecimal batchAmount;

    public Currency() {}

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

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public TransactionVSDto getSendRequest(String toUserName, String toUserIBAN, String subject,
                                           Boolean isTimeLimited) throws ExceptionVS {
        this.toUserName = toUserName;
        this.toUserIBAN = toUserIBAN;
        this.subject = subject;
        if(isTimeLimited == false && checkIfTimeLimited(x509AnonymousCert.getNotBefore(),
                x509AnonymousCert.getNotAfter())) {
            throw new ExceptionVS("Time limited Currency with 'isTimeLimited' signature param set to false");
        }
        return TransactionVSDto.CURRENCY_SEND(toUserName, subject, amount, currencyCode, toUserIBAN,
                isTimeLimited, tag);
    }

    public Currency(SMIMEMessage smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        x509AnonymousCert = smimeMessage.getCurrencyCert();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        initCertData(certExtensionDto, smimeMessage.getCurrencyCert().getSubjectDN().toString());
        CurrencyBatchDto currencyBatchDto = JSON.readValue(smimeMessage.getSignedContent(), CurrencyBatchDto.class);
        if(amount.compareTo(currencyBatchDto.getCurrencyAmount()) != 0) throw new ExceptionVS("Currency amount '" + amount +
                "' CurrencyBatchDto amount  '" + currencyBatchDto.getCurrencyAmount() + "'");
        this.batchAmount = currencyBatchDto.getBatchAmount();
        this.batchUUID = currencyBatchDto.getBatchUUID();
        this.paymentMethod = currencyBatchDto.getPaymentMethod();
        this.operation = currencyBatchDto.getOperation();
        if(TypeVS.CURRENCY_SEND != operation)
            throw new ExceptionVS("Expected operation 'CURRENCY_SEND' - found: " + currencyBatchDto.getOperation() + "'");
        if(!this.currencyCode.equals(currencyBatchDto.getCurrencyCode())) {
            throw new ExceptionVS(getErrorPrefix() +
                    "expected currencyCode '" + currencyCode + "' - found: '" + currencyBatchDto.getCurrencyCode());
        }
        tag = currencyBatchDto.getTag();
        if(!TagVSDto.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(tag))
            throw new ExceptionVS("expected tag '" + certExtensionDto.getTag() + "' - found: '" +
                    currencyBatchDto.getTag());
        Date signatureTime = smimeMessage.getTimeStampToken(x509AnonymousCert).getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        this.subject = currencyBatchDto.getSubject();
        this.toUserIBAN = currencyBatchDto.getToUserIBAN();
        this.toUserName = currencyBatchDto.getToUserName();
        this.timeLimited = currencyBatchDto.isTimeLimited();
    }

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public Currency(PKCS10CertificationRequest csr) throws ExceptionVS, IOException {
        this.csr = csr;
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        CurrencyCertExtensionDto certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.CURRENCY_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                    certExtensionDto = JSON.readValue(certAttributeJSONStr, CurrencyCertExtensionDto.class);
                    break;
            }
        }
        initCertData(certExtensionDto, info.getSubject().toString());
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        setCertificationRequest(certificationRequest.initSigner(csrBytes));
    }

    public static Currency fromCertificationRequestVS(CertificationRequestVS certificationRequest) throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
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

    public void setCertificationRequest(CertificationRequestVS certificationRequest)
            throws Exception {
        this.certificationRequest = certificationRequest;
        x509AnonymousCert = certificationRequest.getCertificate();
        if(x509AnonymousCert != null) {
            validFrom = x509AnonymousCert.getNotBefore();
            validTo = x509AnonymousCert.getNotAfter();
            if(Calendar.getInstance().getTime().after(validTo) && state == State.OK) {
                state = State.LAPSED;
            }
        }
        CurrencyCertExtensionDto certExtensionData = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, ContextVS.CURRENCY_OID);
        initCertData(certExtensionData, x509AnonymousCert.getSubjectDN().toString());
        certSubjectDto.setNotBefore(x509AnonymousCert.getNotBefore());
        certSubjectDto.setNotAfter(x509AnonymousCert.getNotAfter());
        state = State.OK;
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

    public String getSignedTagVS() {
        return signedTagVS;
    }

    public void setSignedTagVS(String signedTagVS) {
        this.signedTagVS = signedTagVS;
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

    public TransactionVSDto getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionVSDto transaction) {
        this.transaction = transaction;
    }

    public static JSONObject getCurrencyAccountInfoRequest(String nif) {
        Map mapToSend = new HashMap();
        mapToSend.put("NIF", nif);
        mapToSend.put("operation", TypeVS.CURRENCY_ACCOUNTS_INFO.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(mapToSend);
    }

    public JSONObject getCancellationRequest() {
        Map dataMap = new HashMap();
        dataMap.put("UUID", UUID.randomUUID().toString());
        dataMap.put("operation", TypeVS.CURRENCY_CANCEL.toString());
        dataMap.put("hashCertVS", getHashCertVS());
        dataMap.put("originHashCertVS", getOriginHashCertVS());
        dataMap.put("currencyCertSerialNumber", getCertificationRequest().
                getCertificate().getSerialNumber().longValue());
        return new JSONObject(dataMap);
    }

    public JSONObject getTransaction(String toUserName,
            String toUserIBAN, String tag, Boolean isTimeLimited) {
        Map dataMap = new HashMap();
        dataMap.put("operation", TypeVS.CURRENCY.toString());
        dataMap.put("subject", subject);
        dataMap.put("toUser", toUserName);
        dataMap.put("toUserIBAN", toUserIBAN);
        dataMap.put("tagVS", tag);
        dataMap.put("amount", amount.toString());
        dataMap.put("currencyCode", currencyCode);
        if(isTimeLimited != null) dataMap.put("isTimeLimited", isTimeLimited.booleanValue());
        dataMap.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(dataMap);
    }

    public Currency initCertData(CurrencyCertExtensionDto certExtensionDto, String subjectDN) throws ExceptionVS {
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        this.certExtensionDto = certExtensionDto;
        certSubjectDto = getCertSubjectDto(subjectDN, hashCertVS);
        hashCertVS = certExtensionDto.getHashCertVS();
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        if(!certSubjectDto.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        amount = certExtensionDto.getAmount();
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + subjectDN);
        currencyCode = certExtensionDto.getCurrencyCode();
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(certExtensionDto.getTag()))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    private CurrencyDto getCertSubjectDto(String subjectDN, String hashCertVS) {
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