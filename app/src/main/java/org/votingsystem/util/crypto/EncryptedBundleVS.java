package org.votingsystem.util.crypto;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.util.ResponseVS;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptedBundleVS {

    public enum Type {CMS_MESSAGE, TEXT, FILE}

    private byte[] encryptedMessageBytes;
    private CMSSignedMessage decryptedCMSMessage;
    private byte[] decryptedMessageBytes;
    private String message;
    private Type type;
    private int statusCode = ResponseVS.SC_PROCESSING;

    public EncryptedBundleVS(byte[] encryptedMessageBytes, Type type) {
        this.encryptedMessageBytes = encryptedMessageBytes;
        this.type = type;
    }

    public void setDecryptedMessageBytes(byte[] decryptedMessageBytes) {
        this.decryptedMessageBytes = decryptedMessageBytes;
    }

    public CMSSignedMessage getDecryptedCMSMessage() {
        return decryptedCMSMessage;
    }

    public void setDecryptedCMSMessage(CMSSignedMessage decryptedCMSMessage) {
        this.decryptedCMSMessage = decryptedCMSMessage;
    }

    public byte[] getEncryptedMessageBytes() {
        return encryptedMessageBytes;
    }

    public void setEncryptedMessageBytes(byte[] encryptedMessageBytes) {
        this.encryptedMessageBytes = encryptedMessageBytes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}