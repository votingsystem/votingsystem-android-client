package org.votingsystem.signature.util;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.voting.AnonymousDelegationCertExtensionDto;
import org.votingsystem.dto.voting.VoteCertExtensionDto;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import static org.votingsystem.util.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CertificationRequestVS implements java.io.Serializable {

    public static final String TAG = CertificationRequestVS.class.getSimpleName();

    private static final long serialVersionUID = 1L;
    

    private transient PKCS10CertificationRequest csr;
    private transient SignedMailGenerator signedMailGenerator;
    private transient KeyPair keyPair;
    private String hashPin;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;
    private byte[] csrPEM;

    private CertificationRequestVS(KeyPair keyPair, PKCS10CertificationRequest csr,
            String signatureMechanism) throws IOException {
        this.keyPair = keyPair;
        this.csr = csr;
        this.csrPEM = CertUtils.getPEMEncoded(getCsr());
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequestVS getVoteRequest(String signatureMechanism,
            String provider, String accessControlURL, Long eventId, String hashCertVS) throws
            NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        VoteCertExtensionDto dto = new VoteCertExtensionDto(accessControlURL, hashCertVS, eventId);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.VOTE_TAG,
                new DERUTF8String(JSON.writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getAnonymousDelegationRequest(
            String signatureMechanism, String provider, String accessControlURL, String hashCertVS,
            Integer weeksOperationActive, Date validFrom, Date validTo) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +
                ", OU=AnonymousRepresentativeDelegation");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        AnonymousDelegationCertExtensionDto dto = new AnonymousDelegationCertExtensionDto(accessControlURL, hashCertVS,
                weeksOperationActive, validFrom, validTo);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getCurrencyRequest(
            String signatureMechanism, String provider, String currencyServerURL, String hashCertVS,
            BigDecimal amount, String currencyCode, Boolean timeLimited, String tagVS) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        tagVS = (tagVS == null)? TagVSDto.WILDTAG:tagVS.trim();
        X500Principal subject = new X500Principal("CN=currencyServerURL:" + currencyServerURL +
                ", OU=CURRENCY_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currencyCode +
                ", OU=TAG:" + tagVS + ", OU=DigitalCurrency");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        CurrencyCertExtensionDto dto = new CurrencyCertExtensionDto(amount, currencyCode, hashCertVS,
                currencyServerURL, timeLimited, tagVS);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.CURRENCY_TAG,
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getUserRequest (String signatureMechanism, String provider,
             String nif, String email, String phone, String deviceId,
             String givenName, String surName, DeviceVSDto.Type deviceType)
            throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGeneratorVS.INSTANCE.genKeyPair();
        String principal = "SERIALNUMBER=" + nif + ", GIVENNAME=" + givenName + ", SURNAME=" + surName;
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        CertExtensionDto dto = new CertExtensionDto(deviceId, Utils.getDeviceName(),
                email, phone, deviceType);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.DEVICEVS_TAG,
                new DERUTF8String(JSON.writeValueAsString(dto))));
        X500Principal subject = new X500Principal(principal);
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public CertificationRequestVS initSigner (byte[] signedCsr) {
        this.signedCsr = signedCsr;
        return this;
    }

    public SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
                                 String subject) throws Exception {
        return getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject);
    }

    private SignedMailGenerator getSignedMailGenerator() throws Exception {
        if (signedMailGenerator == null) {
            Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(signedCsr);
            LOGD(TAG + "getSignedMailGenerator()", "Num certs: " + certificates.size());
            if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
            certificate = certificates.iterator().next();
            X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
            certificates.toArray(arrayCerts);
            signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
            signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
        }
        return signedMailGenerator;
    }

    public X509Certificate getCertificate() {
        if(certificate == null && signedCsr != null) {
            try {
                Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(signedCsr);
                LOGD(TAG + "getSignedMailGenerator()", "Num certs: " + certificates.size());
                if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
                certificate = certificates.iterator().next();
            } catch(Exception ex) { ex.printStackTrace();  }
        }
        return certificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }


    public byte[] getCsrPEM() throws Exception {
        return csrPEM;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(certificate != null) s.writeObject(certificate.getEncoded());
            else s.writeObject(null);
            if(keyPair != null) {//this is to deserialize private keys outside android environments
                s.writeObject(keyPair.getPublic().getEncoded());
                s.writeObject(keyPair.getPrivate().getEncoded());
            } else {
                s.writeObject(null);
                s.writeObject(null);
            }
        } catch(Exception ex) { ex.printStackTrace(); }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                certificate = CertUtils.loadCertificate(certificateBytes);
            } catch(Exception ex) { ex.printStackTrace(); }
        }
        try {
            Object temp = null;
            PublicKey publicKey = null;
            PrivateKey privateKey = null;
            if((temp = s.readObject()) != null) publicKey =  KeyFactory.getInstance("RSA").
                    generatePublic(new X509EncodedKeySpec((byte[]) temp));
            if((temp = s.readObject()) != null) privateKey =  KeyFactory.getInstance("RSA").
                    generatePrivate(new PKCS8EncodedKeySpec((byte[]) temp));
            if(privateKey != null && publicKey != null) keyPair = new KeyPair(publicKey, privateKey);
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    private PKCS10CertificationRequest getCsr() {
        if(csr == null && signedCsr != null) {
            csr = new PKCS10CertificationRequest(signedCsr);
        }
        return csr;
    }

    public String getHashPin() {
        return hashPin;
    }

    public void setHashPin(String hashPin) {
        this.hashPin = hashPin;
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    public void setSignedCsr(byte[] signedCsr) {
        this.signedCsr = signedCsr;
    }
}