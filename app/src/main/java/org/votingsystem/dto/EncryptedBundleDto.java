package org.votingsystem.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
        iv = Base64.encodeToString(encryptedBundle.getIV(), Base64.NO_WRAP);
        salt = Base64.encodeToString(encryptedBundle.getSalt(), Base64.NO_WRAP);
        cipherText = Base64.encodeToString(encryptedBundle.getCipherText(), Base64.NO_WRAP);
    }

    public EncryptedBundle getEncryptedBundle() {
        byte[] iv = Base64.decode(this.iv, Base64.NO_WRAP);
        byte[] cipherText = Base64.decode(this.cipherText, Base64.NO_WRAP);
        byte[] salt = Base64.decode(this.salt, Base64.NO_WRAP);
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
