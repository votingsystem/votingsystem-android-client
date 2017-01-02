package org.votingsystem.dto.voting;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ElectionOptionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;
    private Long numVotes;

    public ElectionOptionDto() {}

    public ElectionOptionDto(String content, Long numVotes) {
        this.content = content;
        this.numVotes = numVotes;
    }

    public String getContent() {
        return content;
    }

    public Long getNumVotes() {
        return numVotes;
    }

    public ElectionOptionDto setContent(String content) {
        this.content = content;
        return this;
    }

    public ElectionOptionDto setNumVotes(Long numVotes) {
        this.numVotes = numVotes;
        return this;
    }

}