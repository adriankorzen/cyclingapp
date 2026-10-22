package com.adkor.cyclingapp.analysis;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.adkor.cyclingapp.TestcontainersConfiguration;
import com.adkor.cyclingapp.activity.ActivityData;
import com.adkor.cyclingapp.activity.ActivityService;
import com.adkor.cyclingapp.activity.ActivitySource;
import com.adkor.cyclingapp.fit.FitTestFiles;
import com.adkor.cyclingapp.rider.Rider;
import com.adkor.cyclingapp.rider.RiderRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ActivityAnalysisIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RiderRepository riderRepository;

	@Autowired
	private ActivityService activityService;

	private Long riderId;

	@BeforeEach
	void setUp() {
		riderRepository.deleteAll();
		Rider rider = new Rider("Anna", new BigDecimal("60.0"));
		rider.setFtpWatts(TestSessions.FTP);
		rider.setLthrBpm(165);
		rider.setMaxHeartRateBpm(180);
		riderId = riderRepository.save(rider).getId();
	}

	@Test
	void analysesStoredFitFileWithRiderThresholds() throws Exception {
		double[][] session = TestSessions.intervalSession();
		Long activityId = importActivity("1", FitTestFiles.cyclingActivity(session[0], session[1]));

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sport").value("cycling"))
				.andExpect(jsonPath("$.durationSeconds").value(session[0].length))
				.andExpect(jsonPath("$.power.normalizedPower").isNumber())
				.andExpect(jsonPath("$.powerZones", hasSize(7)))
				.andExpect(jsonPath("$.heartRateZones", hasSize(5)))
				.andExpect(jsonPath("$.intervals.detected").value(true))
				.andExpect(jsonPath("$.intervals.blocks", hasSize(TestSessions.REPEATS)))
				.andExpect(jsonPath("$.intervals.targetPower", closeTo(320.0, 10.0)));
	}

	@Test
	void appliesManualDetectionSettings() throws Exception {
		double[][] session = TestSessions.intervalSession();
		Long activityId = importActivity("1", FitTestFiles.cyclingActivity(session[0], session[1]));

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId)
				.param("relativeThreshold", "1.1")
				.param("targetWatts", "300"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.intervals.mode").value("MANUAL"))
				.andExpect(jsonPath("$.intervals.targetSource").value("MANUAL"))
				.andExpect(jsonPath("$.intervals.targetPower").value(300.0));
	}

	@Test
	void rejectsInvalidParameters() throws Exception {
		Long activityId = importActivity("1", FitTestFiles.cyclingActivity(FitTestFiles.constant(60, 200), null));

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId).param("targetWatts", "-5"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsUnprocessableContentForCorruptedFile() throws Exception {
		Long activityId = importActivity("2", "corrupted".getBytes());

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void returnsNotFoundForUnknownActivity() throws Exception {
		mockMvc.perform(get("/api/activities/{id}/analysis", 999_999))
				.andExpect(status().isNotFound());
	}

	private Long importActivity(String externalId, byte[] fitFile) {
		ActivityData data = new ActivityData(ActivitySource.GARMIN, externalId, "Ride", "road_biking",
				Instant.parse("2026-10-21T17:00:00Z"), null, null, null, null, null, null, null, null, null, null,
				null, null);
		return activityService.importActivity(riderId, data, "FIT", fitFile).id();
	}

}
