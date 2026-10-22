package com.adkor.cyclingapp.rider;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;

/**
 * Thresholds valid from {@code effectiveFrom} (today when omitted).
 * When creating an entry, omitted values are carried over from the entry valid before that date,
 * so e.g. a new FTP test only needs {@code ftpWatts}.
 */
public record ThresholdRequest(
		@PastOrPresent LocalDate effectiveFrom,
		@Min(30) @Max(2500) Integer ftpWatts,
		@Min(60) @Max(230) Integer lthrBpm,
		@Min(80) @Max(240) Integer maxHeartRateBpm) {
}
