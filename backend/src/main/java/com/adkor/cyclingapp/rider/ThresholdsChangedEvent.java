package com.adkor.cyclingapp.rider;

/**
 * Published when a rider's threshold history was changed (entry added, updated or deleted).
 */
public record ThresholdsChangedEvent(Long riderId) {
}
