package com.adkor.cyclingapp.common;

/**
 * Base class for exceptions signalling a conflict with the current state of a resource (HTTP 409).
 */
public abstract class ConflictException extends RuntimeException {

	protected ConflictException(String message) {
		super(message);
	}

}
