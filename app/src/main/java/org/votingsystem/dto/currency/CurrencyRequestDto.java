package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.TagVSDto;
import org.votingsystem.model.Currency;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRequestDto {

    private static Logger log = Logger.getLogger(CurrencyRequestDto.class.getSimpleName());

    private TypeVS operation = TypeVS.CURRENCY_REQUEST;
    private String subject;
    private String serverURL;
    private String currencyCode;
    private TagVSDto tagVS;
    private String UUID;
    private BigDecimal totalAmount;
    private Boolean timeLimited;

    @JsonIgnore private Map<String, CurrencyDto> currencyDtoMap;
    @JsonIgnore private Map<String, Currency> currencyMap;
    @JsonIgnore private Set<String> requestCSRSet;

    public CurrencyRequestDto() {}

    public static CurrencyRequestDto CREATE_REQUEST(TransactionDto transactionDto,
            BigDecimal currencyValue, String serverURL) throws Exception {
        CurrencyRequestDto currencyRequestDto = new CurrencyRequestDto();
        currencyRequestDto.serverURL = serverURL;
        currencyRequestDto.subject = transactionDto.getSubject();
        currencyRequestDto.totalAmount = transactionDto.getAmount();
        currencyRequestDto.currencyCode = transactionDto.getCurrencyCode();
        currencyRequestDto.timeLimited = transactionDto.isTimeLimited();
        currencyRequestDto.tagVS = transactionDto.getTagVS();
        currencyRequestDto.UUID = java.util.UUID.randomUUID().toString();

        Map<String, Currency> currencyMap = new HashMap<>();
        Set<String> requestCSRSet = new HashSet<>();
        BigDecimal divideAndRemainder[] = transactionDto.getAmount().divideAndRemainder(currencyValue);
        if(divideAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) throw new ValidationExceptionVS(MessageFormat.format(
                "request with remainder - requestAmount ''{0}''  currencyValue ''{{1}}'' remainder ''{{2}}''",
                transactionDto.getAmount(), currencyValue, divideAndRemainder[1]));
        for(int i = 0; i < divideAndRemainder[0].intValue(); i++) {
            Currency currency = new Currency(serverURL, currencyValue, transactionDto.getCurrencyCode(),
                    transactionDto.isTimeLimited(), currencyRequestDto.tagVS.getName());
            requestCSRSet.add(new String(currency.getCertificationRequest().getCsrPEM()));
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        currencyRequestDto.requestCSRSet = requestCSRSet;
        currencyRequestDto.setCurrencyMap(currencyMap);
        return currencyRequestDto;
    }


    public void loadCurrencyCerts(Collection<String> currencyCerts) throws Exception {
        log.info("loadCurrencyCerts - Num IssuedCurrency: " + currencyCerts.size());
        if(currencyCerts.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + currencyCerts.size());
        }
        for(String pemCert:currencyCerts) {
            Currency currency = loadCurrencyCert(pemCert);
            currencyMap.put(currency.getHashCertVS(), currency);
        }
    }

    public Currency loadCurrencyCert(String pemCert) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(pemCert.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(pemCert.getBytes());
        return currency;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public Set<String> getRequestCSRSet() {
        return requestCSRSet;
    }

    public void setRequestCSRSet(Set<String> requestCSRSet) {
        this.requestCSRSet = requestCSRSet;
    }

    public Map<String, CurrencyDto> getCurrencyDtoMap() {
        return currencyDtoMap;
    }

    public void setCurrencyDtoMap(Map<String, CurrencyDto> currencyDtoMap) {
        this.currencyDtoMap = currencyDtoMap;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public TagVSDto getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVSDto tagVS) {
        this.tagVS = tagVS;
    }
}