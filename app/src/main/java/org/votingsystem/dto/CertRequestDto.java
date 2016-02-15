package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertRequestDto {

    private String nif;
    private String email;
    private String phone;
    private String deviceId;
    private String givenName;
    private String surname;


    public CertRequestDto() {}

    public CertRequestDto(String deviceId, String phone, String givenName, String surname,
                          String nif, String email) {
        this.deviceId = deviceId;
        this.phone = phone;
        this.givenName = givenName;
        this.surname = surname;
        this.nif = nif;
        this.email = email;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

}
