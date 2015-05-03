package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.signature.smime.EncryptedBundle;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedBundleDto {

    private Long id;
    private String iv;
    private String salt;
    private String cipherText;

    public EncryptedBundleDto() {}

    public EncryptedBundleDto(EncryptedBundle encryptedBundle) {
        iv = new String (Base64.encode(encryptedBundle.getIV()));
        salt = new String (Base64.encode(encryptedBundle.getSalt()));
        cipherText = new String (Base64.encode(encryptedBundle.getCipherText()));
    }

    public EncryptedBundle getEncryptedBundle() {
        byte[] iv = Base64.decode(this.iv);
        byte[] cipherText = Base64.decode(this.cipherText);
        byte[] salt = Base64.decode(this.salt);
        return new EncryptedBundle(cipherText, iv, salt);
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getCipherText() {
        return cipherText;
    }

    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
