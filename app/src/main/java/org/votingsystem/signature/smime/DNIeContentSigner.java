package org.votingsystem.signature.smime;


import android.util.Log;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle2.asn1.smime.SMIMECapability;
import org.bouncycastle2.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle2.util.Store;
import org.votingsystem.AppVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import static org.votingsystem.util.ContextVS.DNIe_SIGN_MECHANISM;
import static org.votingsystem.util.ContextVS.PROVIDER;

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
        return new DefaultSignatureAlgorithmIdentifierFinder().find(DNIe_SIGN_MECHANISM);
    }

    @Override public OutputStream getOutputStream() {
        return stream;
    }

    @Override public byte[] getSignature() {
        return stream.getSignature();
    }

    public void closeSession () {
        Log.d(TAG, "closeSession");
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
                Signature signature = Signature.getInstance(DNIe_SIGN_MECHANISM, "DNIeJCAProvider");
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

    public static SMIMEMessage getSMIME(PrivateKey privateKey, X509Certificate userCert, Store cerStore,
                String toUser, String textToSign, String subject, Header... headers) throws Exception {
        if (subject == null) throw new ExceptionVS("missing subject");
        if (textToSign == null) throw new ExceptionVS("missing text to sign");
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        DNIeContentSigner dnieContentSigner = new DNIeContentSigner(privateKey, userCert, cerStore);
        SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(dnieContentSigner);
        gen.addSignerInfoGenerator(signerInfoGenerator);
        gen.addCertificates(dnieContentSigner.getCerStore());
        MimeBodyPart msg = new MimeBodyPart();// create the base for our message
        msg.setText(textToSign); // extract the multipart object from the SMIMESigned object.
        MimeMultipart mimeMultipart = gen.generate(msg, "");
        SMIMEMessage smimeMessage = new SMIMEMessage(mimeMultipart, headers);
        String userNIF = null;
        if (AppVS.getInstance().getUserVS() != null) userNIF =
                AppVS.getInstance().getUserVS().getNIF();
        if(userNIF != null) smimeMessage.setFrom(new InternetAddress(userNIF));
        toUser = StringUtils.getNormalized(toUser);
        if(toUser != null) smimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toUser));
        smimeMessage.setSubject(subject, "UTF-8");
        return smimeMessage;
    }

}



