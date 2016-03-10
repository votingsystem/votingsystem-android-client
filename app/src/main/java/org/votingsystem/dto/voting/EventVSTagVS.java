package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.dto.TagVSDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSTagVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EventVSDto eventVS;
    private TagVSDto tagVS;

    public EventVSTagVS() { }

    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public EventVSDto getEventVS() {
        return this.eventVS;
    }
    
    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public TagVSDto getTagVS() {
        return this.tagVS;
    }
    
    public void setTagVS(TagVSDto tagVS) {
        this.tagVS = tagVS;
    }

}


