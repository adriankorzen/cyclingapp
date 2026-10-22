package com.adkor.cyclingapp.rider;

import java.time.LocalDate;

import com.adkor.cyclingapp.common.ConflictException;

public class ThresholdConflictException extends ConflictException {

	public ThresholdConflictException(LocalDate effectiveFrom) {
		super("Thresholds effective from " + effectiveFrom + " already exist");
	}

}
