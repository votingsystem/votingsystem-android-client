package org.votingsystem.dto.currency;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.model.Currency;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyStateDto {

    private String revocationHash;
    private Long batchId;
    private Currency.State state;
    private Currency.Type type;
    private String currencyCert;
    private String leftOverCert;
    private String currencyChangeCert;
    private Date dateCreated;

    public CurrencyStateDto() {}

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public Currency.State getState() {
        return state;
    }

    public void setState(Currency.State state) {
        this.state = state;
    }

    public Currency.Type getType() {
        return type;
    }

    public void setType(Currency.Type type) {
        this.type = type;
    }

    public String getLeftOverCert() {
        return leftOverCert;
    }

    public void setLeftOverCert(String leftOverCert) {
        this.leftOverCert = leftOverCert;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCurrencyCert() {
        return currencyCert;
    }

    public void setCurrencyCert(String currencyCert) {
        this.currencyCert = currencyCert;
    }

}
