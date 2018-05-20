package org.votingsystem.util;

import org.votingsystem.dto.voting.ElectionDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum OperationType {

    GET_METADATA("/api/metadata"),
    GET_QR("/api/qr"),
    GET_QR_INFO("/api/qr/info"),
    ADMIN_OPERATION_PROCESS("/api/operation/process"),

    METADATA("/api/metadata"),
    //TimeStamp server
    TIMESTAMP_REQUEST("/api/timestamp"),
    TIMESTAMP_REQUEST_DISCRETE("/api/timestamp/discrete"),

    //voting service provider
    FETCH_ELECTION("/api/election/uuid/{UUID}"),
    SEND_VOTE("/api/vote"),
    PUBLISH_ELECTION("/api/election/save"),
    ELECTIONS_STATS("/api/election/uuid/{electionUUID}/stats"),
    ELECTIONS_SEARCH("/api/election/search?max={pageSize}&offset={offset}&state={state}&searchText={searchText}"),
    VOTE_REPOSITORY("/api/vote/repository"),
    ANON_VOTE_CERT_REQUEST("/election/validateIdentity"),
    CANCEL_VOTE(null),
    //id provider
    AUTHENTICATE_CITIZEN("/api/authenticateCitizen"),
    VALIDATE_CITIZEN_AUTHENTICATION("/api/authenticateCitizen/validate"),

    //id provider
    //Service that generates the QR code with the operation details
    INIT_AUTHENTICATION("/api/auth/initAuthentication"),
    VALIDATE_IDENTITY("/api/auth/validate"),
    ELECTION_INIT_AUTHENTICATION("/api/election/initAuthentication"),

    PROCESS_URL(null);

    private String url;

    OperationType(String url) {
        this.url = url;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

    /********************************************************************************
     *** voting-service urls
     ********************************************************************************/
    public static String getElectionURL(String systenEntityId, String electionUUID) {
        return FETCH_ELECTION.getUrl(systenEntityId).replace("{UUID}", electionUUID);
    }

    public static String getSearchServiceURL(String systenEntityId, Integer offset,
                                             Integer pageSize, String searchText, ElectionDto.State state) {
        if (pageSize == null) pageSize = Constants.ELECTIONS_PAGE_SIZE;
        if (offset == null) pageSize = 0;
        return ELECTIONS_SEARCH.getUrl(systenEntityId).replace("{pageSize}", pageSize.toString())
                .replace("{offset}", offset.toString())
                .replace("{state}", state.name())
                .replace("{searchText}", searchText);
    }

    /*
    public static String getElectionsURL(String systenEntityId, ElectionDto.State state,
                                         Integer pageSize, Long offset) {
        return FETCH_ELECTIONS.getUrl(systenEntityId)
                .replace("{state}", state.name())
                .replace("{size}", pageSize.toString())
                .replace("{offset}", offset.toString());
    }*/

    //"/api/eventElection/id/{id}/stats"
    public static String getVoteStatsURL(String systenEntityId, String electionUUID) {
        return ELECTIONS_STATS.getUrl(systenEntityId).replace("{electionUUID}", electionUUID);
    }

    public static String getVoteRepositoryURL(String systenEntityId) {
        return VOTE_REPOSITORY.getUrl(systenEntityId);
    }

}
