package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldEventDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private EventVSDto eventVS;
    private String content;
    private String value;

    public FieldEventDto() {}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static FieldEventDto parse (Map fieldMap) {
        FieldEventDto fieldEvent = null;
        try {
            fieldEvent = new FieldEventDto();
            if(fieldMap.containsKey("id"))
                fieldEvent.setId(((Integer) fieldMap.get("id")).longValue());
            if(fieldMap.containsKey("content")) {
                fieldEvent.setContent((String) fieldMap.get("content"));
            }
            if(fieldMap.containsKey("value")) {
                fieldEvent.setValue((String) fieldMap.get("value"));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return fieldEvent;
    }

}