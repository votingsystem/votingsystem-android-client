package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.throwable.ExceptionVS;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagVSInfoDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String name;
    private String currencyCode;
    private BigDecimal timeLimited = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal from = BigDecimal.ZERO;

    public TagVSInfoDto(String name, String currencyCode) {
        this.currencyCode = currencyCode;
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(BigDecimal timeLimited) {
        this.timeLimited = timeLimited;
    }

    public BigDecimal getFrom() {
        return from;
    }

    public void setFrom(BigDecimal from) {
        this.from = from;
    }

    public BigDecimal getCash() {
        return total.subtract(from);
    }

    public BigDecimal getTimeLimitedRemaining() {
        return timeLimited.subtract(from);
    }

    public void checkResult(BigDecimal cashExpected) throws ExceptionVS {
        BigDecimal result = getCash();
        if(result.compareTo(cashExpected) != 0) throw new ExceptionVS("currency '" +
            currencyCode + "' tag '" + name + "' cash expected '" + cashExpected.toPlainString() +
            "' calculated '" + result.toPlainString() + "'");
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

}