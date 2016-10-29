package org.votingsystem.dto.currency;

import java.io.Serializable;


public class PaymentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originRevocationHash;
    private String revocationHash;
    private String UUID;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
