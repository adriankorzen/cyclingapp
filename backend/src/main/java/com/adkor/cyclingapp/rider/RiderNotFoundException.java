package com.adkor.cyclingapp.rider;

import com.adkor.cyclingapp.common.ResourceNotFoundException;

public class RiderNotFoundException extends ResourceNotFoundException {

	public RiderNotFoundException(Long id) {
		super("Rider with id " + id + " not found");
	}

}
