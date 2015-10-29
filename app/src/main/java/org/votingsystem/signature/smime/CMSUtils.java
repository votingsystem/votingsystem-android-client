package org.votingsystem.signature.smime;

import android.util.Log;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.BEROctetStringGenerator;
import org.bouncycastle2.asn1.DERNull;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.cms.ContentInfo;
import org.bouncycastle2.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle2.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle2.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.util.Store;
import org.bouncycastle2.util.encoders.Base64;
import org.bouncycastle2.util.io.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import static org.votingsystem.util.ContextVS.PROVIDER;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CMSUtils {

    public static final String TAG = CMSUtils.class.getSimpleName();

    public static final String  DIGEST_SHA1 = OIWObjectIdentifiers.idSHA1.getId();
    public static final String  DIGEST_SHA224 = NISTObjectIdentifiers.id_sha224.getId();
    public static final String  DIGEST_SHA256 = NISTObjectIdentifiers.id_sha256.getId();
    public static final String  DIGEST_SHA384 = NISTObjectIdentifiers.id_sha384.getId();
    public static final String  DIGEST_SHA512 = NISTObjectIdentifiers.id_sha512.getId();
    public static final String  DIGEST_MD5 = PKCSObjectIdentifiers.md5.getId();
    public static final String  DIGEST_GOST3411 = CryptoProObjectIdentifiers.gostR3411.getId();
    public static final String  DIGEST_RIPEMD128 = TeleTrusTObjectIdentifiers.ripemd128.getId();
    public static final String  DIGEST_RIPEMD160 = TeleTrusTObjectIdentifiers.ripemd160.getId();
    public static final String  DIGEST_RIPEMD256 = TeleTrusTObjectIdentifiers.ripemd256.getId();

    public static final String  ENCRYPTION_RSA = PKCSObjectIdentifiers.rsaEncryption.getId();
    public static final String  ENCRYPTION_DSA = X9ObjectIdentifiers.id_dsa_with_sha1.getId();
    public static final String  ENCRYPTION_ECDSA = X9ObjectIdentifiers.ecdsa_with_SHA1.getId();
    public static final String  ENCRYPTION_RSA_PSS = PKCSObjectIdentifiers.id_RSASSA_PSS.getId();
    public static final String  ENCRYPTION_GOST3410 = CryptoProObjectIdentifiers.gostR3410_94.getId();
    public static final String  ENCRYPTION_ECGOST3410 = CryptoProObjectIdentifiers.gostR3410_2001.getId();

    private static final String  ENCRYPTION_ECDSA_WITH_SHA1 = X9ObjectIdentifiers.ecdsa_with_SHA1.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA224 = X9ObjectIdentifiers.ecdsa_with_SHA224.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA256 = X9ObjectIdentifiers.ecdsa_with_SHA256.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA384 = X9ObjectIdentifiers.ecdsa_with_SHA384.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA512 = X9ObjectIdentifiers.ecdsa_with_SHA512.getId();

    private static final Runtime RUNTIME = Runtime.getRuntime();

    static int getMaximumMemory() {
        long maxMem = RUNTIME.maxMemory();
        if (maxMem > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)maxMem;
    }

    static ContentInfo readContentInfo(byte[] input) throws CMSException {
        // enforce limit checking as from a byte array
        return readContentInfo(new ASN1InputStream(input));
    }

    static ContentInfo readContentInfo(InputStream input) throws CMSException {
        // enforce some limit checking
        return readContentInfo(new ASN1InputStream(input, getMaximumMemory()));
    }

    static OutputStream createBEROctetOutputStream(OutputStream s,
            int tagNo, boolean isExplicit, int bufferSize) throws IOException {
        BEROctetStringGenerator octGen = new BEROctetStringGenerator(s, tagNo, isExplicit);
        if (bufferSize != 0) {
            return octGen.getOctetOutputStream(new byte[bufferSize]);
        }
        return octGen.getOctetOutputStream();
    }


    private static ContentInfo readContentInfo(ASN1InputStream in) throws CMSException {
        try {
            return ContentInfo.getInstance(in.readObject());
        } catch (IOException e) {
            throw new CMSException("IOException reading content.", e);
        } catch (ClassCastException e) {
            throw new CMSException("Malformed content.", e);
        } catch (IllegalArgumentException e) {
            throw new CMSException("Malformed content.", e);
        }
    }

    public static byte[] streamToByteArray(InputStream in) throws IOException {
        return Streams.readAll(in);
    }

    public static byte[] streamToByteArray(InputStream in, int limit) throws IOException {
        return Streams.readAllLimited(in, limit);
    }

    public static Provider getProvider(String providerName) throws NoSuchProviderException {
        if (providerName != null) {
            Provider prov = Security.getProvider(providerName);
            if (prov != null) {
                return prov;
            }
            throw new NoSuchProviderException("provider " + providerName + " not found.");
        }
        return null;
    }

    /**
     * verify that the sig is correct and that it was generated when the
     * certificate was current(assuming the cert is contained in the message).
     */
    public static boolean isValidSignature(SMIMESigned smimeSigned) throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore signers = smimeSigned.getSignerInfos();
        Log.d(TAG + ".isValidSignature ", "signers.size(): " + signers.size());
        Iterator it = signers.getSigners().iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection certCollection = certs.getMatches(signer.getSID());
            Log.d(TAG + ".isValidSignature ", "Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(
                    (X509CertificateHolder)certIt.next());
            Log.d(TAG + ".isValidSignature ", "cert.getSubjectDN(): " + cert.getSubjectDN());
            Log.d(TAG + ".isValidSignature ", "cert.getNotBefore(): " + cert.getNotBefore());
            Log.d(TAG + ".isValidSignature ", "cert.getNotAfter(): " + cert.getNotAfter());

            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(PROVIDER).build(cert))){
                Log.d(TAG + ".isValidSignature ", "signature verified");
                result = true;
            } else {
                Log.d(TAG + ".isValidSignature ", "signature failed!");
                result = false;
            }
        }
        return result;
    }

   /**
    * Sacado de http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
    * Checks whether given X.509 certificate is self-signed.
    */
    public static boolean isSelfSigned(X509Certificate cert) throws Exception {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = (PublicKey) cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (Exception keyEx) {
            // Invalid key –> not self-signed
            return false;
        }
    }

    public static String getDigestId (String digestAlgOID) {
        if (DIGEST_SHA1.equals(digestAlgOID)) return "SHA1";
        if (DIGEST_SHA224.equals(digestAlgOID)) return "SHA224";
        if (DIGEST_SHA256.equals(digestAlgOID)) return "SHA256";
        if (DIGEST_SHA384.equals(digestAlgOID)) return "SHA384";
        if (DIGEST_SHA512.equals(digestAlgOID)) return "SHA512";
        if (DIGEST_MD5.equals(digestAlgOID)) return "MD5";
        if (DIGEST_GOST3411.equals(digestAlgOID)) return "GOST3411";
        if (DIGEST_RIPEMD128.equals(digestAlgOID)) return "RIPEMD128";
        if (DIGEST_RIPEMD160.equals(digestAlgOID)) return "RIPEMD160";
        if (DIGEST_RIPEMD256.equals(digestAlgOID)) return "RIPEMD256";
        return null;
    }

    public static String getEncryptiontId (String encryptionAlgOID) {
        if (ENCRYPTION_RSA.equals(encryptionAlgOID)) return "RSA";
        if (ENCRYPTION_DSA.equals(encryptionAlgOID)) return "DSA";
        if (ENCRYPTION_ECDSA.equals(encryptionAlgOID)) return "ECDSA";
        if (ENCRYPTION_RSA_PSS.equals(encryptionAlgOID)) return "RSA_PSS";
        if (ENCRYPTION_GOST3410.equals(encryptionAlgOID)) return "GOST3410";
        if (ENCRYPTION_ECGOST3410.equals(encryptionAlgOID)) return "ECGOST3410";
        return null;
    }
    
    public static String getHashBase64 (String originStr, String digestAlgorithm)
            throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
        byte[] resultDigest =  sha.digest(originStr.getBytes());
        return new String(Base64.encode(resultDigest));
    }

    public static AlgorithmIdentifier fixAlgID(AlgorithmIdentifier algId) {
        if (algId.getParameters() == null) {
            return new AlgorithmIdentifier(algId.getObjectId(), DERNull.INSTANCE);
        }
        return algId;
    }

    public static byte[] getSignerDigest(SignerInformation signer) throws CMSException {
        Attribute hash = signer.getSignedAttributes().get(CMSAttributes.messageDigest);
        return ((ASN1OctetString)hash.getAttrValues().getObjectAt(0)).getOctets();
    }

    public static DERObject getSingleValuedSignedAttribute(AttributeTable signedAttrTable,
                                                           DERObjectIdentifier attrOID, String printableName) throws CMSException {
        if (signedAttrTable == null) return null;
        ASN1EncodableVector vector = signedAttrTable.getAll(attrOID);
        switch (vector.size()) {
            case 0:
                return null;
            case 1:
                Attribute t = (Attribute)vector.get(0);
                ASN1Set attrValues = t.getAttrValues();
                if (attrValues.size() != 1) throw new CMSException("A " + printableName +
                        " attribute MUST have a single attribute value");
                return attrValues.getObjectAt(0).getDERObject();
            default: throw new CMSException(
                    "The SignedAttributes in a signerInfo MUST NOT include multiple instances of the "
                            + printableName + " attribute");
        }
    }

}
