package com.adkor.cyclingapp.garmin;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adkor.cyclingapp.TestcontainersConfiguration;
import com.adkor.cyclingapp.activity.ActivityRepository;
import com.adkor.cyclingapp.rider.Rider;
import com.adkor.cyclingapp.rider.RiderRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GarminSyncIntegrationTest {

	private static final byte[] FIT_FILE = { 14, 16, 1, 2, 3 };

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RiderRepository riderRepository;

	@Autowired
	private ActivityRepository activityRepository;

	@MockitoBean
	private GarminClient garminClient;

	private Long riderId;

	@BeforeEach
	void setUp() {
		riderRepository.deleteAll();
		riderId = riderRepository.save(new Rider("Anna", new BigDecimal("60.0"))).getId();
		given(garminClient.downloadFitFile(anyLong())).willReturn(FIT_FILE);
	}

	@Test
	void importsNewActivitiesWithFitFiles() throws Exception {
		given(garminClient.searchCyclingActivities(any(), anyInt(), anyInt()))
				.willReturn(List.of(garminActivity(1L, 21), garminActivity(2L, 22)));

		mockMvc.perform(post("/api/riders/{id}/garmin/sync", riderId).param("since", "2026-10-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.since").value("2026-10-01"))
				.andExpect(jsonPath("$.fetched").value(2))
				.andExpect(jsonPath("$.imported").value(2));

		mockMvc.perform(get("/api/riders/{id}/activities", riderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].externalId").value("2"))
				.andExpect(jsonPath("$[0].source").value("GARMIN"))
				.andExpect(jsonPath("$[0].sportType").value("road_biking"))
				.andExpect(jsonPath("$[0].startTime").value("2026-10-22T17:00:00Z"))
				.andExpect(jsonPath("$[0].normalizedPower").value(246))
				.andExpect(jsonPath("$[0].durationSeconds").value(3600));

		Long activityId = activityRepository.findByRiderIdOrderByStartTimeDesc(riderId).getFirst().getId();
		mockMvc.perform(get("/api/activities/{id}/file", activityId))
				.andExpect(status().isOk())
				.andExpect(content().bytes(FIT_FILE));
	}

	@Test
	void skipsAlreadyImportedActivities() throws Exception {
		given(garminClient.searchCyclingActivities(any(), anyInt(), anyInt()))
				.willReturn(List.of(garminActivity(1L, 21)));
		mockMvc.perform(post("/api/riders/{id}/garmin/sync", riderId)).andExpect(status().isOk());

		given(garminClient.searchCyclingActivities(any(), anyInt(), anyInt()))
				.willReturn(List.of(garminActivity(1L, 21), garminActivity(2L, 22)));
		mockMvc.perform(post("/api/riders/{id}/garmin/sync", riderId))
				.andExpect(status().isOk())
				// Continues from the day of the latest imported activity
				.andExpect(jsonPath("$.since").value("2026-10-21"))
				.andExpect(jsonPath("$.fetched").value(2))
				.andExpect(jsonPath("$.imported").value(1));

		verify(garminClient, times(1)).downloadFitFile(1L);
		verify(garminClient, times(1)).downloadFitFile(2L);
		verify(garminClient).searchCyclingActivities(eq(LocalDate.of(2026, 10, 21)), eq(0), anyInt());
	}

	@Test
	void returnsBadGatewayWhenGarminFails() throws Exception {
		given(garminClient.searchCyclingActivities(any(), anyInt(), anyInt()))
				.willThrow(new GarminApiException("Garmin API request failed with status 500"));

		mockMvc.perform(post("/api/riders/{id}/garmin/sync", riderId))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.detail").value("Garmin API request failed with status 500"));
	}

	@Test
	void returnsNotFoundForUnknownRider() throws Exception {
		mockMvc.perform(post("/api/riders/{id}/garmin/sync", 999_999))
				.andExpect(status().isNotFound());
	}

	private static GarminActivity garminActivity(long id, int dayOfMonth) {
		LocalDateTime startGmt = LocalDateTime.of(2026, 10, dayOfMonth, 17, 0);
		return new GarminActivity(id, "Ride " + id, new GarminActivity.ActivityType("road_biking"),
				startGmt, startGmt.plusHours(2), 3600.4, 3500.0, 30000.0, 250.0,
				135.0, 170.0, 210.0, 600.0, 246.0, 88.0, 900.0);
	}

}
