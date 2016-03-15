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


    public String getTransactionServiceURL() {
        return getServerURL() + "/rest/transaction";
    }

    public String getCurrencyTransactionServiceURL() {
        return getServerURL() + "/rest/transaction/currency";
    }

    public String getCurrencyRequestServiceURL() {
        return getServerURL() + "/currency/request";
    }

    public String getUserInfoServiceURL(String nif) {
        return getServerURL() + "/rest/user/nif/" + nif;
    }

    public String getTagVSSearchServiceURL(String searchParam) {
        return getServerURL() + "/rest/tagVS?tag=" + searchParam;
    }

    public String getTagVSServiceURL() {
        return getServerURL() + "/rest/tagVS/list";
    }

    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/rest/user" + DateUtils.getPath(date);
    }

    public String getDeviceConnectedServiceURL(String nif) {
        return getServerURL() + "/rest/device/nif/" + nif + "/connected";
    }

    public String getDeviceConnectedServiceURL(Long deviceId, Boolean getAllDevicesFromOwner) {
        return getServerURL() + "/rest/device/id/" + deviceId +
                "/connected?getAllDevicesFromOwner=" + getAllDevicesFromOwner;
    }

    public String getSearchServiceURL(String searchText) {
        return getServerURL() + "/rest/user/search?searchText=" + searchText;
    }

    public String getSearchServiceURL(String phone, String email) {
        String query = phone != null? "phone=" + phone.replace(" ", "").trim() + "&":"";
        if(email != null) query = query + "email=" + email.trim();
        return getServerURL() + "/rest/user/searchByDevice?" + query;
    }

    public String getCurrencyStateServiceURL(String hashCertVS) {
        return getServerURL() + "/rest/currency/hash/" +
                new String(Hex.encode(hashCertVS.getBytes()), ContextVS.UTF_8) + "/state";
    }

    public String getCurrencyBundleStateServiceURL() {
        return getServerURL() + "/rest/currency/bundleState";
    }

    public String getDeviceByIdServiceURL(Long deviceId) {
        return getServerURL() + "/rest/device/id/" + deviceId;
    }

}
