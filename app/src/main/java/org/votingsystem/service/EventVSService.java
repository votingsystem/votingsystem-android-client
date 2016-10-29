package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.EventVSContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.HttpConnection;
import org.votingsystem.util.JSON;
import org.votingsystem.dto.ResponseDto;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSService extends IntentService {

    public static final String TAG = EventVSService.class.getSimpleName();

    public EventVSService() { super(TAG); }

    private App app = null;

    @Override protected void onHandleIntent(Intent intent) {
        LOGD(TAG + ".onHandleIntent", "onHandleIntent ");
        ResponseDto responseDto = null;
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        app = (App) getApplicationContext();
        if(arguments != null && arguments.containsKey(Constants.STATE_KEY)
                && arguments.containsKey(Constants.OFFSET_KEY)) {
            EventVSDto.State eventState = (EventVSDto.State) arguments.getSerializable(Constants.STATE_KEY);
            Long offset = arguments.getLong(Constants.OFFSET_KEY);
            if(app.getAccessControl() == null) {
                LOGD(TAG, "AccessControl not initialized");
                app.broadcastResponse(ResponseDto.ERROR(getString(R.string.error_lbl),
                        getString(R.string.connection_error_msg)).setServiceCaller(serviceCaller));
                return;
            }
            String serviceURL = app.getAccessControl().getEventVSURL(eventState,
                    Constants.EVENTS_PAGE_SIZE, offset);
            responseDto = HttpConnection.getInstance().getData(serviceURL, ContentType.JSON);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    ResultListDto<EventVSDto> resultListDto = JSON.readValue(
                            responseDto.getMessageBytes(), new TypeReference<ResultListDto<EventVSDto>>() {});
                    switch (eventState) {
                        case ACTIVE:
                            EventVSContentProvider.setNumTotalElectionsActive(
                                    Long.valueOf(resultListDto.getTotalCount()));
                            break;
                        case PENDING:
                            EventVSContentProvider.setNumTotalElectionsPending(
                                    Long.valueOf(resultListDto.getTotalCount()));
                            break;
                        case TERMINATED:
                            EventVSContentProvider.setNumTotalElectionsTerminated(
                                    Long.valueOf(resultListDto.getTotalCount()));
                            break;
                    }
                    List<ContentValues> contentValuesList = new ArrayList<>();
                    for(EventVSDto eventVS : resultListDto.getResultList()) {
                        EventVSDto.State eventVSState = eventVS.getState();
                        if(eventVSState == EventVSDto.State.CANCELLED) eventVSState =
                                EventVSDto.State.TERMINATED;
                        ContentValues values = new ContentValues(5);
                        values.put(EventVSContentProvider.SQL_INSERT_OR_REPLACE, true );
                        values.put(EventVSContentProvider.ID_COL, eventVS.getId());
                        values.put(EventVSContentProvider.URL_COL, eventVS.getURL());
                        values.put(EventVSContentProvider.JSON_DATA_COL,
                                JSON.writeValueAsString(eventVS));
                        values.put(EventVSContentProvider.TYPE_COL, eventVS.getOperationType().toString());
                        values.put(EventVSContentProvider.STATE_COL, eventVSState.toString());
                        contentValuesList.add(values);
                    }
                    if(!contentValuesList.isEmpty()) {
                        int numRowsCreated = getContentResolver().bulkInsert(
                                EventVSContentProvider.CONTENT_URI, contentValuesList.toArray(
                                new ContentValues[contentValuesList.size()]));
                        LOGD(TAG + ".onHandleIntent", "inserted: " + numRowsCreated + " rows" +
                            " - eventState: " + eventState);
                    } else { //To notify ContentProvider Listeners
                        getContentResolver().insert(EventVSContentProvider.CONTENT_URI, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseDto = ResponseDto.EXCEPTION(ex, this);
                }
            } else responseDto.setCaption(getString(R.string.operation_error_msg));
            app.broadcastResponse(responseDto.setServiceCaller(serviceCaller));
        } else LOGD(TAG + ".onHandleIntent", "missing params");
    }

}
