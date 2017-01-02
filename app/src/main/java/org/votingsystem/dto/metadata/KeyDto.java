package org.votingsystem.dto.metadata;

import android.util.Base64;

import org.votingsystem.crypto.CertUtils;

import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class KeyDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type {
        X509CERT;
    }

    public enum Use {ENCRYPTION, SIGN;}

    private Use use;
    private Type type;
    private String x509CertificateBase64;

    public KeyDto() {
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @param x509Cert
     * @param use
     * @throws CertificateEncodingException
     */
    public KeyDto(Type type, X509Certificate x509Cert, Use use) throws CertificateEncodingException {
        this.type = type;
        this.x509CertificateBase64 = Base64.encodeToString(x509Cert.getEncoded(), Base64.NO_WRAP);
        this.use = use;
    }

    public KeyDto(Type type, String x509CertificateBase64, Use use) throws CertificateEncodingException {
        this.type = type;
        this.x509CertificateBase64 = x509CertificateBase64;
        this.use = use;
    }

    public Use getUse() {
        return use;
    }

    public X509Certificate getX509Certificate() throws Exception {
        if (x509CertificateBase64 == null)
            return null;
        else {
            byte[] certEncoded = org.bouncycastle2.util.encoders.Base64.decode
                    (x509CertificateBase64.getBytes());
            return CertUtils.loadCertificate(certEncoded);
        }
    }


    public void setUse(Use use) {
        this.use = use;
    }

    public String getX509CertificateBase64() {
        return x509CertificateBase64;
    }

    public void setX509CertificateBase64(String x509CertificateBase64) {
        this.x509CertificateBase64 = x509CertificateBase64;
    }
}
