package org.votingsystem.signature.util;

import android.util.Log;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x509.BasicConstraints;
import org.bouncycastle2.asn1.x509.KeyUsage;
import org.bouncycastle2.asn1.x509.X509Extensions;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.X509v1CertificateBuilder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.jce.PrincipalUtil;
import org.bouncycastle2.jce.X509Principal;
import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.openssl.PEMWriter;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

import javax.mail.MessagingException;
import javax.security.auth.x500.X500Principal;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
* Casi todo el código sacado de:
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtils {
    
    public static String ROOT_ALIAS = "root";
    public static String END_ENTITY_ALIAS = "end";
    public static final int PERIODO_VALIDEZ = 7 * 24 * 60 * 60 * 1000;
    
    static public String SIG_ALGORITHM = "SHA1WithRSAEncryption";
    
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
        throws InvalidAlgorithmParameterException, KeyStoreException, MessagingException, CertPathBuilderException {
         
        if (cert == null || store == null || trustedStore == null) 
            throw new IllegalArgumentException("cert == "+cert+", store == "+store+", trustedStore == "+trustedStore);

        CertPathBuilder pathBuilder;

        // I create the CertPathBuilder object. It will be used to find a
        // certification path that starts from the signer's certificate and
        // leads to a trusted root certificate.
        try {
            pathBuilder = CertPathBuilder.getInstance("PKIX", "BC");
        } catch (Exception e) {
            throw new MessagingException("Error during the creation of the certpathbuilder.", e);
        }

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
        } catch (InvalidAlgorithmParameterException e) {
            // If this exception is thrown an error has occured during
            // certification path search. 
            throw new MessagingException("Error during the certification path search.", e);
        }
        
    }


    public static CertValidatorResultVS verifyCertificate(Set<TrustAnchor> anchors,
                  boolean checkCRL, List<X509Certificate> certs) throws ExceptionVS {
        return verifyCertificate(anchors, checkCRL, certs, Calendar.getInstance().getTime());
    }

    public static CertValidatorResultVS verifyCertificate(Set<TrustAnchor> anchors,
          boolean checkCRL, List<X509Certificate> certs, Date signingDate) throws ExceptionVS {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(anchors);
            pkixParameters.setDate(signingDate);
            CertExtensionCheckerVS checker = new CertExtensionCheckerVS();
            pkixParameters.addCertPathChecker(checker);
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
            return new CertValidatorResultVS(checker, (PKIXCertPathValidatorResult)result);
        } catch(Exception ex) {
            String msg = "Empty cert list";
            if(certs != null && !certs.isEmpty()) msg = ex.getMessage() + " - cert: " +
                    certs.iterator().next().getSubjectDN();
            throw new ExceptionVS(msg, ex);
        }
    }

    public static byte[] getPEMEncoded (Object objectToEncode) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        if(objectToEncode instanceof Collection) {
            Collection objectToEncodeColection = ((Collection)objectToEncode);
            for(Object object : objectToEncodeColection) {
                pemWrt.writeObject(object);
            }
        } else pemWrt.writeObject(objectToEncode);
        pemWrt.close();
        bOut.close();
        return bOut.toByteArray();
    }

    public static byte[] getPEMEncoded (X509Certificate certificate) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        pemWrt.writeObject(certificate);
        pemWrt.close();
        bOut.close();
        return bOut.toByteArray();
    }

    public static X509Certificate fromPEMToX509Cert (byte[] pemFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509","BC");
        X509Certificate x509Cert = (X509Certificate)fact.generateCertificate(in);
        return x509Cert;
    }
	
    public static Collection<X509Certificate> fromPEMToX509CertCollection (
    		byte[] pemChainFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemChainFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509","BC");
        Collection<X509Certificate> x509Certs = 
                (Collection<X509Certificate>)fact.generateCertificates(in);
        return x509Certs;
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

    public static <T> T getCertExtensionData(Class<T> type, X509Certificate x509Certificate,
                                             String extensionOID) throws Exception {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        DERTaggedObject derTaggedObject = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(extensionValue);
        String extensionData = ((DERUTF8String) derTaggedObject.getObject()).getString();
        return JSON.readValue(extensionData, type);
    }

    public static <T> T getCertExtensionData(Class<T> type, PKCS10CertificationRequest csr, int tagNo) throws Exception {
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects();
        T certExtensionDto = null;
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            if(attribute.getTagNo() == tagNo) {
                String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                certExtensionDto = JSON.getMapper().readValue(certAttributeJSONStr, type);
            }
        }
        return certExtensionDto;
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

    public static PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (byte[] csrBytes) throws Exception {
        PEMReader pemReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(csrBytes)));
        PKCS10CertificationRequest result = (PKCS10CertificationRequest)pemReader.readObject();
        pemReader.close();
        return result;
    }

    public static class CertValidatorResultVS {
        CertExtensionCheckerVS checker;
        PKIXCertPathValidatorResult result;

        public CertValidatorResultVS(CertExtensionCheckerVS checker, PKIXCertPathValidatorResult result) {
            this.checker = checker;
            this.result = result;
        }

        public CertExtensionCheckerVS getChecker() {return checker;}
        public PKIXCertPathValidatorResult getResult() {return result;}
    }
}