package org.votingsystem.dto.metadata;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OrganizationDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String organizationName;
    private String organizationUnit;
    private String organizationURL;

    public OrganizationDto() {
    }

    public OrganizationDto(String organizationName, String organizationUnit, String organizationURL) {
        this.organizationName = organizationName;
        this.organizationUnit = organizationUnit;
        this.organizationURL = organizationURL;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationURL() {
        return organizationURL;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }

    public void setOrganizationURL(String organizationURL) {
        this.organizationURL = organizationURL;
    }
}
