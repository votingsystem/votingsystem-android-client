package org.votingsystem.signature.smime;

import org.votingsystem.dto.EncryptedBundleDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptedBundle {

    private byte[] iv;
    private byte[] cipherText;
    private byte[] salt;

    public EncryptedBundle(byte[] cipherText, byte[] iv, byte[] salt) {
        this.iv = iv;
        this.cipherText = cipherText;
        this.salt = salt;
    }
    public byte[] getIV() { return iv; }
    public byte[] getCipherText() { return cipherText; }
    public byte[] getSalt() { return salt; }

    public EncryptedBundleDto toDto() {
        return new EncryptedBundleDto(this);
    }
}