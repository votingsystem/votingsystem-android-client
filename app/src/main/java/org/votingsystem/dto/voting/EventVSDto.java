package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = EventVSDto.class.getSimpleName();

    public enum State {ACTIVE, TERMINATED, CANCELLED, PENDING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM}

    public enum Type { CLAIM, MANIFEST, ELECTION }

    public enum Cardinality { MULTIPLE, EXCLUSIVE}

    private Long id;
    private Long eventId;
    private OperationType operationType = OperationType.VOTING_EVENT;
    private Cardinality cardinality;
    private String content;
    private String subject;
    private String URL;
    private Integer numSignaturesCollected;
    private Integer numVotesCollected;
    private ControlCenterDto controlCenter;
    private String user;
    private AccessControlDto accessControl;

    private Set<FieldEventDto> fieldsEventVS = new HashSet<>();
    private List<String> tags;

    private Date dateBegin;
    private Date dateFinish;
    private Date dateCreated;
    private Date lastUpdated;
    private State state;
    private VoteDto vote;
    private String UUID;

    public EventVSDto() {}

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getContent () {
        return content;
    }

    public void setContent (String content) {
        this.content = content;
    }

    public String getSubject() { return subject; }

    public void setSubject (String subject) {
        this.subject = subject;
    }

    public Cardinality getElectionType() {
        return cardinality;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setEventVSId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventVSId() {
        return eventId;
    }


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public ControlCenterDto getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenterDto controlCenter) {
        this.controlCenter = controlCenter;
    }

    public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    public Integer getNumVotesCollected() {
        return numVotesCollected;
    }

    public void setNumVotesCollected(Integer numVotesCollected) {
        this.numVotesCollected = numVotesCollected;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public AccessControlDto getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControlDto accessControl) {
        this.accessControl = accessControl;
    }

    public Set<FieldEventDto> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEventDto> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public void setVote(VoteDto vote) {
        this.vote = vote;
    }

    public VoteDto getVote() {
        return vote;
    }

    @JsonIgnore
    public String getStatsServiceURL() {
        return accessControl.getServerURL() + "/rest/eventElection/id/" + id + "/stats";
    }

    @JsonIgnore public boolean isActive() {
        Date todayDate = java.util.Calendar.getInstance().getTime();
        if (todayDate.after(dateBegin) && todayDate.before(dateFinish)) return true;
        else return false;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}