package org.votingsystem.util.crypto;

import android.util.Base64;

import org.bouncycastle2.cms.CMSAlgorithm;
import org.bouncycastle2.cms.CMSEnvelopedDataParser;
import org.bouncycastle2.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSTypedStream;
import org.bouncycastle2.cms.RecipientInformation;
import org.bouncycastle2.cms.RecipientInformationStore;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.crypto.CipherParameters;
import org.bouncycastle2.crypto.InvalidCipherTextException;
import org.bouncycastle2.crypto.engines.AESEngine;
import org.bouncycastle2.crypto.modes.CBCBlockCipher;
import org.bouncycastle2.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle2.crypto.params.KeyParameter;
import org.bouncycastle2.crypto.params.ParametersWithIV;
import org.bouncycastle2.operator.OperatorCreationException;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

	private  Encryptor() { }

    public static EncryptedBundleVS decryptEncryptedBundle(EncryptedBundleVS encryptedBundleVS,
               PrivateKey privateKey) throws Exception {
        byte[] decriptedMsgBytes = decryptCMS(encryptedBundleVS.getEncryptedMessageBytes(),
                privateKey);
        CMSSignedMessage cmsMessage = new CMSSignedMessage(decriptedMsgBytes);
        encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
        encryptedBundleVS.setDecryptedCMSMessage(cmsMessage);
        return encryptedBundleVS;
    }

    public static List<EncryptedBundleVS> decryptEncryptedBundleList(
            List<EncryptedBundleVS> encryptedBundleVSList, PrivateKey privateKey) throws Exception {
        List<EncryptedBundleVS> result = new ArrayList<EncryptedBundleVS>();
        for(EncryptedBundleVS encryptedBundleVS : encryptedBundleVSList) {
            result.add(decryptEncryptedBundle(encryptedBundleVS, privateKey));
        }
        return result;
    }

    public static byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert)
            throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
        CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
        dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(receiverCert).
                setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = dataStreamGen.open(bOut, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.AES256_CBC).setProvider(ContextVS.PROVIDER).build());
        out.write(dataToEncrypt);
        out.close();
        return android.util.Base64.encode(bOut.toByteArray(), android.util.Base64.NO_WRAP);
    }

    /**
     * Method to decrypt CMS signed messages
     */
    public static byte[] decryptCMS( byte[] base64EncryptedData, PrivateKey privateKey)
            throws Exception {
        byte[] cmsEncryptedData = Base64.decode(base64EncryptedData, Base64.NO_WRAP);
        CMSEnvelopedDataParser ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection c = recipients.getRecipients();
        Iterator it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.ANDROID_PROVIDER));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
            //assertEquals(true, Arrays.equals(data, dataOut.toByteArray()));
        }
        return result;
    }

    public static EncryptedBundle pbeAES_Encrypt(String password, byte[] bytesToEncrypt)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGeneratorVS.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ContextVS.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
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
        KeySpec spec = new PBEKeySpec(password.toCharArray(), bundle.getSalt(), ContextVS.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.getIV()));
        return cipher.doFinal(bundle.getCipherText());
    }

    /*public static String encryptAES(String messageToEncrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessage = cipher.doFinal(messageToEncrypt.getBytes("UTF-8"));
        return new String(org.bouncycastle2.util.encoders.Base64.encode(encryptedMessage));
    }*/

    /*public static String decryptAES(String messageToDecrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessageBytes = org.bouncycastle2.util.encoders.Base64.decode(
                messageToDecrypt.getBytes("UTF-8"));
        byte[] decryptedBytes = cipher.doFinal(encryptedMessageBytes);
        return new String(decryptedBytes, "UTF8");
    }*/

    //BC provider to avoid key length restrictions on normal jvm
    public static String encryptAES(String messageToEncrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        ParametersWithIV params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(true, params); //to decrypt put param to false
        byte[] input = messageToEncrypt.getBytes("UTF-8");
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        return Base64.encodeToString(output, Base64.NO_WRAP);
    }

    //BC provider to avoid key length restrictions on normal jvm
    public static String decryptAES(String messageToDecrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(false, params); //to encrypt put param to true
        byte[] input = Base64.decode(messageToDecrypt.getBytes("UTF-8"), Base64.NO_WRAP);
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        int i = output.length - 1; //remove padding
        while (i >= 0 && output[i] == 0) { --i; }
        byte[] result = Arrays.copyOf(output, i + 1);
        return new String(result);
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