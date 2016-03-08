package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.AppVS;
import org.votingsystem.util.crypto.Encryptor;

import java.util.UUID;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCertificationRequestDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private AddressVS addressVS;
    private byte[] csrRequest;
    private byte[] token;
    @JsonIgnore private byte[] plainToken;

    public UserCertificationRequestDto(){}

    public UserCertificationRequestDto(AddressVS addressVS, byte[] csrRequest) throws Exception {
        this.addressVS = addressVS;
        this.csrRequest = csrRequest;
        this.plainToken = UUID.randomUUID().toString().getBytes();
        this.token = Encryptor.encryptToCMS(this.plainToken,
                AppVS.getInstance().getCurrencyServer().getCertificate());
    }

    public byte[] getCsrRequest() {
        return csrRequest;
    }

    public void setCsrRequest(byte[] csrRequest) {
        this.csrRequest = csrRequest;
    }

    public AddressVS getAddressVS() {
        return addressVS;
    }

    public void setAddressVS(AddressVS addressVS) {
        this.addressVS = addressVS;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public byte[] getPlainToken() {
        return plainToken;
    }

    public void setPlainToken(byte[] plainToken) {
        this.plainToken = plainToken;
    }
}
