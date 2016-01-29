package org.votingsystem.dto.currency;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponseDto {

    private TypeVS operation;
    private String smimeMessage;
    private String currencyChangeCert;

    public TransactionResponseDto() {}

    public TransactionResponseDto(TypeVS operation, String currencyChangeCert,
                                  SMIMEMessage smimeMessage) throws Exception {
        this.operation = operation;
        this.smimeMessage = Base64.encodeToString(smimeMessage.getBytes(), Base64.NO_WRAP);
        this.currencyChangeCert = currencyChangeCert;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }
}
