package org.votingsystem.dto;

import android.util.Base64;

import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRResponseDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private Date date;
    private OperationType operationType;
    private String base64Data;

    public QRResponseDto() {}

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Date getDate() {
        return date;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public byte[] getData() {
        return Base64.decode(base64Data.getBytes(), Base64.NO_WRAP);
    }

    public QRResponseDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }

    public QRResponseDto setDate(Date date) {
        this.date = date;
        return this;
    }

}
