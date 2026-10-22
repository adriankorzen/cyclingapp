package com.adkor.cyclingapp.garmin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client of the Garmin Connect Personal API.
 */
@Component
public class GarminClient {

	private static final String SEARCH_ACTIVITIES_PATH = "/activitylist-service/activities/search/activities";
	private static final String DOWNLOAD_ACTIVITY_PATH = "/download-service/files/activity/{activityId}";
	private static final String CYCLING_ACTIVITY_TYPE = "cycling";

	private final RestClient restClient;

	public GarminClient(RestClient.Builder restClientBuilder, GarminProperties properties,
			GarminTokenProvider tokenProvider) {
		this.restClient = restClientBuilder
				.baseUrl(properties.baseUrl())
				.requestInterceptor(new GarminAuthInterceptor(tokenProvider))
				.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
					throw new GarminApiException("Garmin API request " + request.getURI().getPath()
							+ " failed with status " + response.getStatusCode());
				})
				.build();
	}

	/**
	 * Returns cycling activities (all cycling subtypes) that started on or after the given local date,
	 * oldest first.
	 */
	public List<GarminActivity> searchCyclingActivities(LocalDate startDate, int start, int limit) {
		List<GarminActivity> activities = restClient.get()
				.uri(uri -> uri.path(SEARCH_ACTIVITIES_PATH)
						.queryParam("activityType", CYCLING_ACTIVITY_TYPE)
						.queryParam("startDate", startDate)
						.queryParam("sortOrder", "asc")
						.queryParam("start", start)
						.queryParam("limit", limit)
						.build())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		return activities == null ? List.of() : activities;
	}

	/**
	 * Downloads the original FIT file of an activity. Garmin delivers it wrapped in a ZIP archive.
	 */
	public byte[] downloadFitFile(long activityId) {
		byte[] archive = restClient.get()
				.uri(DOWNLOAD_ACTIVITY_PATH, activityId)
				.retrieve()
				.body(byte[].class);
		if (archive == null) {
			throw new GarminApiException("Garmin returned no file for activity " + activityId);
		}
		return extractFitFile(archive, activityId);
	}

	private static byte[] extractFitFile(byte[] archive, long activityId) {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".fit")) {
					return zip.readAllBytes();
				}
			}
		}
		catch (IOException ex) {
			throw new GarminApiException("Cannot read file archive of activity " + activityId, ex);
		}
		throw new GarminApiException("File archive of activity " + activityId + " contains no FIT file");
	}

}
