package org.votingsystem.signature.smime;

import android.util.Log;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle2.asn1.smime.SMIMECapability;
import org.bouncycastle2.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle2.mail.smime.SMIMESignedGenerator;
import org.bouncycastle2.util.Store;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SignedMailGenerator {
	
	public static final String TAG = SignedMailGenerator.class.getSimpleName();
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    // Get a Session object and create the mail message
    private static Properties props = System.getProperties();
    private static Session session = Session.getDefaultInstance(props, null);

    public SignedMailGenerator(PrivateKey key, Certificate[] chain,
    		String signatureMechanism, String provider) throws Exception {
        Log.d(TAG + ".SignedMailGenerator", " - signatureMechanism: " + signatureMechanism);
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        List certList = Arrays.asList(chain);
        Store certs = new JcaCertStore(certList);
        smimeSignedGenerator = new SMIMESignedGenerator();
        SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();

        signerInfoGeneratorBuilder.setProvider(provider);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, (X509Certificate)chain[0]);
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);

        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }

    public SignedMailGenerator(PrivateKey key, X509Certificate x509Cert,
                   String signatureMechanism, String provider) throws Exception {
        Log.d(TAG + ".SignedMailGenerator", " - signatureMechanism: " + signatureMechanism);
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        smimeSignedGenerator = new SMIMESignedGenerator();
        SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();

        signerInfoGeneratorBuilder.setProvider(provider);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, x509Cert);
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        List certList = Arrays.asList(x509Cert);
        Store certs = new JcaCertStore(certList);
        smimeSignedGenerator.addCertificates(certs);
    }
    
    public SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
            String subject, Header... headers) throws Exception {
        if (subject == null) subject = "";
        if (textToSign == null) textToSign = "";
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(msg,
                Security.getProvider(ContextVS.PROVIDER));
        SMIMEMessage smimeMessage = new SMIMEMessage(mimeMultipart, headers);
        fromUser = StringUtils.getNormalized(fromUser);
        toUser = StringUtils.getNormalized(toUser);
        if(fromUser != null) smimeMessage.setFrom(new InternetAddress(fromUser));
        if(toUser != null) smimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toUser));
        smimeMessage.setSubject(subject);
        return smimeMessage;
    }

    
}