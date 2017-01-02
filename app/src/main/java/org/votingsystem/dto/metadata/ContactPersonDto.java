package org.votingsystem.dto.metadata;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactPersonDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type {
        TECHNICAL;
    }

    private Type contactType;
    private String company;
    private String givenName;
    private String surName;
    private String emailAddress;
    private String telephoneNumber;

    public ContactPersonDto() {
    }

    public ContactPersonDto(String company, String givenName, String surName, String emailAddress,
                            String telephoneNumber, Type contactType) {
        this.company = company;
        this.givenName = givenName;
        this.surName = surName;
        this.emailAddress = emailAddress;
        this.telephoneNumber = telephoneNumber;
        this.contactType = contactType;
    }

    public Type getContactType() {
        return contactType;
    }

    public String getCompany() {
        return company;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSurName() {
        return surName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setContactType(Type contactType) {
        this.contactType = contactType;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

}
