package com.adkor.cyclingapp.common;

/**
 * Base class for exceptions signalling that content cannot be processed (HTTP 422),
 * e.g. a corrupted activity file.
 */
public abstract class UnprocessableContentException extends RuntimeException {

	protected UnprocessableContentException(String message) {
		super(message);
	}

	protected UnprocessableContentException(String message, Throwable cause) {
		super(message, cause);
	}

}
