package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.ActorDto;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControlCenterDto extends ActorDto implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private EventVSDto eventVS;

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }
        
    @Override public Type getServerType() {
        return Type.CONTROL_CENTER;
    }

    public String getVoteServiceURL () {
        return getServerURL() + "/rest/voteVS";
    }

}