package org.votingsystem.util.crypto;

import android.util.Log;

import org.bouncycastle2.asn1.ASN1Object;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x509.BasicConstraints;
import org.bouncycastle2.asn1.x509.KeyUsage;
import org.bouncycastle2.asn1.x509.X509Extension;
import org.bouncycastle2.asn1.x509.X509Extensions;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.X509v1CertificateBuilder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.jce.PrincipalUtil;
import org.bouncycastle2.jce.X509Principal;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle2.x509.X509V1CertificateGenerator;
import org.bouncycastle2.x509.X509V3CertificateGenerator;
import org.bouncycastle2.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle2.x509.extension.SubjectKeyIdentifierStructure;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import custom.org.apache.harmony.security.asn1.ASN1Primitive;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
* Casi todo el código sacado de:
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtils {

    private static final String TAG = CertUtils.class.getSimpleName();
    
    public static String ROOT_ALIAS = "root";
    public static String END_ENTITY_ALIAS = "end";
    public static final int PERIODO_VALIDEZ = 7 * 24 * 60 * 60 * 1000;
    
    static public String SIG_ALGORITHM = "SHA1WithRSAEncryption";

    /**
     * Generate V3 Certificate from CSR
     */
    public static X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit,
            PrivateKey caKey, Date dateBegin, Date dateFinish,
            DERTaggedObject... certExtensions) throws Exception {
        String strSubjectDN = csr.getCertificationRequestInfo().getSubject().toString();
        if (!csr.verify() || strSubjectDN == null) throw new Exception("ERROR VERIFYING CSR");
        if(organizationalUnit != null) strSubjectDN = organizationalUnit + "," + strSubjectDN;
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey requestPublicKey = csr.getPublicKey();
        X509Principal x509Principal = new X509Principal(strSubjectDN);

        certGen.setIssuerDN(csr.getCertificationRequestInfo().getSubject());
        certGen.setSubjectDN(csr.getCertificationRequestInfo().getSubject());

        certGen.setSerialNumber(KeyGeneratorVS.INSTANCE.getSerno());
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(requestPublicKey);
        certGen.setSignatureAlgorithm(ContextVS.SIGNATURE_ALGORITHM);
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(requestPublicKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true,
                new BasicConstraints(false));//Certificado final
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        ASN1Set attributes = csr.getCertificationRequestInfo().getAttributes();
        if (attributes != null) {
            for (int i = 0; i != attributes.size(); i++) {
                if(attributes.getObjectAt(i) instanceof DERTaggedObject) {
                    DERTaggedObject taggedObject = (DERTaggedObject)attributes.getObjectAt(i);
                    ASN1ObjectIdentifier oid = new  ASN1ObjectIdentifier(ContextVS.VOTING_SYSTEM_BASE_OID +
                            taggedObject.getTagNo());
                    certGen.addExtension(oid, true, taggedObject);
                } else {
                    Attribute attr = Attribute.getInstance(attributes.getObjectAt(i));
                    if (attr.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                        X509Extensions extensions = X509Extensions.getInstance(attr.getAttrValues().getObjectAt(0));
                        Enumeration e = extensions.oids();
                        while (e.hasMoreElements()) {
                            DERObjectIdentifier oid = (DERObjectIdentifier) e.nextElement();
                            X509Extension ext = extensions.getExtension(oid);
                            certGen.addExtension(oid, ext.isCritical(), ext.getValue().getOctets());
                        }
                    }
                }
            }
        }
        if(certExtensions != null) {
            for(DERTaggedObject taggedObject: certExtensions) {
                if(taggedObject != null) {
                    ASN1ObjectIdentifier oid = new  ASN1ObjectIdentifier(ContextVS.VOTING_SYSTEM_BASE_OID +
                            taggedObject.getTagNo());
                    certGen.addExtension(oid, true, taggedObject);
                } LOGD(TAG, "null taggedObject");
            }
        }
        return certGen.generate(caKey, ContextVS.PROVIDER);
    }

    /**
     * Genera un certificado V1 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV1RootCert(KeyPair pair, 
    		long comienzo, int periodoValidez, String principal) throws Exception {
        X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal(principal));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(new X500Principal(principal));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(SIG_ALGORITHM);
        return certGen.generate(pair.getPrivate(), "BC");
    }
    
    public static X509Certificate[] generateCertificate(KeyPair keyPair,Date fechaIncio, 
    		Date dateFinish, String principal) throws Exception {
		X509v1CertificateBuilder certGen = new JcaX509v1CertificateBuilder(
	   			new X500Name(principal),
	   			BigInteger.valueOf(System.currentTimeMillis()),
	   			fechaIncio, dateFinish, new X500Name(principal),
	   			keyPair.getPublic());
   		JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withRSA");
   		ContentSigner contentSigner = contentSignerBuilder.build(keyPair.getPrivate());  
   		X509CertificateHolder certHolder = certGen.build(contentSigner); 
   		JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
   		X509Certificate cert = certConverter.getCertificate(certHolder);
   		X509Certificate[] certs = {cert};
   		return certs;
	}
    
    /**
     * Genera un certificado V3 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, 
    		long comienzo, int periodoValidez, String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        Log.i("CertUitil.strSubjectDN", strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);          
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(SIG_ALGORITHM);
        //The following fragment shows how to create one which indicates that 
        //the certificate containing it is a CA and that only one certificate can follow in the certificate path.
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(pair.getPublic()));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return certGen.generate(pair.getPrivate(), "BC");
    }

    /**
     * Genera un certificado V3 para usarlo como certificado de usuario final
     */
    public static X509Certificate generateEndEntityCert(PublicKey entityKey, 
    		PrivateKey caKey, X509Certificate caCert, long comienzo, int periodoValidez,
                String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm("SHA1WithRSAEncryption");        
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return certGen.generate(caKey, "BC");
    }

   public static CertPath verifyCertificate(X509Certificate cert, CertStore store, KeyStore trustedStore)
           throws InvalidAlgorithmParameterException, KeyStoreException, CertPathBuilderException,
           NoSuchProviderException, NoSuchAlgorithmException {

        if (cert == null || store == null || trustedStore == null) 
            throw new IllegalArgumentException("cert == "+cert+", store == "+store+", trustedStore == "+trustedStore);
        CertPathBuilder pathBuilder;
        // I create the CertPathBuilder object. It will be used to find a
        // certification path that starts from the signer's certificate and
        // leads to a trusted root certificate.
       pathBuilder = CertPathBuilder.getInstance("PKIX", "BC");

        X509CertSelector xcs = new X509CertSelector();
        xcs.setCertificate(cert);
        //PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints) 
        PKIXBuilderParameters params = new PKIXBuilderParameters(trustedStore, xcs);
        params.addCertStore(store);
        params.setRevocationEnabled(false);
        try {
            CertPathBuilderResult result = pathBuilder.build(params);
            CertPath path = result.getCertPath();
            return path;
        } catch (CertPathBuilderException e) {
            // A certification path is not found, so null is returned.
            return null;
        }
    }

    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
                  boolean checkCRL, List<X509Certificate> certs) throws ExceptionVS {
        return verifyCertificate(anchors, checkCRL, certs, Calendar.getInstance().getTime());
    }

    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
          boolean checkCRL, List<X509Certificate> certs, Date signingDate) throws ExceptionVS {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(anchors);
            pkixParameters.setDate(signingDate);
            pkixParameters.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", ContextVS.PROVIDER);
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            CertPath certPath = certFact.generateCertPath(certs);
            CertPathValidatorResult result = certPathValidator.validate(certPath, pkixParameters);
            // Get the CA used to validate this path
            //PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult)result;
            //TrustAnchor ta = pkixResult.getTrustAnchor();
            //X509Certificate certCaResult = ta.getTrustedCert();
            //log.debug("certCaResult: " + certCaResult.getSubjectDN().toString()+
            //        "- serialNumber: " + certCaResult.getSerialNumber().longValue());
            return (PKIXCertPathValidatorResult)result;
        } catch(Exception ex) {
            String msg = "Empty cert list";
            if(certs != null && !certs.isEmpty()) msg = ex.getMessage() + " - cert: " +
                    certs.iterator().next().getSubjectDN();
            throw new ExceptionVS(msg, ex);
        }
    }

    public static <T> T getCertExtensionData(Class<T> type, X509Certificate x509Certificate,
                                             String extensionOID) throws Exception {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        DERSet derSet = (DERSet) X509ExtensionUtil.fromExtensionValue(extensionValue);
        String extensionData = ((DERUTF8String) derSet.getObjectAt(0)).getString();
        return JSON.readValue(extensionData, type);
    }

    public static String getCertExtensionData(X509Certificate x509Certificate, String extensionOID) throws Exception {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        ASN1Object asn1Object = X509ExtensionUtil.fromExtensionValue(extensionValue);
        if(asn1Object instanceof ASN1Set) {
            return ((ASN1Set) asn1Object).getObjectAt(0).toString();
        }
        return null;
    }

    public static <T> T getCertExtensionData(Class<T> type, PKCS10CertificationRequest csr, String oid) throws Exception {
        ASN1Set asn1Set = csr.getCertificationRequestInfo().getAttributes();
        if(asn1Set.getObjects() != null) {
            Enumeration ASN1SetEnum = asn1Set.getObjects();
            while(ASN1SetEnum.hasMoreElements()) {
                Object object = ASN1SetEnum.nextElement();
                if(object instanceof org.bouncycastle2.asn1.pkcs.Attribute) {
                    Attribute attribute = (Attribute) object;
                    if(attribute.getAttrType().getId().equals(oid)) {
                        String certAttributeJSONStr = ((DERUTF8String)attribute.getAttrValues().getObjectAt(0)).getString();
                        return JSON.getMapper().readValue(certAttributeJSONStr, type);
                    }
                }
            }
        }
        LOGD(TAG, "missing attribute with oid: " + oid);
        return null;
    }

    public static X509Certificate loadCertificate (byte[] certBytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(certBytes);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(inputStream);
        X509Certificate x509Cert = certificateChain.iterator().next();
        inputStream.close();
        return x509Cert;
    }

    /**
     * Checks whether given X.509 certificate is self-signed.
     *
     * http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
     */
    public static boolean isSelfSigned(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (SignatureException sigEx) {
            // Invalid signature --> not self-signed
            return false;
        } catch (InvalidKeyException keyEx) {
            // Invalid key --> not self-signed
            return false;
        }
    }

}