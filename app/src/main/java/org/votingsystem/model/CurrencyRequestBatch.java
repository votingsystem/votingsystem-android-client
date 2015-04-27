package org.votingsystem.model;

import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyRequestBatch implements Serializable  {

    private static Logger log = Logger.getLogger(CurrencyRequestBatch.class.getSimpleName());

    public static final long serialVersionUID = 1L;

    private String tagVS;
    private Boolean isTimeLimited;

    private CurrencyRequestDto requestDto;
    private Map<String, Currency> currencyMap;
    private List<CurrencyDto> currencyDtoList;
    private SMIMEMessage smimeMessage;

    public CurrencyRequestBatch() {}

    public CurrencyRequestBatch(CurrencyRequestDto requestDto, SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
        this.requestDto = requestDto;
    }

    public static CurrencyRequestBatch createRequest(BigDecimal requestAmount, BigDecimal currencyValue,
                String currencyCode, TagVSDto tagVS, Boolean isTimeLimited, String serverURL) throws Exception {
        if(tagVS == null) tagVS = new TagVSDto(TagVSDto.WILDTAG);
        CurrencyRequestDto requestDto = new CurrencyRequestDto(null, requestAmount, currencyCode, isTimeLimited,
                serverURL, tagVS);
        CurrencyRequestBatch currencyRequestBatch = new CurrencyRequestBatch();
        currencyRequestBatch.setRequestDto(requestDto);
        Map<String, Currency> currencyMap = new HashMap<>();
        BigDecimal numCurrency = requestAmount.divide(currencyValue);
        for(int i = 0; i < numCurrency.intValue(); i++) {
            Currency currency = new Currency(serverURL, currencyValue, currencyCode, tagVS.getName());
            currency.setTimeLimited(isTimeLimited);
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        currencyRequestBatch.setCurrencyMap(currencyMap);
        List<CurrencyDto> currencyDtoList = new ArrayList<>();
        for(Currency currency : currencyMap.values()) {
            currencyDtoList.add(CurrencyDto.serialize(currency));
        }
        currencyRequestBatch.setCurrencyDtoList(currencyDtoList);
        return currencyRequestBatch;
    }

    public static CurrencyRequestBatch createRequest(TransactionVSDto transactionVSDto,
        String serverURL) throws Exception {
        return createRequest(transactionVSDto.getAmount(), transactionVSDto.getAmount(),
                transactionVSDto.getCurrencyCode(), transactionVSDto.getTagVS(),
                transactionVSDto.isTimeLimited(), serverURL);
    }


    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public List<String> getIssuedCurrencyListPEM() throws IOException {
        List<String> result = new ArrayList<>();
        for(Currency currency : currencyMap.values()) {
            result.add(new String(currency.getIssuedCertPEM()));
        }
        return result;
    }

    public void loadIssuedCurrency(List<String> issuedCurrencyArray) throws Exception {
        log.info("CurrencyRequestBatch - Num IssuedCurrency: " + issuedCurrencyArray.size());
        if(issuedCurrencyArray.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + issuedCurrencyArray.size());
        }
        for(int i = 0; i < issuedCurrencyArray.size(); i++) {
            Currency currency = loadIssuedCurrency(issuedCurrencyArray.get(i));
            currencyMap.put(currency.getHashCertVS(), currency);
        }
    }

    public Currency loadIssuedCurrency(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(signedCsr.getBytes());
        return currency;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }


    public List<CurrencyDto> getCurrencyCSRList () {
        return currencyDtoList;
    }

    public String getTagVS() {
        if(tagVS != null) return tagVS;
        else return requestDto.getTagVS().getName();
    }

    public void setTagVS(TagVSDto tagVS) {
        this.tagVS = tagVS.getName();
        for(Currency currency : currencyMap.values()) {
            currency.setTag(tagVS.getName());
        }
    }

    public BigDecimal getRequestAmount() {
        if(requestDto == null) return null;
        else return requestDto.getTotalAmount();
    }

    public String getCurrencyCode() {
        if(requestDto == null) return null;
        else return requestDto.getCurrencyCode();
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public CurrencyRequestDto getRequestDto() {
        return requestDto;
    }

    public void setRequestDto(CurrencyRequestDto requestDto) {
        this.requestDto = requestDto;
    }

    public void setCurrencyDtoList(List<CurrencyDto> currencyDtoList) {
        this.currencyDtoList = currencyDtoList;
    }

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }
}