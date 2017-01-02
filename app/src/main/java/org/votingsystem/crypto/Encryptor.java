package org.votingsystem.crypto;

import android.util.Base64;

import org.bouncycastle2.asn1.cms.ContentInfo;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle2.cms.CMSAlgorithm;
import org.bouncycastle2.cms.CMSEnvelopedData;
import org.bouncycastle2.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle2.cms.CMSEnvelopedDataParser;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSTypedStream;
import org.bouncycastle2.cms.RecipientInformation;
import org.bouncycastle2.cms.RecipientInformationStore;
import org.bouncycastle2.cms.bc.BcRSAKeyTransRecipientInfoGenerator;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.openssl.PEMReader;
import org.bouncycastle2.operator.OutputEncryptor;
import org.votingsystem.util.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Collection;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Encryptor {

    public static final String TAG = Encryptor.class.getSimpleName();

    private Encryptor() {
    }

    public static byte[] encryptToCMS(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
        edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator("".getBytes(), publicKey));
        OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider(
                Constants.PROVIDER).build();
        CMSEnvelopedData ed = edGen.generate(new CMSProcessableByteArray(bytesToEncrypt), encryptor);
        return PEMUtils.getPEMEncoded(ed.getContentInfo());
    }

    public static byte[] encryptToCMS(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
        edGen.addRecipientInfoGenerator(new BcRSAKeyTransRecipientInfoGenerator(
                new JcaX509CertificateHolder(receiverCert)));
        OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider(
                Constants.PROVIDER).build();
        CMSEnvelopedData ed = edGen.generate(new CMSProcessableByteArray(bytesToEncrypt), encryptor);
        return PEMUtils.getPEMEncoded(ed.getContentInfo());
    }

    /**
     * Method to decrypt CMS signed messages
     */
    public static byte[] decryptCMS(byte[] pemEncrypted, PrivateKey privateKey)
            throws Exception {
        PEMReader PEMParser = new PEMReader(new InputStreamReader(new ByteArrayInputStream(pemEncrypted)));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        CMSEnvelopedDataParser ep = new CMSEnvelopedDataParser(contentInfo.getEncoded());
        RecipientInformationStore recipients = ep.getRecipientInfos();
        Collection c = recipients.getRecipients();
        Iterator it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation recipient = (RecipientInformation) it.next();
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(Constants.ANDROID_PROVIDER));
            InputStream dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
        }
        return result;
    }

    public static EncryptedBundle pbeAES_Encrypt(String password, byte[] bytesToEncrypt)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGenerator.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, Constants.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, Constants.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new EncryptedBundle(cipher.doFinal(bytesToEncrypt), iv, salt);
    }

    public static byte[] pbeAES_Decrypt(String password, EncryptedBundle bundle) throws
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, UnsupportedEncodingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), bundle.getSalt(), Constants.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, Constants.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.getIV()));
        return cipher.doFinal(bundle.getCipherText());
    }

    public static String encryptRSA(String plainText, PublicKey publicKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = rsaCipher.doFinal(plainText.getBytes());
        return Base64.encodeToString(cipherText, Base64.NO_WRAP);
    }

    public static String decryptRSA(String encryptedTextBase64, PrivateKey privateKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainText = rsaCipher.doFinal(Base64.decode(encryptedTextBase64.getBytes("UTF-8"), Base64.NO_WRAP));
        return new String(plainText, "UTF-8");
    }
}
