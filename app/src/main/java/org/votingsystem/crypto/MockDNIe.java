package org.votingsystem.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MockDNIe {

    public static final String END_ENTITY_ALIAS = "userkey";
    public static final String PASSWORD = "local-demo";

    private X509Certificate x509Certificate;
    private Certificate[] certificateChain;
    private PrivateKey privateKey;
    private KeyStore keyStore;

    public MockDNIe(InputStream keyStoreInputStream) throws IOException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableEntryException {
        this.keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyStoreInputStream, PASSWORD.toCharArray());
        certificateChain = keyStore.getCertificateChain(END_ENTITY_ALIAS);
        x509Certificate = (X509Certificate) keyStore.getCertificate(END_ENTITY_ALIAS);
        privateKey = (PrivateKey) keyStore.getKey(END_ENTITY_ALIAS, PASSWORD.toCharArray());
    }

    public MockDNIe(PrivateKey privateKey, X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        this.privateKey = privateKey;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public List<X509Certificate> getX509CertificateChain() {
        List<X509Certificate> result = new ArrayList<>();
        for (Certificate cert : certificateChain) {
            result.add((X509Certificate) cert);
        }
        return result;
    }

    public Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public MockDNIe setCertificateChain(Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

}