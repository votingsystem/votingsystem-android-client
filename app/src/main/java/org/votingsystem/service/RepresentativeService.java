package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.TypeVS;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = RepresentativeService.class.getSimpleName();

    private AppVS appVS;

    public RepresentativeService() {
        super(TAG);
    }

    @Override protected void onHandleIntent(Intent intent) {
        appVS = (AppVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String dtoStr = arguments.getString(ContextVS.DTO_KEY);
        LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - serviceCaller: " + serviceCaller);
        if(appVS.getAccessControl() == null) {
            ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(
                    R.string.server_connection_error_msg, appVS.getAccessControlURL()));
            appVS.broadcastResponse(responseVS.setServiceCaller(
                    serviceCaller).setTypeVS(operation));
        }
        switch(operation) {
            case ITEMS_REQUEST:
                requestRepresentatives(arguments.getString(ContextVS.URL_KEY), serviceCaller);
                break;
            case ITEM_REQUEST:
                requestRepresentative(arguments.getLong(ContextVS.ITEM_ID_KEY), serviceCaller);
                break;
            case STATE:
                checkRepresentationState(serviceCaller);
                break;
            default: LOGD(TAG + ".onHandleIntent", "unhandled operation: " + operation.toString());
        }
    }

    private void checkRepresentationState(String serviceCaller) {
        if(appVS.getAccessControl() == null) {
            appVS.broadcastResponse(ResponseVS.ERROR(getString(R.string.error_lbl),
                    getString(R.string.connection_required_msg)).setServiceCaller(serviceCaller));
            return;
        }
        String serviceURL = appVS.getAccessControl().getRepresentationStateServiceURL(
                appVS.getUser().getNIF());
        ResponseVS responseVS = null;
        try {
            RepresentationStateDto stateDto = HttpHelper.getInstance().getData(
                    RepresentationStateDto.class, serviceURL, MediaType.JSON);
            switch (stateDto.getState()) {
                case REPRESENTATIVE:
                    ResponseVS representativeImageReponse = HttpHelper.getInstance().getData(
                            appVS.getAccessControl().getRepresentativeImageURL(
                                    stateDto.getRepresentative().getId()), null);
                    if (ResponseVS.SC_OK == representativeImageReponse.getStatusCode()) {
                        stateDto.getRepresentative().setImageBytes(
                                representativeImageReponse.getMessageBytes());
                    }
                    break;
                case WITH_ANONYMOUS_REPRESENTATION:
                    break;
                case WITHOUT_REPRESENTATION:
                    break;
            }
            PrefUtils.putRepresentationState(stateDto);
            responseVS = ResponseVS.OK();
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.STATE);
        appVS.broadcastResponse(responseVS);
    }


    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentType.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                ResultListDto<UserDto> resultListDto = (ResultListDto<UserDto>)
                        responseVS.getMessage(new TypeReference<ResultListDto<UserDto>>() {});
                UserContentProvider.setNumTotalRepresentatives(resultListDto.getTotalCount());
                List<ContentValues> contentValuesList = new ArrayList<>();
                for(UserDto representative : resultListDto.getResultList()) {
                    contentValuesList.add(UserContentProvider.getContentValues(representative));
                }
                if(!contentValuesList.isEmpty()) {
                    int numRowsCreated = getContentResolver().bulkInsert(
                            UserContentProvider.CONTENT_URI,contentValuesList.toArray(
                            new ContentValues[contentValuesList.size()]));
                    LOGD(TAG + ".requestRepresentatives", "inserted: " + numRowsCreated + " rows");
                } else { //To notify ContentProvider Listeners
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, null);
                }
                responseVS = new ResponseVS(ResponseVS.SC_OK);
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.EXCEPTION(ex, this);
            }
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEMS_REQUEST);
        appVS.broadcastResponse(responseVS);
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = appVS.getAccessControl().getRepresentativeURL(representativeId);
        String imageServiceURL = appVS.getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        ResponseVS responseVS = null;
        UserDto representative = null;
        try {
            responseVS = HttpHelper.getInstance().getData(imageServiceURL, null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                representativeImageBytes = responseVS.getMessageBytes();
            responseVS = HttpHelper.getInstance().getData(serviceURL, ContentType.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                representative = (UserDto) responseVS.getMessage(UserDto.class);
                representative.setImageBytes(representativeImageBytes);
                Uri representativeURI = UserContentProvider.getUserURI(
                        representative.getId());
                getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
                responseVS.setUri(representativeURI);
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.EXCEPTION(ex, this);
        }
        responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.ITEM_REQUEST);
        Intent intent = new Intent(serviceCaller);
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        intent.putExtra(ContextVS.USER_KEY, representative);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}