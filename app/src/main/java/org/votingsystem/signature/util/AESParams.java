package org.votingsystem.signature.util;

import android.util.Base64;
import org.votingsystem.dto.AESParamsDto;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AESParams {

    private Key key;
    private IvParameterSpec iv;

    public AESParams() throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        iv = new IvParameterSpec(random.generateSeed(16));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(random);
        kg.init(256);
        key = kg.generateKey();
    }

    public Key getKey() {
        return key;
    }

    public IvParameterSpec getIV() {
        return iv;
    }

    public AESParamsDto getDto() throws UnsupportedEncodingException {
        return new AESParamsDto(Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP),
                Base64.encodeToString(iv.getIV(), Base64.NO_WRAP));
    }

    public static AESParams load(AESParamsDto dto) throws NoSuchAlgorithmException {
        AESParams aesParams = new AESParams();
        byte[] decodeKeyBytes = Base64.decode(dto.getKey().getBytes(), Base64.NO_WRAP);
        aesParams.key = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");
        byte[] iv = Base64.decode(dto.getIv().getBytes(), Base64.NO_WRAP);
        aesParams.iv = new IvParameterSpec(iv);
        return aesParams;
    }

}