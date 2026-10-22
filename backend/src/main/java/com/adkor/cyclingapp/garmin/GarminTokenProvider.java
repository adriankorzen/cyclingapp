package com.adkor.cyclingapp.garmin;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Exchanges the personal API key for an access token and caches it until it expires.
 * Garmin does not issue refresh tokens, so a new token is simply requested with the key.
 */
@Component
public class GarminTokenProvider {

	private static final String TOKEN_PATH = "/personalapitoken-service/personal-api/personal-api-key/token";

	private final RestClient restClient;
	private final GarminProperties properties;

	private String token;
	private Instant expiresAt = Instant.MIN;

	public GarminTokenProvider(RestClient.Builder restClientBuilder, GarminProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
		this.properties = properties;
	}

	public synchronized String getToken() {
		if (token == null || Instant.now().isAfter(expiresAt)) {
			token = requestToken();
			expiresAt = Instant.now().plus(properties.tokenTtl());
		}
		return token;
	}

	public synchronized void invalidate() {
		token = null;
	}

	private String requestToken() {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new GarminApiException("Garmin API key is not configured (GARMIN_API_KEY)");
		}
		// The endpoint returns the raw token as text/plain and rejects "Accept: application/json"
		String response = restClient.post()
				.uri(TOKEN_PATH)
				.header(HttpHeaders.AUTHORIZATION, "ApiKey " + properties.apiKey())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, res) -> {
					throw new GarminApiException("Garmin token request failed with status " + res.getStatusCode());
				})
				.body(String.class);
		if (!StringUtils.hasText(response)) {
			throw new GarminApiException("Garmin token response is empty");
		}
		return response.trim();
	}

}
