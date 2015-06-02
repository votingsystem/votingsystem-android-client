package org.votingsystem.dto.currency;

import org.bouncycastle2.util.encoders.Hex;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyServerDto extends ActorDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = CurrencyServerDto.class.getSimpleName();


    public String getTransactionVSServiceURL() {
        return getServerURL() + "/rest/transactionVS";
    }

    public String getCurrencyTransactionServiceURL() {
        return getServerURL() + "/rest/transactionVS/currency";
    }

    public String getCurrencyRequestServiceURL() {
        return getServerURL() + "/currency/request";
    }

    public String getUserInfoServiceURL(String nif) {
        return getServerURL() + "/rest/userVS/nif/" + nif;
    }

    public String getTagVSSearchServiceURL(String searchParam) {
        return getServerURL() + "/rest/tagVS?tag=" + searchParam;
    }

    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/rest/userVS" + DateUtils.getPath(date);
    }

    public String getDeviceVSConnectedServiceURL(String nif) {
        return getServerURL() + "/rest/deviceVS/nif/" + nif + "/connected";
    }

    public String getSearchServiceURL(String searchText) {
        return getServerURL() + "/rest/userVS/search?searchText=" + searchText;
    }

    public String getSearchServiceURL(String phone, String email) {
        return getServerURL() + "/rest/userVS/searchByDevice?phone=" + phone + "&email=" + email;
    }
    public String getCurrencyStateServiceURL(String hashCertVS) {
        return getServerURL() + "/rest/currency/state/" +
                new String(Hex.encode(hashCertVS.getBytes()), ContextVS.UTF_8);
    }

    public String getCurrencyBundleStateServiceURL() {
        return getServerURL() + "/rest/currency/bundleState";
    }

    public String getDeviceVSByIdServiceURL(Long deviceId) {
        return getServerURL() + "/rest/deviceVS/id/" + deviceId;
    }

}
