package com.adkor.cyclingapp.rider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A rider's threshold history, resolvable for any date.
 */
public final class ThresholdTimeline {

	/** Sorted by {@code effectiveFrom}, oldest first. */
	private final List<RiderThreshold> entries;

	ThresholdTimeline(List<RiderThreshold> entries) {
		this.entries = List.copyOf(entries);
	}

	/**
	 * Returns the entry valid on the given date. Dates before the first entry fall back to the
	 * first entry - the oldest known thresholds are the best estimate for earlier activities.
	 */
	public Optional<RiderThreshold> at(LocalDate date) {
		if (entries.isEmpty()) {
			return Optional.empty();
		}
		RiderThreshold valid = entries.getFirst();
		for (RiderThreshold entry : entries) {
			if (entry.getEffectiveFrom().isAfter(date)) {
				break;
			}
			valid = entry;
		}
		return Optional.of(valid);
	}

}
