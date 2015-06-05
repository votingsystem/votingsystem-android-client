package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.model.Currency;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private T data;
    @JsonIgnore private String origingHashCertVS;
    @JsonIgnore private Currency currency ;
    private Long deviceId;
    private Date dateCreated;
    private String hashCertVS;
    private String url;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(DeviceVSDto deviceVSDto, TypeVS typeVS){
        this.typeVS = typeVS;
        this.deviceId = deviceVSDto.getId();
        this.dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public static QRMessageDto FROM_URL(String url) throws NoSuchAlgorithmException {
        QRMessageDto result = new QRMessageDto();
        result.setUrl(url);
        result.createRequest();
        return result;
    }

    public void createRequest() throws NoSuchAlgorithmException {
        this.origingHashCertVS = java.util.UUID.randomUUID().toString();
        this.hashCertVS = CMSUtils.getHashBase64(origingHashCertVS, ContextVS.VOTING_DATA_DIGEST);
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOrigingHashCertVS() {
        return origingHashCertVS;
    }

    public void setOrigingHashCertVS(String origingHashCertVS) {
        this.origingHashCertVS = origingHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}