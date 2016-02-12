package org.votingsystem.dto;

import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CryptoDeviceAccessMode implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Mode{PIN, PATTER_LOCK}

    private Mode mode;
    private String hashBase64;

    public CryptoDeviceAccessMode(Mode mode, String passw) throws NoSuchAlgorithmException {
        this.mode = mode;
        this.hashBase64 = StringUtils.getHashBase64(passw, ContextVS.VOTING_DATA_DIGEST);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getHashBase64() {
        return hashBase64;
    }

    public void setHashBase64(String hashBase64) {
        this.hashBase64 = hashBase64;
    }

}
