package org.votingsystem.util.crypto;

import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.openssl.PEMWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMUtils {

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

    public static String getPEMEncodedStr (Object objectToEncode) throws IOException {
        return new String(PEMUtils.getPEMEncoded(objectToEncode));
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

    public static PublicKey fromPEMToRSAPublicKey(byte[] pemBytes) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        PEMReader pemReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(pemBytes)));
        RSAPublicKey jcerSAPublicKey = (RSAPublicKey) pemReader.readObject();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(jcerSAPublicKey.getEncoded());
        PublicKey publicKey = factory.generatePublic(pubKeySpec);
        return publicKey;
    }

    public static PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (byte[] csrBytes) throws Exception {
        PEMReader pemReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(csrBytes)));
        PKCS10CertificationRequest result = (PKCS10CertificationRequest)pemReader.readObject();
        pemReader.close();
        return result;
    }
}
