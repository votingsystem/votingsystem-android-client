package org.votingsystem.dto;

import org.votingsystem.dto.voting.EventVSDto;

import java.io.Serializable;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CommentVSDto implements Comparable, Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long messageCMSId;
    private String content;
    private UserVSDto userVS;
    private EventVSDto eventVS;
    private Date dateCreated;
    private Date lastUpdated;


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

	public void setEventVS(EventVSDto eventVS) {
		this.eventVS = eventVS;
	}

	public EventVSDto getEventVS() {
		return eventVS;
	}

    public Long getMessageCMSId() {
        return messageCMSId;
    }

    public void setMessageCMSId(Long messageCMSId) {
        this.messageCMSId = messageCMSId;
    }

    @Override public int compareTo(Object comment) {
        return dateCreated.compareTo(((CommentVSDto)comment).dateCreated);
    }

}
