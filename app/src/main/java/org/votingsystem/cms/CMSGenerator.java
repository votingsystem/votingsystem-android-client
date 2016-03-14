package org.votingsystem.cms;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedDataGenerator;
import org.bouncycastle2.cms.CMSTypedData;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle2.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle2.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle2.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle2.util.Store;
import org.votingsystem.util.crypto.CMSUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSGenerator {

    private PrivateKey key;
    private List  certList;
    private String signatureMechanism;
    private String provider;
    private AlgorithmIdentifier sigAlgId;
    private AlgorithmIdentifier digAlgId;

    public CMSGenerator() {}

    public CMSGenerator(PrivateKey privateKey, Certificate[] arrayCerts,
                        String signatureMechanism, String provider) {
        this.key = privateKey;
        certList = Arrays.asList(arrayCerts);
        this.signatureMechanism = signatureMechanism;
        this.provider = provider;
        sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureMechanism);
        digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    }

    public CMSGenerator(PrivateKey privateKey, List<Certificate> certList,
                        String signatureMechanism, String provider) {
        this.key = privateKey;
        this.certList = certList;
        this.signatureMechanism = signatureMechanism;
        this.provider = provider;
        sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureMechanism);
        digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
    }

    public CMSSignedMessage signData(byte[] contentToSign, TimeStampToken timeStampToken) throws Exception {
        CMSAttributeTableGenerator signedAttributeGenerator =
                CMSUtils.getSignedAttributeTableGenerator(timeStampToken);
        CMSTypedData msg = new CMSProcessableByteArray(contentToSign);
        Store certs = new JcaCertStore(certList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism)
                .setProvider(provider).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                .setProvider(provider).build()).setSignedAttributeGenerator(signedAttributeGenerator)
                .build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        CMSSignedData signedData = gen.generate(msg, true);
        return  new CMSSignedMessage(signedData);
    }

    public synchronized CMSSignedData addSignature(CMSSignedData cmsMessage) throws Exception {
        Store signedDataCertStore = cmsMessage.getCertificates();
        SignerInformationStore signers = cmsMessage.getSignerInfos();
        //You'll need to copy the other signers certificates across as well if you want them included.
        List resultCertList = new ArrayList();
        Iterator it = signers.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            Collection certCollection = signedDataCertStore.getMatches(signer.getSID());
            X509CertificateHolder certificateHolder = (X509CertificateHolder)certCollection.iterator().next();
            X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider(
                    provider).getCertificate(certificateHolder);
            resultCertList.add(x509Certificate);
        }
        resultCertList.add(certList.get(0));
        Store certs = new JcaCertStore(resultCertList);
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder(signatureMechanism).setProvider(provider).build(key);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder()
                .setProvider(provider).build()).build(signer, (X509Certificate) certList.get(0)));
        gen.addCertificates(certs);
        gen.addSigners(signers);
        return gen.generate((CMSTypedData)cmsMessage.getSignedContent(), true);
    }

    public byte[] getContentDigest(byte[] contentBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digAlgId.getAlgorithm().getId());
        return digest.digest(contentBytes);
    }

    public TimeStampRequest getTimeStampRequest(byte[] signatureContent) throws NoSuchAlgorithmException {
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        return reqgen.generate(digAlgId.getAlgorithm().getId(), getContentDigest(signatureContent));
    }

}