package org.votingsystem.dto.voting;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonymousDelegationCertExtensionDto {

    private String accessControlURL;
    private String revocationHash;
    private Integer weeksOperationActive;
    private Date validFrom;
    private Date validTo;

    public AnonymousDelegationCertExtensionDto() {}

    public AnonymousDelegationCertExtensionDto(String accessControlURL, String revocationHash, Integer weeksOperationActive,
                                               Date validFrom, Date validTo) {
        this.accessControlURL = accessControlURL;
        this.revocationHash = revocationHash;
        this.weeksOperationActive = weeksOperationActive;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }


    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public Integer getWeeksOperationActive() {
        return weeksOperationActive;
    }

    public void setWeeksOperationActive(Integer weeksOperationActive) {
        this.weeksOperationActive = weeksOperationActive;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }
}
