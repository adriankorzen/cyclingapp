package com.adkor.cyclingapp.activity;

import com.adkor.cyclingapp.common.ResourceNotFoundException;

public class ActivityNotFoundException extends ResourceNotFoundException {

	public ActivityNotFoundException(Long id) {
		super("Activity with id " + id + " not found");
	}

	public static ActivityNotFoundException fileOf(Long id) {
		return new ActivityNotFoundException("File of activity with id " + id + " not found");
	}

	private ActivityNotFoundException(String message) {
		super(message);
	}

}
