package com.adkor.cyclingapp.garmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GarminClientTest {

	private static final String BASE_URL = "https://garmin.test";
	private static final String TOKEN_URL = BASE_URL + "/personalapitoken-service/personal-api/personal-api-key/token";
	private static final String SEARCH_URL = BASE_URL + "/activitylist-service/activities/search/activities"
			+ "?activityType=cycling&startDate=2026-10-01&sortOrder=asc&start=0&limit=50";

	private static final String ACTIVITIES_JSON = """
			[{
			  "activityId": 24806987503,
			  "activityName": "Evening Ride",
			  "activityType": { "typeKey": "road_biking", "typeId": 10 },
			  "startTimeGMT": "2026-10-21 17:24:42",
			  "startTimeLocal": "2026-10-21 19:24:42",
			  "duration": 7105.89,
			  "distance": 61049.24,
			  "avgPower": 220.0,
			  "normPower": 246.0,
			  "averageHR": 132.0,
			  "unknownField": "ignored"
			}]
			""";

	private MockRestServiceServer server;
	private GarminClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		GarminProperties properties = new GarminProperties(BASE_URL, "test-key", Duration.ofHours(23), 30);
		GarminTokenProvider tokenProvider = new GarminTokenProvider(builder.clone(), properties);
		client = new GarminClient(builder.clone(), properties, tokenProvider);
	}

	@Test
	void exchangesApiKeyForTokenAndSearchesActivities() {
		expectTokenRequest("token-1");
		server.expect(requestTo(SEARCH_URL))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer token-1"))
				.andRespond(withSuccess(ACTIVITIES_JSON, MediaType.APPLICATION_JSON));

		List<GarminActivity> activities = client.searchCyclingActivities(LocalDate.of(2026, 10, 1), 0, 50);

		server.verify();
		assertThat(activities).hasSize(1);
		GarminActivity activity = activities.getFirst();
		assertThat(activity.activityId()).isEqualTo(24806987503L);
		assertThat(activity.activityType().typeKey()).isEqualTo("road_biking");
		assertThat(activity.startTimeGMT()).isEqualTo(LocalDateTime.of(2026, 10, 21, 17, 24, 42));
		assertThat(activity.normPower()).isEqualTo(246.0);
	}

	@Test
	void reusesCachedToken() {
		expectTokenRequest("token-1");
		server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
		server.expect(requestTo(SEARCH_URL))
				.andExpect(header("Authorization", "Bearer token-1"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		client.searchCyclingActivities(LocalDate.of(2026, 10, 1), 0, 50);
		client.searchCyclingActivities(LocalDate.of(2026, 10, 1), 0, 50);

		server.verify();
	}

	@Test
	void retriesWithNewTokenWhenTokenIsRejected() {
		expectTokenRequest("expired-token");
		server.expect(requestTo(SEARCH_URL)).andRespond(withUnauthorizedRequest());
		expectTokenRequest("fresh-token");
		server.expect(requestTo(SEARCH_URL))
				.andExpect(header("Authorization", "Bearer fresh-token"))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		List<GarminActivity> activities = client.searchCyclingActivities(LocalDate.of(2026, 10, 1), 0, 50);

		server.verify();
		assertThat(activities).isEmpty();
	}

	@Test
	void extractsFitFileFromDownloadedArchive() throws IOException {
		byte[] fitContent = { 14, 16, 1, 2, 3 };
		expectTokenRequest("token-1");
		server.expect(requestTo(BASE_URL + "/download-service/files/activity/123"))
				.andRespond(withSuccess(zip("123_ACTIVITY.fit", fitContent), MediaType.parseMediaType("application/x-zip-compressed")));

		byte[] result = client.downloadFitFile(123L);

		assertThat(result).isEqualTo(fitContent);
	}

	@Test
	void wrapsGarminErrors() {
		expectTokenRequest("token-1");
		server.expect(requestTo(SEARCH_URL)).andRespond(withServerError());

		assertThatExceptionOfType(GarminApiException.class)
				.isThrownBy(() -> client.searchCyclingActivities(LocalDate.of(2026, 10, 1), 0, 50))
				.withMessageContaining("500");
	}

	private void expectTokenRequest(String token) {
		server.expect(requestTo(TOKEN_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "ApiKey test-key"))
				.andRespond(withSuccess(token, MediaType.TEXT_PLAIN));
	}

	private static byte[] zip(String entryName, byte[] content) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry(entryName));
			zip.write(content);
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

}
