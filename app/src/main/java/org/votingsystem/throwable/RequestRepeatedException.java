package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class RequestRepeatedException extends ExceptionBase {

    public RequestRepeatedException(String message) {
        super(message);
    }

    public RequestRepeatedException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }

}
