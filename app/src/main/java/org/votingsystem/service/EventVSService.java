package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.fasterxml.jackson.core.type.TypeReference;

import org.votingsystem.AppVS;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.EventVSContentProvider;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.ResponseVS;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSService extends IntentService {

    public static final String TAG = EventVSService.class.getSimpleName();

    public EventVSService() { super(TAG); }

    private AppVS appVS = null;

    public void checkDates(EventVSDto eventVS) {
        Date todayDate = Calendar.getInstance().getTime();
        final String checkURL = appVS.getAccessControl().
                getCheckDatesServiceURL(eventVS.getId());
        Runnable runnable = null;
        if(eventVS.getState() == EventVSDto.State.ACTIVE) {
            if(todayDate.after(eventVS.getDateFinish())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        } else if(eventVS.getState() == EventVSDto.State.PENDING) {
            if(todayDate.after(eventVS.getDateBegin())){
                runnable = new Runnable() {
                    public void run() {HttpHelper.getData(checkURL, null);}
                };
            }
        }
        if(runnable != null) new Thread(runnable).start();
    }

    @Override protected void onHandleIntent(Intent intent) {
        LOGD(TAG + ".onHandleIntent", "onHandleIntent ");
        ResponseVS responseVS = null;
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        appVS = (AppVS) getApplicationContext();
        if(arguments != null && arguments.containsKey(ContextVS.STATE_KEY)
                && arguments.containsKey(ContextVS.OFFSET_KEY)) {
            EventVSDto.State eventState = (EventVSDto.State) arguments.getSerializable(ContextVS.STATE_KEY);
            Long offset = arguments.getLong(ContextVS.OFFSET_KEY);
            if(appVS.getAccessControl() == null) {
                LOGD(TAG, "AccessControl not initialized");
                appVS.broadcastResponse(ResponseVS.ERROR(getString(R.string.error_lbl),
                        getString(R.string.connection_error_msg)).setServiceCaller(serviceCaller));
                return;
            }
            String serviceURL = appVS.getAccessControl().getEventVSURL(eventState,
                    ContextVS.EVENTS_PAGE_SIZE, offset);
            responseVS = HttpHelper.getData(serviceURL, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    ResultListDto<EventVSDto> resultListDto = JSON.readValue(
                            responseVS.getMessageBytes(), new TypeReference<ResultListDto<EventVSDto>>() {});
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
                        checkDates(eventVS);
                        ContentValues values = new ContentValues(5);
                        values.put(EventVSContentProvider.SQL_INSERT_OR_REPLACE, true );
                        values.put(EventVSContentProvider.ID_COL, eventVS.getId());
                        values.put(EventVSContentProvider.URL_COL, eventVS.getURL());
                        values.put(EventVSContentProvider.JSON_DATA_COL,
                                JSON.writeValueAsString(eventVS));
                        values.put(EventVSContentProvider.TYPE_COL, eventVS.getTypeVS().toString());
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
                    responseVS = ResponseVS.EXCEPTION(ex, this);
                }
            } else responseVS.setCaption(getString(R.string.operation_error_msg));
            appVS.broadcastResponse(responseVS);
        } else LOGD(TAG + ".onHandleIntent", "missing params");
    }

}
