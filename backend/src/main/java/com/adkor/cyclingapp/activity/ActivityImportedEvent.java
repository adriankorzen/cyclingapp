package com.adkor.cyclingapp.activity;

/**
 * Published when a new activity with its original file has been imported.
 */
public record ActivityImportedEvent(Long activityId, Long riderId) {
}
