package org.votingsystem.dto;

import android.util.Base64;

import org.votingsystem.throwable.ExceptionBase;
import org.votingsystem.util.Country;

import java.io.Serializable;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AddressDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private Long id;
    private String address;
    private String metaInf;
    private String postalCode;
    private String province;
    private Country country;
    private String city;
    public Date dateCreated;
    public Date lastUpdated;

    public AddressDto() {
    }

    public AddressDto(String address, String postalCode) {
        if (address != null) {
            this.address = Base64.encodeToString(address.getBytes(), Base64.NO_WRAP);
        }
        this.postalCode = postalCode;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        String result = null;
        try {
            byte[] decodeAddressBytes = Base64.decode(address.getBytes(), Base64.NO_WRAP);
            if (address != null) result = new String(decodeAddressBytes, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public void checkAddress(AddressDto address) throws ExceptionBase {
        if (address.getAddress() != null)
            if (!address.getAddress().equals(this.address)) throw new ExceptionBase(
                    "expected address " + address.getAddress() + " found " + this.address);
        if (address.getPostalCode() != null) if (!address.getPostalCode().equals(postalCode))
            throw new ExceptionBase("expected postalCode " + address.getAddress() +
                    " found " + postalCode);
        if (address.getProvince() != null) if (!address.getProvince().equals(province))
            throw new ExceptionBase("expected province " + address.getProvince() +
                    " found " + province);
        if (address.getCity() != null) if (!address.getCity().equals(city))
            throw new ExceptionBase("expected city " + address.getCity() + " found " + city);
        if (address.getCountry() != null) if (!address.getCountry().equals(country))
            throw new ExceptionBase("expected country " + address.getCountry() + " found " + country);
    }

}
