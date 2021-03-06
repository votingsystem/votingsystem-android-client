package org.votingsystem.dto;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificateDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type {
        VOTE_ROOT, VOTE, USER, CERTIFICATE_AUTHORITY, ACTOR_VS,
        ANONYMOUS_REPRESENTATIVE_DELEGATION, CURRENCY, TIMESTAMP_SERVER
    }

    public enum State {OK, ERROR, CANCELLED, USED, UNKNOWN}

    private String x509CertificatePEM;

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

    public CertificateDto() {
    }

    public CertificateDto(X509Certificate x509Cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        serialNumber = x509Cert.getSerialNumber().toString();
        isRoot = CertUtils.isSelfSigned(x509Cert);
        pemCert = new String(PEMUtils.getPEMEncoded(x509Cert), "UTF-8");
        subjectDN = x509Cert.getSubjectDN().toString();
        issuerDN = x509Cert.getIssuerDN().toString();
        sigAlgName = x509Cert.getSigAlgName();
        notBefore = x509Cert.getNotBefore();
        notAfter = x509Cert.getNotAfter();
    }

    X509Certificate getX509Cert() throws Exception {
        return PEMUtils.fromPEMToX509CertCollection(pemCert.getBytes()).iterator().next();
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

    public CertificateDto.Type getType() {
        return type;
    }

    public CertificateDto.State getState() {
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
