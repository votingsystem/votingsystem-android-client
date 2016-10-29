package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.util.OperationType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponseDto {

    private OperationType operation;
    private String cmsMessagePEM;
    private String currencyChangeCert;

    public TransactionResponseDto() {}

    public TransactionResponseDto(OperationType operation, String currencyChangeCert,
                                  CMSSignedMessage cmsMessage) throws Exception {
        this.operation = operation;
        this.cmsMessagePEM = cmsMessage.toPEMStr();
        this.currencyChangeCert = currencyChangeCert;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getCMSMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCMSMessage(String cmsMessage) {
        this.cmsMessagePEM = cmsMessage;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

}