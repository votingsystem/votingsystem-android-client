package org.votingsystem.dto.metadata;

import org.votingsystem.http.SystemEntityType;

import java.io.Serializable;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SystemEntityDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private SystemEntityType entityType;
    private String id;
    private OrganizationDto organization;
    private LocationDto location;
    private ContactPersonDto contactPerson;
    private Set<SystemEntityDto> timeStampServiceProviders;

    public SystemEntityDto() {
    }

    public SystemEntityDto(String id, SystemEntityType entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    public SystemEntityType getSystemEntityType() {
        return entityType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SystemEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(SystemEntityType entityType) {
        this.entityType = entityType;
    }

    public OrganizationDto getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDto organization) {
        this.organization = organization;
    }

    public ContactPersonDto getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(ContactPersonDto contactPerson) {
        this.contactPerson = contactPerson;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public Set<SystemEntityDto> getTimeStampServiceProviders() {
        return timeStampServiceProviders;
    }

    public void setTimeStampServiceProviders(Set<SystemEntityDto> timeStampServiceProviders) {
        this.timeStampServiceProviders = timeStampServiceProviders;
    }

}
