package org.votingsystem.cms;

import android.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DEREncodable;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle2.asn1.cms.ContentInfo;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle2.cms.SignerId;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.util.Store;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CMSUtils;
import org.votingsystem.util.crypto.KeyGeneratorVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CMSSignedMessage extends CMSSignedData {

    private static Logger log = Logger.getLogger(CMSSignedMessage.class.getName());

    private MessageData messageData;

    public CMSSignedMessage(CMSSignedData signedData) throws IOException, CMSException {
        super(signedData.getEncoded());
    }

    public CMSSignedMessage(byte[] messageBytes) throws Exception {
        super(messageBytes);
    }

    public String getSignedContentStr() throws Exception {
        return new String((byte[])getSignedContent().getContent());
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        return JSON.getMapper().readValue((byte[])getSignedContent().getContent(), type);
    }

    public <T> T getSignedContent(TypeReference type) throws Exception {
        return JSON.getMapper().readValue((byte[])getSignedContent().getContent(), type);
    }

    public static TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                CMSSignedData signedData = new CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                //byte[] hashToken = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
                //String hashTokenStr = new String(Base64.encode(hashToken));
                //Log.d(TAG, "checkTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
                timeStampToken = new TimeStampToken(signedData);
                return timeStampToken;
            }
        } else log.info("checkTimeStampToken - without unsignedAttributes");
        return timeStampToken;
    }

    public TimeStampRequest getTimeStampRequest() throws Exception {
        SignerInformation signerInformation = (SignerInformation) getSignerInfos().getSigners().iterator().next();
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets(),
                BigInteger.valueOf(KeyGeneratorVS.INSTANCE.getNextRandomInt()));
    }

    public byte[] toPEM() throws IOException {
        return PEMUtils.getPEMEncoded(getContentInfo());
    }

    public String toPEMStr() throws IOException {
        return new String(PEMUtils.getPEMEncoded(getContentInfo()));
    }

    public boolean isValidSignature() throws Exception {
        getMessageData();
        return true;
    }

    public static CMSSignedMessage addTimeStamp(CMSSignedMessage signedMessage, TimeStampToken timeStampToken) throws Exception {
        CMSSignedData timeStampedSignedData = CMSUtils.addTimeStamp(signedMessage, timeStampToken);
        return new CMSSignedMessage(timeStampedSignedData.getEncoded());
    }
    /**
     * Digest for storing unique MessageCMS in database
     */
    public String getContentDigestStr() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return Base64.encodeToString(messageDigest.digest((byte[])getSignedContent().getContent()),
                Base64.NO_WRAP);
    }

    public Collection checkSignerCert(X509Certificate x509Cert) throws Exception {
        Store certs = getCertificates();
        X509CertificateHolder holder = new X509CertificateHolder(x509Cert.getEncoded());
        SignerId signerId = new SignerId(holder.getIssuer(), x509Cert.getSerialNumber());
        return certs.getMatches(signerId);
    }

    public static CMSSignedMessage FROM_PEM(String pkcs7PEMData) throws Exception {
        PEMReader PEMReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(pkcs7PEMData.getBytes())));
        ContentInfo contentInfo = (ContentInfo) PEMReader.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    public static CMSSignedMessage FROM_PEM(byte[] pemBytes) throws Exception {
        PEMReader PEMReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(pemBytes)));
        ContentInfo contentInfo = (ContentInfo) PEMReader.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    public static CMSSignedMessage FROM_PEM(InputStream inputStream) throws Exception {
        PEMReader PEMReader = new PEMReader(new InputStreamReader(inputStream));
        ContentInfo contentInfo = (ContentInfo) PEMReader.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    private MessageData getMessageData() throws Exception {
        if(messageData == null) messageData = new MessageData();
        return messageData;
    }

    public TimeStampToken getTimeStampToken() throws Exception {
        return getMessageData().getTimeStampToken();
    }

    public TimeStampToken getTimeStampToken(X509Certificate requestCert) throws Exception {
        Store certs = getCertificates();
        SignerInformationStore signerInfos = getSignerInfos();
        Iterator it = signerInfos.getSigners().iterator();
        while (it.hasNext()) {// check each signer
            SignerInformation   signer = (SignerInformation)it.next();
            Collection certCollection = certs.getMatches(signer.getSID());
            Iterator certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                    (X509CertificateHolder) certIt.next());
            if(requestCert.getSerialNumber().equals(cert.getSerialNumber())) {
                return checkTimeStampToken(signer);
            }
        }
        return null;
    }

    public UserVSDto getSigner() throws Exception {
        return  getMessageData().getSignerVS();
    }

    public Set<UserVSDto> getSigners() throws Exception {
        return getMessageData().getSigners();
    }

    public VoteVSDto getVoteVS() throws Exception {
        return getMessageData().getVoteVS();
    }

    public Set<X509Certificate> getSignersCerts() throws Exception {
        Set<X509Certificate> signerCerts = new HashSet<>();
        for(UserVSDto userVS : getMessageData().getSigners()) {
            signerCerts.add(userVS.getCertificate());
        }
        return signerCerts;
    }

    public X509Certificate getCurrencyCert() throws Exception {
        return getMessageData().getCurrencyCert();
    }

    private class MessageData {

        private Set<UserVSDto> signers = null;
        private UserVSDto signerVS;
        private VoteVSDto voteVS;
        private X509Certificate currencyCert;
        private TimeStampToken timeStampToken;

        public MessageData() throws Exception {
            checkSignature();
        }

        /**
         * verify that the signature is correct and that it was generated when the
         * certificate was current(assuming the cert is contained in the message).
         */
        private boolean checkSignature() throws Exception {
            Store certs = getCertificates();
            SignerInformationStore signerInfos = getSignerInfos();
            Set<X509Certificate> signerCerts = new HashSet<X509Certificate>();
            log.info("checkSignature - document with '" + signerInfos.size() + "' signers");
            Iterator it = signerInfos.getSigners().iterator();
            Date firstSignature = null;
            signers = new HashSet<>();
            while (it.hasNext()) {
                SignerInformation   signer = (SignerInformation)it.next();
                Collection certCollection = certs.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                        (X509CertificateHolder) certIt.next());
                log.info("checkSignature - cert: " + cert.getSubjectDN() + " - " + certCollection.size() + " match");
                try {
                    signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    //concurrency issues ->
                    //signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(ContextVS.PROVIDER).build(cert));
                } catch(CMSVerifierCertificateNotValidException ex) {
                    log.log(Level.SEVERE, "checkSignature - cert notBefore: " + cert.getNotBefore() + " - NotAfter: " +
                            cert.getNotAfter());
                    throw ex;
                } catch (Exception ex) {
                    throw ex;
                }
                UserVSDto userVS = UserVSDto.getUserVS(cert);
                userVS.setSignerInformation(signer);
                TimeStampToken tsToken = checkTimeStampToken(signer);
                userVS.setTimeStampToken(tsToken);
                if(tsToken != null) {
                    Date timeStampDate = tsToken.getTimeStampInfo().getGenTime();
                    if(firstSignature == null || firstSignature.after(timeStampDate)) {
                        firstSignature = timeStampDate;
                        signerVS = userVS;
                    }
                    timeStampToken = tsToken;
                }
                signers.add(userVS);
                if (cert.getExtensionValue(ContextVS.VOTE_OID) != null) {
                    voteVS = getSignedContent(VoteVSDto.class);
                    voteVS.loadSignatureData(cert, timeStampToken);
                } else if (cert.getExtensionValue(ContextVS.CURRENCY_OID) != null) {
                    currencyCert = cert;
                } else {signerCerts.add(cert);}
            }
            if(voteVS != null) voteVS.setServerCerts(signerCerts);
            return true;
        }

        public UserVSDto getSignerVS() {
            return signerVS;
        }

        public Set<UserVSDto> getSigners() {
            return signers;
        }

        public TimeStampToken getTimeStampToken() {
            return timeStampToken;
        }

        public VoteVSDto getVoteVS() {
            return voteVS;
        }

        public X509Certificate getCurrencyCert() {
            return currencyCert;
        }

    }
}
