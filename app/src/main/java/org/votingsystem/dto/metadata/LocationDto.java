package org.votingsystem.dto.metadata;

import org.votingsystem.dto.AddressDto;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class LocationDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private CountryDto country;
    private String city;
    private AddressDto address;

    public LocationDto() {
    }

    public CountryDto getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setCountry(CountryDto country) {
        this.country = country;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

}

