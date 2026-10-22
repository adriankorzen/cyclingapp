package com.adkor.cyclingapp.rider;

import com.adkor.cyclingapp.common.ResourceNotFoundException;

public class ThresholdNotFoundException extends ResourceNotFoundException {

	public ThresholdNotFoundException(Long id) {
		super("Threshold entry with id " + id + " not found");
	}

}
