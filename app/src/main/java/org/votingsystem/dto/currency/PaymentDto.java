package org.votingsystem.dto.currency;

import java.io.Serializable;


public class PaymentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originHashCertVS;
    private String hashCertVS;
    private String UUID;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
