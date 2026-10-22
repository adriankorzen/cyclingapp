package com.adkor.cyclingapp.garmin;

import com.adkor.cyclingapp.common.ExternalServiceException;

public class GarminApiException extends ExternalServiceException {

	public GarminApiException(String message) {
		super(message);
	}

	public GarminApiException(String message, Throwable cause) {
		super(message, cause);
	}

}
