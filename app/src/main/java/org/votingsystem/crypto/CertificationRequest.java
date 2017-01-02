package org.votingsystem.crypto;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.DERBoolean;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.asn1.pkcs.Attribute;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.votingsystem.util.Constants;
import org.votingsystem.xades.SignatureAlgorithm;
import org.votingsystem.xades.SignatureBuilder;
import org.votingsystem.xades.XAdESUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequest implements java.io.Serializable {

    public static final String TAG = CertificationRequest.class.getSimpleName();

    private static final long serialVersionUID = 1L;


    private transient PKCS10CertificationRequest csr;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequest(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequest getVoteRequest(String indentityServiceEntity, String votingServiceEntity,
                                       String electionUUID, String revocationHashBase64) throws
            NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException {
        KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
        X500Name x500Name = new X500Name("CN=identityService:" + indentityServiceEntity +
                ";votingService:" + votingServiceEntity + ", OU=electionUUID:" + electionUUID);
        X500Principal subject = new X500Principal(x500Name.getEncoded());
        String certVoteExtension = certVoteExtensionJson(indentityServiceEntity, votingServiceEntity,
                revocationHashBase64, electionUUID);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(new Attribute(new ASN1ObjectIdentifier(Constants.VOTE_OID),
                new DERSet(new DERUTF8String(certVoteExtension))));
        asn1EncodableVector.add(new Attribute(new ASN1ObjectIdentifier(Constants.ANON_CERT_OID),
                new DERSet(new DERBoolean(true))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Constants.SIGNATURE_ALGORITHM, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), Constants.PROVIDER);
        return new CertificationRequest(keyPair, csr, Constants.SIGNATURE_ALGORITHM);
    }

    public static String certVoteExtensionJson(String identityServiceEntity, String votingServiceEntity,
            String revocationHashBase64, String electionUUID) {
        return "{\"identityServiceEntity\":\"" + identityServiceEntity + "\",\"votingServiceEntity\":\"" +
                votingServiceEntity + "\"," + "\"revocationHashBase64\":\"" + revocationHashBase64 +
                "\",\"electionUUID\":\"" + electionUUID + "\"}";
    }

    public byte[] signDataWithTimeStamp(byte[] cotentToSign, String timeStampServiceURL) throws Exception {
        List<X509Certificate> certificates = new ArrayList<>(PEMUtils.fromPEMToX509CertCollection(signedCsr));
        X509Certificate issuedCert = PEMUtils.fromPEMToX509Cert(signedCsr);
        return new SignatureBuilder(cotentToSign, XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(),
                keyPair.getPrivate(), issuedCert, certificates, timeStampServiceURL).build();
    }

    public X509Certificate getCertificate() throws Exception {
        if(certificate == null)
            return PEMUtils.fromPEMToX509Cert(signedCsr);
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
        return PEMUtils.getPEMEncoded(csr);
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
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                certificate = CertUtils.loadCertificate(certificateBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] publicKeyBytes = (byte[]) s.readObject();
            PublicKey publicKey =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            byte[] privateKeyBytes = (byte[]) s.readObject();
            PrivateKey privateKey =  KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            if(privateKey != null && publicKey != null) keyPair = new KeyPair(publicKey, privateKey);
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    public CertificationRequest setSignedCsr(byte[] signedCsr) {
        this.signedCsr = signedCsr;
        return this;
    }


}