package org.votingsystem.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.dto.CertRequestDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.ContextVS.CALLER_KEY;
import static org.votingsystem.util.ContextVS.DTO_KEY;
import static org.votingsystem.util.ContextVS.PIN_KEY;
import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.util.ContextVS.State;
import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserCertRequestService extends IntentService {

    public static final String TAG = UserCertRequestService.class.getSimpleName();

    public UserCertRequestService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        LOGD(TAG + ".onHandleIntent", "arguments: " + arguments);
        AppVS appVS = (AppVS) getApplicationContext();
        String serviceCaller = arguments.getString(CALLER_KEY);
        ResponseVS responseVS = null;
        try {
            CertRequestDto dto = JSON.readValue(arguments.getString(DTO_KEY),
                    CertRequestDto.class);
            char[] pin = arguments.getCharArray(PIN_KEY);
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    SIGNATURE_ALGORITHM, PROVIDER, dto.getNif(), dto.getEmail(),
                    dto.getPhone(), dto.getDeviceId(), dto.getGivenName(), dto.getSurname(),
                    DeviceVSDto.Type.MOBILE);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            responseVS = HttpHelper.sendData(csrBytes, null,
                    appVS.getAccessControl().getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                certificationRequest.setHashPin(CMSUtils.getHashBase64(new String(pin),
                        ContextVS.VOTING_DATA_DIGEST));
                PrefUtils.putCsrRequest(requestId, certificationRequest);
                PrefUtils.putAppCertState(appVS.getAccessControl().getServerURL(),
                        State.WITH_CSR, null);
                responseVS.setCaption(getString(R.string.operation_ok_msg));
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch (Exception ex){
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        } finally {
            responseVS.setServiceCaller(serviceCaller);
            appVS.broadcastResponse(responseVS);
        }
    }

}
