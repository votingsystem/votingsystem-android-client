package org.votingsystem.model;

import android.util.Base64;

import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyServerDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyBatch {

    public static final String TAG = CurrencyBatch.class.getSimpleName();

    private CurrencyServerDto currencyServer;

    private UserVSDto toUserVS;
    private BigDecimal batchAmount = null;
    private BigDecimal wildTagAmount;
    private BigDecimal currencyAmount = null;
    private String tagVS;
    private Boolean isTimeLimited = Boolean.FALSE;
    private String batchUUID;
    private String subject;

    private List<Currency> tagCurrencyList;
    private List<Currency> wildTagCurrencyList;

    private List<Currency> currencyList;
    private Currency leftOverCurrency;
    private BigDecimal leftOver;
    private TypeVS operation;
    private String currencyCode;
    private String toUserIBAN;
    private String tag;
    private SMIMEMessage smimeMessage;
    private Map<String, Currency> currencyMap;

    public CurrencyBatch() {}

    public CurrencyBatch(BigDecimal batchAmount, BigDecimal currencyAmount, String currencyCode,
                         String tagVS, Boolean isTimeLimited, CurrencyServerDto currencyServer) throws Exception {
        this.batchAmount = batchAmount;
        this.setCurrencyServerDto(currencyServer);
        this.setCurrencyCode(currencyCode);
        this.currencyAmount = currencyAmount;
        this.isTimeLimited = isTimeLimited;
        this.tagVS = (tagVS == null)? TagVSDto.WILDTAG:tagVS;
    }

    public CurrencyBatch(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public void addCurrency(Currency currency) {
        if(currencyList == null) currencyList = new ArrayList<Currency>();
        currencyList.add(currency);
    }

    public void validateTransactionVSResponse(Map dataMap, Set<TrustAnchor> trustAnchor) throws Exception {
        SMIMEMessage receipt = new SMIMEMessage(Base64.decode(((String) dataMap.get("receipt")).getBytes(), Base64.NO_WRAP));
        if(dataMap.containsKey("leftOverCoin")) {

        }


        Map<String, Currency> currencyMap = getCurrencyMap();
        if(currencyMap.size() != dataMap.size()) throw new ExceptionVS("Num. currency: '" +
                currencyMap.size() + "' - num. receipts: " + dataMap.size());
        for(int i = 0; i < dataMap.size(); i++) {
            Map receiptData = (Map) dataMap.get(i);
            //TODO
            String hashCertVS = (String) receiptData.keySet().iterator().next();
            SMIMEMessage smimeReceipt = new SMIMEMessage(
                    Base64.decode(((String) receiptData.get(hashCertVS)).getBytes(), Base64.NO_WRAP));
            CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    smimeReceipt.getCurrencyCert(), ContextVS.CURRENCY_OID);
            Currency currency = currencyMap.remove(certExtensionDto.getHashCertVS());
            currency.validateReceipt(smimeReceipt, trustAnchor);
        }
        if(currencyMap.size() != 0) throw new ExceptionVS(currencyMap.size() + " Currency transactions without receipt");
    }


    public static CurrencyBatch getAnonymousSignedTransactionBatch(BigDecimal totalAmount,
            String currencyCode, String tagVS, Boolean isTimeLimited,
            CurrencyServerDto currencyServer) throws Exception {
        CurrencyBatch result = new CurrencyBatch(totalAmount, null, currencyCode, tagVS,
                isTimeLimited, currencyServer);
        return result;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public CurrencyServerDto getCurrencyServer() {
        return currencyServer;
    }

    public void setCurrencyServerDto(CurrencyServerDto currencyServer) {
        this.currencyServer = currencyServer;
    }


    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public Currency initCurrency(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS(
                "Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                CurrencyCertExtensionDto.class, x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(signedCsr.getBytes());
        return currency;
    }

    public void initCurrency(Collection<String> issuedCurrencyCollection) throws Exception {
        LOGD(TAG + ".initCurrency", "num currency: " + issuedCurrencyCollection.size());
        if(issuedCurrencyCollection.size() != currencyMap.size()) {
            LOGD(TAG + ".initCurrency", "num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + issuedCurrencyCollection.size());
        }
        for(String issuedCurrency : issuedCurrencyCollection) {
            Currency currency = initCurrency(issuedCurrency);
            currencyMap.put(currency.getHashCertVS(), currency);
        }
    }

    private static Comparator<Currency> currencyComparator = new Comparator<org.votingsystem.model.Currency>() {
        public int compare(org.votingsystem.model.Currency c1, org.votingsystem.model.Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

    public UserVSDto getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVSDto toUserVS) {
        this.toUserVS = toUserVS;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getWildTagAmount() {
        return wildTagAmount;
    }

    public void setWildTagAmount(BigDecimal wildTagAmount) {
        this.wildTagAmount = wildTagAmount;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public Boolean isTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Currency> getTagCurrencyList() {
        return tagCurrencyList;
    }

    public void setTagCurrencyList(List<Currency> tagCurrencyList) {
        this.tagCurrencyList = tagCurrencyList;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public List<Currency> getWildTagCurrencyList() {
        return wildTagCurrencyList;
    }

    public void setWildTagCurrencyList(List<Currency> wildTagCurrencyList) {
        this.wildTagCurrencyList = wildTagCurrencyList;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public List<Currency> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }
}
