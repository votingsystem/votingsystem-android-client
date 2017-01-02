package org.votingsystem.dto.voting;

import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = ElectionDto.class.getSimpleName();

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public enum State {ACTIVE, TERMINATED, CANCELED, PENDING, DELETED_FROM_SYSTEM}

    private Long id;
    private OperationType operation;
    private String content;
    private String subject;
    private String URL;
    private Integer numVotesCollected;
    private String user;
    private Set<ElectionOptionDto> electionOptions = new HashSet<>();
    private List<String> tags;
    private Date dateBegin;
    private Date dateFinish;
    private Date dateCreated;
    private Date lastUpdated;
    private State state;
    private VoteDto vote;
    private String publisher;
    private String entityId;
    private String UUID;

    private SystemEntityDto votingServiceProvider;

    public ElectionDto() {  }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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

    public Set<ElectionOptionDto> getElectionOptions() {
        return electionOptions;
    }

    public void setElectionOptions(Set<ElectionOptionDto> electionOptions) {
        this.electionOptions = electionOptions;
    }

    public void setVote(VoteDto vote) {
        this.vote = vote;
    }

    public VoteDto getVote() {
        return vote;
    }

    public OperationType getOperation() {
        return operation;
    }

    public ElectionDto setOperation(OperationType operation) {
        this.operation = operation;
        return this;
    }

    public boolean isActive() {
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

    public SystemEntityDto getVotingServiceProvider() {
        return votingServiceProvider;
    }

    public void setVotingServiceProvider(SystemEntityDto votingServiceProvider) {
        this.votingServiceProvider = votingServiceProvider;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}