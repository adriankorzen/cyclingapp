package com.adkor.cyclingapp.rider;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.adkor.cyclingapp.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RiderControllerIntegrationTest {

	private static final String VALID_RIDER = """
			{
			  "name": "Jan Kowalski",
			  "weightKg": 72.5,
			  "restingHeartRateBpm": 48
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RiderRepository riderRepository;

	@Autowired
	private RiderThresholdRepository thresholdRepository;

	@BeforeEach
	void cleanDatabase() {
		riderRepository.deleteAll();
	}

	@Test
	void createsRider() throws Exception {
		mockMvc.perform(post("/api/riders").contentType(MediaType.APPLICATION_JSON).content(VALID_RIDER))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/riders/" + singleRiderId())))
				.andExpect(jsonPath("$.name").value("Jan Kowalski"))
				.andExpect(jsonPath("$.weightKg").value(72.5))
				.andExpect(jsonPath("$.currentThresholds").doesNotExist())
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void showsCurrentThresholdsAndWattsPerKg() throws Exception {
		Rider rider = saveRider("Anna");
		saveThreshold(rider, LocalDate.now().minusDays(30), 230, 160);
		saveThreshold(rider, LocalDate.now().minusDays(2), 250, 165);

		mockMvc.perform(get("/api/riders/{id}", rider.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentThresholds.ftpWatts").value(250))
				.andExpect(jsonPath("$.ftpWattsPerKg").value(3.57));
	}

	@Test
	void rejectsInvalidRider() throws Exception {
		String invalid = """
				{ "name": "", "weightKg": 10, "restingHeartRateBpm": 5 }
				""";

		mockMvc.perform(post("/api/riders").contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.weightKg").exists())
				.andExpect(jsonPath("$.errors.restingHeartRateBpm").exists());
	}

	@Test
	void returnsNotFoundForMissingRider() throws Exception {
		mockMvc.perform(get("/api/riders/{id}", 999_999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Rider with id 999999 not found"));
	}

	@Test
	void listsRiders() throws Exception {
		saveRider("Anna");
		saveRider("Piotr");

		mockMvc.perform(get("/api/riders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void updatesRider() throws Exception {
		Long id = saveRider("Anna").getId();

		mockMvc.perform(put("/api/riders/{id}", id).contentType(MediaType.APPLICATION_JSON).content(VALID_RIDER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Jan Kowalski"))
				.andExpect(jsonPath("$.restingHeartRateBpm").value(48));
	}

	@Test
	void deletesRider() throws Exception {
		Long id = saveRider("Anna").getId();

		mockMvc.perform(delete("/api/riders/{id}", id))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/riders/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsCogganZonesForTheGivenDay() throws Exception {
		Rider rider = saveRider("Anna");
		saveThreshold(rider, LocalDate.of(2026, 9, 1), 250, 170);
		saveThreshold(rider, LocalDate.of(2026, 10, 1), 280, 172);

		mockMvc.perform(get("/api/riders/{id}/zones", rider.getId()).param("date", "2026-09-15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.thresholdsEffectiveFrom").value("2026-09-01"))
				.andExpect(jsonPath("$.ftpWatts").value(250))
				.andExpect(jsonPath("$.powerZones", hasSize(7)))
				.andExpect(jsonPath("$.powerZones[3].name").value("Lactate Threshold"))
				.andExpect(jsonPath("$.powerZones[3].min").value(226))
				.andExpect(jsonPath("$.powerZones[3].max").value(263))
				.andExpect(jsonPath("$.heartRateZones", hasSize(5)))
				.andExpect(jsonPath("$.heartRateZones[4].max").doesNotExist());

		mockMvc.perform(get("/api/riders/{id}/zones", rider.getId()))
				.andExpect(jsonPath("$.ftpWatts").value(280));
	}

	@Test
	void returnsEmptyZonesWithoutThresholds() throws Exception {
		Rider rider = saveRider("Anna");

		mockMvc.perform(get("/api/riders/{id}/zones", rider.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.powerZones", hasSize(0)))
				.andExpect(jsonPath("$.heartRateZones", hasSize(0)));
	}

	private Rider saveRider(String name) {
		return riderRepository.save(new Rider(name, new BigDecimal("70.0")));
	}

	private void saveThreshold(Rider rider, LocalDate effectiveFrom, int ftpWatts, int lthrBpm) {
		RiderThreshold threshold = new RiderThreshold(rider, effectiveFrom);
		threshold.setFtpWatts(ftpWatts);
		threshold.setLthrBpm(lthrBpm);
		thresholdRepository.save(threshold);
	}

	private Long singleRiderId() {
		return riderRepository.findAll().getFirst().getId();
	}

}
