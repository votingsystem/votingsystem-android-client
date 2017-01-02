package org.votingsystem.dto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertExtensionDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String deviceId;
    private String deviceName;
    private String email;
    private String mobilePhone;
    private String nif;
    private String givenname;
    private String surname;
    private DeviceDto.Type deviceType;


    public CertExtensionDto() {}

    public CertExtensionDto(String deviceId, String deviceName, String email, String phone, DeviceDto.Type deviceType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public DeviceDto.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceDto.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPrincipal() {
        return "SERIALNUMBER=" + nif + ", GIVENNAME=" + givenname + ", SURNAME=" + surname;
    }

    public static CertExtensionDto fromJson(String jsonStr) throws JSONException {
        CertExtensionDto result = new CertExtensionDto();
        JSONObject reader = new JSONObject(jsonStr);
        result.setDeviceId(reader.getString("deviceId"));
        result.setDeviceName(reader.getString("deviceName"));
        result.setEmail(reader.getString("email"));
        result.setMobilePhone(reader.getString("mobilePhone"));
        result.setNif(reader.getString("nif"));
        result.setGivenname(reader.getString("givenname"));
        result.setSurname(reader.getString("surname"));
        String deviceType = reader.getString("deviceType");
        if(deviceType != null)
            result.setDeviceType(DeviceDto.Type.valueOf(deviceType));
        return result;
    }

}
