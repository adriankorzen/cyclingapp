package com.adkor.cyclingapp.garmin;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Garmin Connect Personal API.
 *
 * @param baseUrl base URL of the Personal API
 * @param apiKey personal API key generated in Garmin Connect Web settings
 * @param tokenTtl how long an access token is reused before a new one is requested
 * @param initialSyncDays how many days back the first sync of a rider reaches
 */
@ConfigurationProperties("garmin.personal-api")
public record GarminProperties(
		String baseUrl,
		String apiKey,
		Duration tokenTtl,
		int initialSyncDays) {
}
