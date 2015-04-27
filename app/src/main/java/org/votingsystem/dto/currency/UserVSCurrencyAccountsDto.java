package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.UserVSDto;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVSCurrencyAccountsDto {

    private UserVSDto userVS;
    private List<CurrencyAccountDto> accounts;

    public UserVSCurrencyAccountsDto() {}

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public List<CurrencyAccountDto> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<CurrencyAccountDto> accounts) {
        this.accounts = accounts;
    }
}
