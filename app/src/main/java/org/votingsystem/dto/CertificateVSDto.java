package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.signature.util.CertUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateVSDto {

    public enum Type {VOTEVS_ROOT, VOTEVS, USER, CERTIFICATE_AUTHORITY, ACTOR_VS,
        ANONYMOUS_REPRESENTATIVE_DELEGATION, CURRENCY, TIMESTAMP_SERVER}

    public enum State {OK, ERROR, CANCELLED, USED, UNKNOWN}

    //SerialNumber as String to avoid Javascript problem handling such big numbers
    private String serialNumber;
    private String issuerSerialNumber;
    private String description;
    private String pemCert;
    private String subjectDN;
    private String issuerDN;
    private String sigAlgName;
    private Date notBefore;
    private Date notAfter;
    private byte[] content;
    private Date dateCreated;
    private Date lastUpdated;
    private Type type;
    private State state;
    private boolean isRoot;

    public CertificateVSDto() {}

    public CertificateVSDto(X509Certificate x509Cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        serialNumber = x509Cert.getSerialNumber().toString();
        isRoot = CertUtils.isSelfSigned(x509Cert);
        pemCert = new String(CertUtils.getPEMEncoded (x509Cert), "UTF-8");
        subjectDN = x509Cert.getSubjectDN().toString();
        issuerDN = x509Cert.getIssuerDN().toString();
        sigAlgName = x509Cert.getSigAlgName();
        notBefore = x509Cert.getNotBefore();
        notAfter = x509Cert.getNotAfter();
    }

    @JsonIgnore X509Certificate getX509Cert() throws Exception {
        return CertUtils.fromPEMToX509CertCollection(pemCert.getBytes()).iterator().next();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getIssuerSerialNumber() {
        return issuerSerialNumber;
    }

    public String getDescription() {
        return description;
    }

    public String getPemCert() {
        return pemCert;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public String getSigAlgName() {
        return sigAlgName;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public org.votingsystem.dto.CertificateVSDto.Type getType() {
        return type;
    }

    public org.votingsystem.dto.CertificateVSDto.State getState() {
        return state;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setState(State state) {
        this.state = state;
    }

}
