package com.adkor.cyclingapp.garmin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GarminSyncController {

	private final GarminSyncService garminSyncService;

	@PostMapping("/api/riders/{riderId}/garmin/sync")
	public GarminSyncResult sync(@PathVariable Long riderId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
		return garminSyncService.sync(riderId, since);
	}

}
