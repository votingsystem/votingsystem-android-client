package org.votingsystem.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import org.votingsystem.App;
import org.votingsystem.android.R;
import org.votingsystem.contentprovider.ElectionContentProvider;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.Constants;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.PrefUtils;
import org.votingsystem.xml.XmlReader;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionService extends IntentService {

    public static final String TAG = ElectionService.class.getSimpleName();

    public ElectionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LOGD(TAG + ".onHandleIntent", "onHandleIntent ");
        ResponseDto response = null;
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        App app = (App) getApplicationContext();
        if (arguments != null && arguments.containsKey(Constants.STATE_KEY)
                && arguments.containsKey(Constants.OFFSET_KEY)) {
            ElectionDto.State state = (ElectionDto.State) arguments.getSerializable(Constants.STATE_KEY);
            Long offset = arguments.getLong(Constants.OFFSET_KEY);
            if (app.getVotingServiceProvider() == null) {
                List<String> votingServiceProvider = PrefUtils.getVotingServiceProviders();
                MetadataDto entityMetadata = null;
                if(!votingServiceProvider.isEmpty()) {
                    entityMetadata = App.getInstance().getSystemEntity(
                            votingServiceProvider.iterator().next(), true);
                }
                if(entityMetadata == null) {
                    LOGD(TAG, "VotingServiceProvider not found");
                    app.broadcastResponse(ResponseDto.ERROR(getString(R.string.error_lbl),
                            getString(R.string.connection_error_msg)).setServiceCaller(serviceCaller));
                    return;
                }
                app.setVotingServiceProvider(entityMetadata.getEntity());
            }
            String serviceURL = OperationType.getElectionsURL(app.getVotingServiceProvider().getId(),
                    state, Constants.ELECTIONS_PAGE_SIZE, offset);
            response = HttpConn.getInstance().doGetRequest(serviceURL, ContentType.XML);
            if (ResponseDto.SC_OK == response.getStatusCode()) {
                try {
                    ResultListDto<ElectionDto> resultListDto =
                            XmlReader.readElections(response.getMessageBytes());
                    ElectionContentProvider.setNumTotalElections(app.getVotingServiceProvider().getId(),
                            state, resultListDto.getTotalCount());
                    List<ContentValues> contentValuesList = new ArrayList<>();
                    for (ElectionDto election : resultListDto.getResultList()) {
                        ElectionDto.State electionState = election.getState();
                        if (electionState == ElectionDto.State.CANCELED) electionState =
                                ElectionDto.State.TERMINATED;
                        ContentValues values = new ContentValues(6);
                        values.put(ElectionContentProvider.SQL_INSERT_OR_REPLACE, true);
                        values.put(ElectionContentProvider.ID_COL, election.getId());
                        values.put(ElectionContentProvider.URL_COL, election.getURL());
                        values.put(ElectionContentProvider.ENTITY_ID_COL,
                                app.getVotingServiceProvider().getId());
                        values.put(ElectionContentProvider.SERIALIZED_OBJECT_COL,
                                ObjectUtils.serializeObject(election));
                        values.put(ElectionContentProvider.STATE_COL, electionState.toString());
                        contentValuesList.add(values);
                    }
                    if (!contentValuesList.isEmpty()) {
                        int numRowsCreated = getContentResolver().bulkInsert(
                                ElectionContentProvider.CONTENT_URI, contentValuesList.toArray(
                                        new ContentValues[contentValuesList.size()]));
                        LOGD(TAG, "onHandleIntent - inserted: " + numRowsCreated + " rows" +
                                " - eventState: " + state);
                    } else { //To notify ContentProvider Listeners
                        getContentResolver().insert(ElectionContentProvider.CONTENT_URI, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    response = ResponseDto.EXCEPTION(ex, this);
                }
                App.getInstance().addElectiontateLoaded(state);
            } else response.setCaption(getString(R.string.operation_error_msg));
            app.broadcastResponse(response.setServiceCaller(serviceCaller));
        } else LOGD(TAG + ".onHandleIntent", "missing params");
    }

}
