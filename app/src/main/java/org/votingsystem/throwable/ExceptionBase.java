package org.votingsystem.throwable;

import org.votingsystem.dto.MessageDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ExceptionBase extends Exception {

    private MessageDto messageDto;

    public ExceptionBase(String message) {
        super(message);
    }

    public ExceptionBase(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionBase(String message, MessageDto messageDto) {
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
