package org.votingsystem.dto.metadata;

import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MetadataDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String language;
    private String timeZone;
    private String validUntil;
    private SystemEntityDto entity;
    private Set<KeyDto> keyDescriptorSet;
    private TrustedEntitiesDto trustedEntities;
    private X509Certificate signingCertificate;

    public MetadataDto() { }

    public MetadataDto(SystemEntityType entityType, String entityID, Set<KeyDto> keyDescriptorSet) {
        this.entity = new SystemEntityDto(entityID, entityType);
        this.keyDescriptorSet = keyDescriptorSet;
    }

    public Set<KeyDto> getKeyDescriptorSet() {
        return keyDescriptorSet;
    }

    public void setKeyDescriptorSet(Set<KeyDto> keyDescriptorSet) {
        this.keyDescriptorSet = keyDescriptorSet;
    }

    public Date getValidUntilDate() {
        if (validUntil == null) return null;
        return DateUtils.getDate(validUntil);
    }


    public MetadataDto setOrganization(OrganizationDto organization) {
        this.entity.setOrganization(organization);
        return this;
    }


    public MetadataDto setContactPerson(ContactPersonDto contactPerson) {
        this.entity.setContactPerson(contactPerson);
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public MetadataDto setLanguage(String language) {
        this.language = language;
        return this;
    }

    public MetadataDto setValidUntil(String validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public SystemEntityDto getEntity() {
        return entity;
    }

    public void setEntity(SystemEntityDto entity) {
        this.entity = entity;
    }

    public MetadataDto setLocation(LocationDto location) {
        this.entity.setLocation(location);
        return this;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        //return TimeZone.getDefault().getDisplayName();
        return timeZone;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(X509Certificate signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public TrustedEntitiesDto getTrustedEntities() {
        return trustedEntities;
    }

    public void setTrustedEntities(TrustedEntitiesDto trustedEntities) {
        this.trustedEntities = trustedEntities;
    }

    public String getFirstTimeStampEntityId() {
        String result = null;
        if(trustedEntities != null) {
            for(TrustedEntitiesDto.EntityDto entity : trustedEntities.getEntities()) {
                if(entity.getType() == SystemEntityType.TIMESTAMP_SERVER)
                    return entity.getId();
            }
        }
        return result;
    }

}
