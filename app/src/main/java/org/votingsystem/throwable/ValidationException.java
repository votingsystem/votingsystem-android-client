package org.votingsystem.throwable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationException extends ExceptionBase {


    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
