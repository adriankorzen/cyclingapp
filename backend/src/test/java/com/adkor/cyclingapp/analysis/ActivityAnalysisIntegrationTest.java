package com.adkor.cyclingapp.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.adkor.cyclingapp.TestcontainersConfiguration;
import com.adkor.cyclingapp.activity.ActivityData;
import com.adkor.cyclingapp.activity.ActivityService;
import com.adkor.cyclingapp.activity.ActivitySource;
import com.adkor.cyclingapp.fit.FitTestFiles;
import com.adkor.cyclingapp.rider.Rider;
import com.adkor.cyclingapp.rider.RiderRepository;
import com.adkor.cyclingapp.rider.RiderThreshold;
import com.adkor.cyclingapp.rider.RiderThresholdRepository;

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

	@Autowired
	private StoredAnalysisRepository storedAnalysisRepository;

	@Autowired
	private RiderThresholdRepository thresholdRepository;

	private Rider rider;

	@BeforeEach
	void setUp() {
		riderRepository.deleteAll();
		rider = riderRepository.save(new Rider("Anna", new BigDecimal("60.0")));
		RiderThreshold threshold = new RiderThreshold(rider, LocalDate.of(2026, 9, 1));
		threshold.setFtpWatts(TestSessions.FTP);
		threshold.setLthrBpm(165);
		threshold.setMaxHeartRateBpm(180);
		thresholdRepository.save(threshold);
	}

	@Test
	void analysesActivityRightAfterImport() {
		Long activityId = importIntervalSession("1", 21);

		StoredAnalysis stored = storedAnalysisRepository.findById(activityId).orElseThrow();
		assertThat(stored.getAnalyzerVersion()).isEqualTo(ActivityAnalyzer.VERSION);
		assertThat(stored.getFtpWatts()).isEqualTo(TestSessions.FTP);
		assertThat(stored.getTrainingStressScore()).isPositive();
		assertThat(stored.getIntervalCount()).isEqualTo(TestSessions.REPEATS);
	}

	@Test
	void returnsStoredAnalysis() throws Exception {
		Long activityId = importIntervalSession("1", 21);

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stored").value(true))
				.andExpect(jsonPath("$.analyzerVersion").value(ActivityAnalyzer.VERSION))
				.andExpect(jsonPath("$.thresholds.ftpWatts").value(TestSessions.FTP))
				.andExpect(jsonPath("$.analysis.sport").value("cycling"))
				.andExpect(jsonPath("$.analysis.power.normalizedPower").isNumber())
				.andExpect(jsonPath("$.analysis.powerZones", hasSize(7)))
				.andExpect(jsonPath("$.analysis.heartRateZones", hasSize(5)))
				.andExpect(jsonPath("$.analysis.intervals.detected").value(true))
				.andExpect(jsonPath("$.analysis.intervals.blocks", hasSize(TestSessions.REPEATS)))
				.andExpect(jsonPath("$.analysis.intervals.targetPower", closeTo(320.0, 10.0)));
	}

	@Test
	void analysesAdHocWithCustomSettingsWithoutStoring() throws Exception {
		Long activityId = importIntervalSession("1", 21);

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId)
				.param("relativeThreshold", "1.1")
				.param("targetWatts", "300"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stored").value(false))
				.andExpect(jsonPath("$.analysis.intervals.mode").value("MANUAL"))
				.andExpect(jsonPath("$.analysis.intervals.targetSource").value("MANUAL"))
				.andExpect(jsonPath("$.analysis.intervals.targetPower").value(300.0));

		assertThat(storedAnalysisRepository.findById(activityId).orElseThrow().getResult()).doesNotContain("MANUAL");
	}

	@Test
	void newFtpTestOnlyAffectsActivitiesFromItsDate() throws Exception {
		Long before = importIntervalSession("1", 10);
		Long onTestDay = importIntervalSession("2", 20);
		Long after = importIntervalSession("3", 21);
		double tssAtOldFtp = tssOf(before);
		Instant beforeAnalyzedAt = storedAnalysisRepository.findById(before).orElseThrow().getAnalyzedAt();

		mockMvc.perform(post("/api/riders/{id}/thresholds", rider.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "effectiveFrom": "2026-10-20", "ftpWatts": 300 }
						"""))
				.andExpect(status().isCreated());

		StoredAnalysis unchanged = storedAnalysisRepository.findById(before).orElseThrow();
		assertThat(unchanged.getFtpWatts()).isEqualTo(TestSessions.FTP);
		assertThat(unchanged.getTrainingStressScore()).isEqualTo(tssAtOldFtp);
		assertThat(unchanged.getAnalyzedAt()).isEqualTo(beforeAnalyzedAt);
		for (Long activityId : new Long[] { onTestDay, after }) {
			StoredAnalysis recalculated = storedAnalysisRepository.findById(activityId).orElseThrow();
			assertThat(recalculated.getFtpWatts()).isEqualTo(300);
			// LTHR carried over from the previous entry
			assertThat(recalculated.getLthrBpm()).isEqualTo(165);
			// Same ride at a higher FTP is relatively easier
			assertThat(recalculated.getTrainingStressScore()).isLessThan(tssAtOldFtp);
		}
	}

	@Test
	void reanalysesWithThresholdsOfTheActivityDay() throws Exception {
		Long activityId = importIntervalSession("1", 21);
		RiderThreshold later = new RiderThreshold(rider, LocalDate.of(2026, 10, 22));
		later.setFtpWatts(320);
		thresholdRepository.save(later);

		mockMvc.perform(post("/api/activities/{id}/analysis", activityId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.thresholds.ftpWatts").value(TestSessions.FTP));
	}

	@Test
	void refreshesMissingAnalysesOrAll() throws Exception {
		Long first = importIntervalSession("1", 20);
		importIntervalSession("2", 21);
		storedAnalysisRepository.deleteById(first);

		mockMvc.perform(post("/api/riders/{id}/analyses/refresh", rider.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.analyzed").value(1))
				.andExpect(jsonPath("$.skipped").value(1))
				.andExpect(jsonPath("$.failed").value(0));

		mockMvc.perform(post("/api/riders/{id}/analyses/refresh", rider.getId()).param("all", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.analyzed").value(2))
				.andExpect(jsonPath("$.skipped").value(0));
	}

	@Test
	void listsSummariesWithinPeriod() throws Exception {
		importIntervalSession("1", 10);
		importIntervalSession("2", 20);
		importIntervalSession("3", 21);

		mockMvc.perform(get("/api/riders/{id}/analyses", rider.getId())
				.param("from", "2026-10-15")
				.param("to", "2026-10-21"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].startTimeLocal").value("2026-10-20T08:00:00"))
				.andExpect(jsonPath("$[0].trainingStressScore").isNumber())
				.andExpect(jsonPath("$[0].intervalCount").value(TestSessions.REPEATS))
				.andExpect(jsonPath("$[1].startTimeLocal").value("2026-10-21T08:00:00"));
	}

	@Test
	void importSucceedsEvenWhenAnalysisFails() throws Exception {
		Long activityId = importActivity("2", 21, "corrupted".getBytes());

		assertThat(storedAnalysisRepository.findById(activityId)).isEmpty();
		mockMvc.perform(get("/api/activities/{id}/analysis", activityId))
				.andExpect(status().isUnprocessableContent());
		mockMvc.perform(post("/api/riders/{id}/analyses/refresh", rider.getId()))
				.andExpect(jsonPath("$.failed").value(1));
	}

	@Test
	void rejectsInvalidParameters() throws Exception {
		Long activityId = importIntervalSession("1", 21);

		mockMvc.perform(get("/api/activities/{id}/analysis", activityId).param("targetWatts", "-5"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsNotFoundForUnknownActivityOrRider() throws Exception {
		mockMvc.perform(get("/api/activities/{id}/analysis", 999_999)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/riders/{id}/analyses", 999_999)).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/riders/{id}/analyses/refresh", 999_999)).andExpect(status().isNotFound());
	}

	private double tssOf(Long activityId) {
		return storedAnalysisRepository.findById(activityId).orElseThrow().getTrainingStressScore();
	}

	private Long importIntervalSession(String externalId, int dayOfMonth) {
		double[][] session = TestSessions.intervalSession();
		return importActivity(externalId, dayOfMonth, FitTestFiles.cyclingActivity(session[0], session[1]));
	}

	private Long importActivity(String externalId, int dayOfMonth, byte[] fitFile) {
		LocalDateTime startLocal = LocalDateTime.of(2026, 10, dayOfMonth, 8, 0);
		ActivityData data = new ActivityData(ActivitySource.GARMIN, externalId, "Ride", "road_biking",
				Instant.parse("2026-10-%02dT06:00:00Z".formatted(dayOfMonth)), startLocal, null, null, null, null,
				null, null, null, null, null, null, null);
		return activityService.importActivity(rider.getId(), data, "FIT", fitFile).id();
	}

}
