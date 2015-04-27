package org.votingsystem.throwable;

import org.votingsystem.dto.MessageDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ExceptionVS extends Exception {

    private MessageDto messageDto;

    public ExceptionVS(String message) {
        super(message);
    }

    public ExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionVS(String message, MessageDto messageDto) {
        super(message);
        this.setMessageDto(messageDto);
    }

    public MessageDto getMessageDto() {
        return messageDto;
    }

    public void setMessageDto(MessageDto messageDto) {
        this.messageDto = messageDto;
    }
}
