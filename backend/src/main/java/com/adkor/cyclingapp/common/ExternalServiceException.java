package com.adkor.cyclingapp.common;

/**
 * Base class for failures of external services the application depends on (HTTP 502).
 */
public abstract class ExternalServiceException extends RuntimeException {

	protected ExternalServiceException(String message) {
		super(message);
	}

	protected ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
