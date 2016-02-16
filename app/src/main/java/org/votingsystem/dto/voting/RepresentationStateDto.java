package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.UserVSDto;

import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentationStateDto {

    public enum State {
        REPRESENTATIVE, WITH_ANONYMOUS_REPRESENTATION, WITHOUT_REPRESENTATION;
    }

    private State state;
    private Date lastCheckedDate;
    private Date dateFrom;
    private Date dateTo;
    private UserVSDto representative;
    private String anonymousDelegationObject;
    private String base64ContentDigest;
    private String stateMsg;
    private String lastCheckedDateMsg;


    public RepresentationStateDto() {}

    public RepresentationStateDto(Date lastCheckedDate, State state,
                                  UserVSDto representative, Date dateTo) {
        this.state = state;
        this.dateTo = dateTo;
        this.lastCheckedDate = lastCheckedDate;
        this.representative = representative;
    }

    public RepresentationStateDto clone() {
        RepresentationStateDto representationStateDto = new RepresentationStateDto();
        representationStateDto.setState(state);
        representationStateDto.setLastCheckedDate(lastCheckedDate);
        representationStateDto.setDateFrom(dateFrom);
        representationStateDto.setDateTo(dateTo);
        representationStateDto.setRepresentative(representative);
        representationStateDto.setBase64ContentDigest(base64ContentDigest);
        return representationStateDto;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getLastCheckedDate() {
        return lastCheckedDate;
    }

    public void setLastCheckedDate(Date lastCheckedDate) {
        this.lastCheckedDate = lastCheckedDate;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public UserVSDto getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVSDto representative) {
        this.representative = representative;
    }

    public String getAnonymousDelegationObject() {
        return anonymousDelegationObject;
    }

    public void setAnonymousDelegationObject(String anonymousDelegationObject) {
        this.anonymousDelegationObject = anonymousDelegationObject;
    }

    public String getBase64ContentDigest() {
        return base64ContentDigest;
    }

    public void setBase64ContentDigest(String base64ContentDigest) {
        this.base64ContentDigest = base64ContentDigest;
    }

    public String getStateMsg() {
        return stateMsg;
    }

    public void setStateMsg(String stateMsg) {
        this.stateMsg = stateMsg;
    }

    public String getLastCheckedDateMsg() {
        return lastCheckedDateMsg;
    }

    public void setLastCheckedDateMsg(String lastCheckedDateMsg) {
        this.lastCheckedDateMsg = lastCheckedDateMsg;
    }
}
