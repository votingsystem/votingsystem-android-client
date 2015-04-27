package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.TagVSDto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyAccountDto {

    private Long id;
    private String currencyCode;
    private String IBAN;
    private BigDecimal amount;
    private Date lastUpdated;
    private TagVSDto tag;

    public CurrencyAccountDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public TagVSDto getTag() {
        return tag;
    }

    public void setTag(TagVSDto tag) {
        this.tag = tag;
    }
}
