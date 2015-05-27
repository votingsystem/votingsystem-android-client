package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.signature.util.CertUtils;

import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceVSDto {

    public enum Type {MOBILE, PC}

    private Long id;
    private String deviceId;
    private String sessionId;
    private String deviceName;
    private String email;
    private String phone;
    private String certPEM;
    private String firstName;
    private String lastName;
    private String NIF;
    private Type deviceType;
    @JsonIgnore private X509Certificate x509Certificate;

    public DeviceVSDto() {}

    public DeviceVSDto(Long id, String name) {
        this.setId(id);
        this.setDeviceName(name);
    }

    public DeviceVSDto(String deviceId, String sessionId) {
        this.setDeviceId(deviceId);
        this.setSessionId(sessionId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCertPEM() {
        return certPEM;
    }

    public void setCertPEM(String certPEM) {
        this.certPEM = certPEM;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Type deviceType) {
        this.deviceType = deviceType;
    }

    public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate == null && certPEM != null) x509Certificate =
                CertUtils.fromPEMToX509CertCollection(certPEM.getBytes()).iterator().next();
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

}
