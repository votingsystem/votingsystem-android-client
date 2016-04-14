package org.votingsystem.dto;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AESParamsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String iv;
    @JsonIgnore private Key aesKey;
    @JsonIgnore private transient IvParameterSpec ivParam;

    public AESParamsDto() { }

    public AESParamsDto(Key aesKey, IvParameterSpec ivParam) {
        this.aesKey = aesKey;
        this.ivParam = ivParam;
    }

    public static AESParamsDto CREATE() throws NoSuchAlgorithmException {
        AESParamsDto result = new AESParamsDto();
        SecureRandom random = new SecureRandom();
        result.ivParam = new IvParameterSpec(random.generateSeed(16));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(random);
        kg.init(256);
        result.aesKey = kg.generateKey();
        return result;
    }

    @JsonIgnore public Key getAesKey() {
        if(aesKey == null && key != null) {
            byte[] decodeKeyBytes = Base64.decode(key.getBytes(), Base64.NO_WRAP);
            aesKey = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        }
        return aesKey;
    }

    @JsonIgnore public IvParameterSpec getIvParam() {
        if(ivParam == null && iv != null) ivParam = new IvParameterSpec(Base64.decode(iv.getBytes(), Base64.NO_WRAP));
        return ivParam;
    }

    public String getKey() {
        if(key == null && aesKey != null) key = Base64.encodeToString(aesKey.getEncoded(), Base64.NO_WRAP);
        return key;
    }

    public String getIv() {
        if(iv == null && ivParam != null) iv = Base64.encodeToString(ivParam.getIV(), Base64.NO_WRAP);
        return iv;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if(ivParam != null) s.writeObject(ivParam.getIV());
        else s.writeObject(null);

    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        byte[] ivParamBytes = (byte[]) s.readObject();
        if(ivParamBytes != null) ivParam = new IvParameterSpec(ivParamBytes);
    }

}