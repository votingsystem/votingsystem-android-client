package org.votingsystem.cms;


import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedDataGenerator;
import org.bouncycastle2.cms.CMSTypedData;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle2.util.Store;
import org.votingsystem.util.crypto.CMSUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import static org.votingsystem.util.Constants.PROVIDER;
import static org.votingsystem.util.Constants.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.LogUtils.LOGD;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DNIeContentSigner implements ContentSigner {

    public static final String TAG = DNIeContentSigner.class.getSimpleName();

    private X509Certificate userCert;
    private Store cerStore;
    private SignatureOutputStream stream;
    private PrivateKey privateKey;


    private DNIeContentSigner(PrivateKey privateKey, X509Certificate userCert, Store cerStore) {
        this.privateKey = privateKey;
        this.userCert = userCert;
        this.cerStore = cerStore;
        stream = new SignatureOutputStream();
    }

    public X509Certificate getUserCert() {
        return userCert;
    }

    public Store getCerStore() {
        return cerStore;
    }

    @Override public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNATURE_ALGORITHM);
    }

    @Override public OutputStream getOutputStream() {
        return stream;
    }

    @Override public byte[] getSignature() {
        return stream.getSignature();
    }

    public void closeSession () {
        LOGD(TAG, "closeSession");
    }

    private class SignatureOutputStream extends OutputStream {
        ByteArrayOutputStream bOut;
        SignatureOutputStream() {
            bOut = new ByteArrayOutputStream();
        }
        public void write(byte[] bytes, int off, int len) throws IOException {
            bOut.write(bytes, off, len);
        }
        public void write(byte[] bytes) throws IOException {
            bOut.write(bytes);
        }
        public void write(int b) throws IOException {
            bOut.write(b);
        }
        byte[] getSignature() {
            byte[] sigBytes = null;
            try {
                Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, "DNIeJCAProvider");
                signature.initSign(privateKey);
                signature.update(bOut.toByteArray());
                sigBytes = signature.sign();
                closeSession();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return sigBytes;
        }
    }

    public static CMSSignedData signData(PrivateKey privateKey, X509Certificate userCert,
                 Store cerStore, byte[] contentToSign, TimeStampToken timeStampToken) throws Exception {
        CMSAttributeTableGenerator signedAttributeGenerator =
                CMSUtils.getSignedAttributeTableGenerator(timeStampToken);
        CMSTypedData msg = new CMSProcessableByteArray(contentToSign);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        DNIeContentSigner dnieContentSigner = new DNIeContentSigner(privateKey, userCert, cerStore);
        SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(signedAttributeGenerator);
        SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(dnieContentSigner);
        gen.addSignerInfoGenerator(signerInfoGenerator);
        gen.addCertificates(cerStore);
        return gen.generate(msg, true);
    }


}



