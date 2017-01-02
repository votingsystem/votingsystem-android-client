package org.votingsystem.dto.metadata;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CountryDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String name;
    private String language;
    private String code;
    private String displayName;

    public CountryDto() {
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
