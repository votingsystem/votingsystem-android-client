package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.UserContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.dto.ResponseDto;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeService extends IntentService {

    public static final String TAG = RepresentativeService.class.getSimpleName();

    private App app;

    public RepresentativeService() {
        super(TAG);
    }

    @Override protected void onHandleIntent(Intent intent) {
        app = (App) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        OperationType operation = (OperationType)arguments.getSerializable(Constants.TYPEVS_KEY);
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        String dtoStr = arguments.getString(Constants.DTO_KEY);
        LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - serviceCaller: " + serviceCaller);
        if(app.getAccessControl() == null) {
            ResponseDto responseDto = new ResponseDto(ResponseDto.SC_ERROR, getString(
                    R.string.server_connection_error_msg, app.getAccessControlURL()));
            app.broadcastResponse(responseDto.setServiceCaller(
                    serviceCaller).setOperationType(operation));
        }
        switch(operation) {
            case ITEMS_REQUEST:
                requestRepresentatives(arguments.getString(Constants.URL_KEY), serviceCaller);
                break;
            case ITEM_REQUEST:
                requestRepresentative(arguments.getLong(Constants.ITEM_ID_KEY), serviceCaller);
                break;
            case STATE:
                checkRepresentationState(serviceCaller);
                break;
            default: LOGD(TAG + ".onHandleIntent", "unhandled operation: " + operation.toString());
        }
    }

    private void checkRepresentationState(String serviceCaller) {
        if(app.getAccessControl() == null) {
            app.broadcastResponse(ResponseDto.ERROR(getString(R.string.error_lbl),
                    getString(R.string.connection_required_msg)).setServiceCaller(serviceCaller));
            return;
        }
        String serviceURL = app.getAccessControl().getRepresentationStateServiceURL(
                app.getUser().getNIF());
        ResponseDto responseDto = null;
        try {
            RepresentationStateDto stateDto = HttpConnection.getInstance().getData(
                    RepresentationStateDto.class, serviceURL, MediaType.JSON);
            switch (stateDto.getState()) {
                case REPRESENTATIVE:
                    ResponseDto representativeImageReponse = HttpConnection.getInstance().getData(
                            app.getAccessControl().getRepresentativeImageURL(
                                    stateDto.getRepresentative().getId()), null);
                    if (ResponseDto.SC_OK == representativeImageReponse.getStatusCode()) {
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
            responseDto = ResponseDto.OK();
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        }
        responseDto.setServiceCaller(serviceCaller).setOperationType(OperationType.STATE);
        app.broadcastResponse(responseDto);
    }


    private void requestRepresentatives(String serviceURL, String serviceCaller) {
        ResponseDto responseDto = HttpConnection.getInstance().getData(serviceURL, ContentType.JSON);
        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            try {
                ResultListDto<UserDto> resultListDto = (ResultListDto<UserDto>)
                        responseDto.getMessage(new TypeReference<ResultListDto<UserDto>>() {});
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
                responseDto = new ResponseDto(ResponseDto.SC_OK);
            } catch (Exception ex) {
                ex.printStackTrace();
                responseDto = ResponseDto.EXCEPTION(ex, this);
            }
        }
        responseDto.setServiceCaller(serviceCaller).setOperationType(OperationType.ITEMS_REQUEST);
        app.broadcastResponse(responseDto);
    }

    private void requestRepresentative(Long representativeId, String serviceCaller) {
        String serviceURL = app.getAccessControl().getRepresentativeURL(representativeId);
        String imageServiceURL = app.getAccessControl().
                getRepresentativeImageURL(representativeId);
        byte[] representativeImageBytes = null;
        ResponseDto responseDto = null;
        UserDto representative = null;
        try {
            responseDto = HttpConnection.getInstance().getData(imageServiceURL, null);
            if(ResponseDto.SC_OK == responseDto.getStatusCode())
                representativeImageBytes = responseDto.getMessageBytes();
            responseDto = HttpConnection.getInstance().getData(serviceURL, ContentType.JSON);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                representative = (UserDto) responseDto.getMessage(UserDto.class);
                representative.setImageBytes(representativeImageBytes);
                Uri representativeURI = UserContentProvider.getUserURI(
                        representative.getId());
                getContentResolver().insert(UserContentProvider.CONTENT_URI,
                        UserContentProvider.getContentValues(representative));
                responseDto.setUri(representativeURI);
            } else responseDto.setCaption(getString(R.string.operation_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        }
        responseDto.setServiceCaller(serviceCaller).setOperationType(OperationType.ITEM_REQUEST);
        Intent intent = new Intent(serviceCaller);
        intent.putExtra(Constants.RESPONSEVS_KEY, responseDto);
        intent.putExtra(Constants.USER_KEY, representative);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}