package com.adkor.cyclingapp.garmin;

import java.time.LocalDate;

/**
 * Outcome of a Garmin sync.
 *
 * @param since local date the sync started from
 * @param fetched number of activities returned by Garmin
 * @param imported number of activities that were new and got imported
 */
public record GarminSyncResult(LocalDate since, int fetched, int imported) {
}
