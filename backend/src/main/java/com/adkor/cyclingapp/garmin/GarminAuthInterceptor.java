package com.adkor.cyclingapp.garmin;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the bearer token to Garmin API requests. When Garmin rejects the token
 * (e.g. the key was rotated), the request is retried once with a fresh token.
 */
class GarminAuthInterceptor implements ClientHttpRequestInterceptor {

	private final GarminTokenProvider tokenProvider;

	GarminAuthInterceptor(GarminTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		request.getHeaders().setBearerAuth(tokenProvider.getToken());
		ClientHttpResponse response = execution.execute(request, body);
		if (response.getStatusCode() != HttpStatus.UNAUTHORIZED) {
			return response;
		}
		response.close();
		tokenProvider.invalidate();
		request.getHeaders().setBearerAuth(tokenProvider.getToken());
		return execution.execute(request, body);
	}

}
