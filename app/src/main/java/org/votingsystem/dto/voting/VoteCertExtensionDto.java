package org.votingsystem.dto.voting;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteCertExtensionDto {

    private String accessControlURL;
    private String revocationHash;
    private Long eventId;

    public VoteCertExtensionDto() {}

    public VoteCertExtensionDto(String accessControlURL, String revocationHash, Long eventId) {
        this.accessControlURL = accessControlURL;
        this.revocationHash = revocationHash;
        this.eventId = eventId;
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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
