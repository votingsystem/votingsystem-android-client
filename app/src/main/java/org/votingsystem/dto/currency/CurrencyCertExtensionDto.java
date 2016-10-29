package org.votingsystem.dto.currency;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCertExtensionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currencyServerURL;
    private String revocationHash;
    private String currencyCode;
    private String tag;
    private Boolean timeLimited;
    private BigDecimal amount;

    public CurrencyCertExtensionDto() {}

    public CurrencyCertExtensionDto(BigDecimal amount, String currencyCode, String revocationHash, String currencyServerURL,
                                    Boolean timeLimited, String tag) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.revocationHash = revocationHash;
        this.currencyServerURL = currencyServerURL;
        this.tag = tag;
        this.timeLimited = timeLimited;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
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

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

}
