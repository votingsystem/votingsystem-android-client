package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.crypto.PEMUtils;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {MOBILE, PC}

    private Long id;
    private String deviceId;
    private String sessionId;
    private String deviceName;
    private String email;
    private String phone;
    private String publicKeyPEM;
    private String x509CertificatePEM;
    private String firstName;
    private String lastName;
    private String IBAN;
    private String NIF;
    private Type deviceType;
    @JsonIgnore private X509Certificate x509Certificate;
    @JsonIgnore private PublicKey publicKey;

    public DeviceDto() {}

    public DeviceDto(Long id) {
        this.setId(id);
    }

    public DeviceDto(Long id, String name) {
        this.setId(id);
        this.setDeviceName(name);
    }

    public DeviceDto(String deviceId, String sessionId) {
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

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
    }

    public String getSessionId() {
        return sessionId;
    }

    public DeviceDto setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
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

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    @JsonIgnore public X509Certificate getX509Certificate() throws Exception {
        if(x509Certificate == null && x509CertificatePEM != null) x509Certificate =
                PEMUtils.fromPEMToX509CertCollection(x509CertificatePEM.getBytes()).iterator().next();
        return x509Certificate;
    }

    public void setX509Cert(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

}
