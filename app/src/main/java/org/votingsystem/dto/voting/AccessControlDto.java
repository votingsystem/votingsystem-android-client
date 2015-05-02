package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.ActorDto;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessControlDto extends ActorDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = "AccessControlDto";

    @JsonIgnore private EventVSDto eventVS;
    private ControlCenterDto controlCenter;

    public AccessControlDto() {}

    public AccessControlDto(ActorDto actorDto) throws Exception {
        setCertificate(actorDto.getCertificate());
        setCertChain(actorDto.getCertChain());
        setCertificatePEM(actorDto.getCertificatePEM());
        setCertificateURL(actorDto.getCertificateURL());
        setDateCreated(actorDto.getDateCreated());
        setId(actorDto.getId());
        setState(actorDto.getState());
        setServerURL(actorDto.getServerURL());
        setLastUpdated(actorDto.getLastUpdated());
        setName(actorDto.getName());
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }
        
    @Override public Type getServerType() {
        return Type.ACCESS_CONTROL;
    }

    public ControlCenterDto getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenterDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public String getCancelVoteServiceURL() {
        return getServerURL() + "/rest/voteVSCanceller";
    }

    public String getSearchServiceURL (int offset, int max) {
        return getServerURL() + "/rest/search/find?max=" + max + "&offset=" + offset;
    }

    public String getSearchServiceURL (Integer offset, Integer max, String searchText,
            EventVSDto.Type eventType, EventVSDto.State state) {
        String offsetStr = (offset != null? offset.toString():"");
        String maxStr = (max != null? max.toString():"");
        String stateStr = (state != null? state.toString():"");
        String typeStr = (eventType != null? eventType.toString():"");
        return getServerURL() + "/rest/search/eventVS?max=" + maxStr + "&offset=" + offsetStr +
                "&eventVSState=" + stateStr + "&eventvsType=" + typeStr + "&searchText=" + searchText;
    }

    public String getEventVSURL () {
        return getServerURL() + "/rest/eventVS";
    }

    public String getRepresentativesURL (Long offset, Integer pageSize) {
        return getServerURL() + "/rest/representative?offset=" + offset + "&max=" + pageSize;
    }

    public String getRepresentativeURL (Long representativeId) {
        return getServerURL() + "/rest/representative/id/" + representativeId;
    }

    public String getRepresentativeURLByNif (String nif) {
        return getServerURL() + "/rest/representative/nif/" + nif;
    }

    public String getRepresentationStateServiceURL (String nif) {
        return getServerURL() + "/rest/userVS/nif/" + nif + "/representationState";
    }

    public String getRepresentativeImageURL (Long representativeId) {
        return getServerURL() + "/rest/representative/id/" + representativeId + "/image";
    }

    public String getRepresentativeServiceURL () {
        return getServerURL() + "/rest/representative";
    }

    public String getRepresentativeDelegationServiceURL () {
        return getServerURL() + "/rest/representative/delegation";
    }

    public String getEventVSURL (EventVSDto.State eventState, int max, Long offset) {
        return getServerURL() + "/rest/eventVSElection?max="+ max + "&offset=" + offset  +
                "&eventVSState=" + eventState.toString();
    }

    public String getPublishServiceURL() {
        return getServerURL() + "/rest/eventVSElection";
    }

    public String getUserCSRServiceURL (Long csrRequestId) {
        return getServerURL() + "/rest/csr?csrRequestId=" + csrRequestId;
    }

    public String getUserCSRServiceURL () {
        return getServerURL() + "/rest/csr/request";
    }

    public String getCertificationCentersURL () {
        return getServerURL() + "/rest/serverInfo/certificationCenters";
    }

    public String getAccessServiceURL () {
        return getServerURL() +  "/rest/accessRequestVS";
    }

    public String getCheckDatesServiceURL (Long id) {
        return getServerURL() +  "/rest/eventVS/checkDates?id=" + id;
    }

    public String getAnonymousDelegationRequestServiceURL() {
        return getServerURL() + "/rest/representative/anonymousDelegationRequest";
    }

    public String getAnonymousDelegationServiceURL() {
        return getServerURL() + "/rest/representative/anonymousDelegation";
    }

    public String getRepresentativeRevokeServiceURL() {
        return getServerURL() + "/rest/representative/revoke";
    }

    public String getVoteVSCheckServiceURL(String hashHex) {
        return getServerURL() + "/rest/voteVS/hash/" + hashHex;
    }

    public String getCancelAnonymousDelegationServiceURL() {
        return getServerURL() + "/rest/representative/cancelAnonymousDelegation";
    }

}
