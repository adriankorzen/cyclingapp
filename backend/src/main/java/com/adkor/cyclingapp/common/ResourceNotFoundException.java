package com.adkor.cyclingapp.common;

/**
 * Base class for exceptions signalling that a requested resource does not exist (HTTP 404).
 */
public abstract class ResourceNotFoundException extends RuntimeException {

	protected ResourceNotFoundException(String message) {
		super(message);
	}

}
