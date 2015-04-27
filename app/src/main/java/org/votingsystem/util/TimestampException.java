package org.votingsystem.util;

import org.votingsystem.throwable.ExceptionVS;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimestampException extends ExceptionVS {

	private static final long serialVersionUID = 1L;

	public TimestampException(String message) {
		super(message);
	}
	
}
