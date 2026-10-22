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
			  "ftpWatts": 250,
			  "lthrBpm": 170,
			  "maxHeartRateBpm": 190,
			  "restingHeartRateBpm": 48
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RiderRepository riderRepository;

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
				.andExpect(jsonPath("$.ftpWatts").value(250))
				.andExpect(jsonPath("$.ftpWattsPerKg").value(3.45))
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void rejectsInvalidRider() throws Exception {
		String invalid = """
				{ "name": "", "weightKg": 10, "ftpWatts": -5 }
				""";

		mockMvc.perform(post("/api/riders").contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.weightKg").exists())
				.andExpect(jsonPath("$.errors.ftpWatts").exists());
	}

	@Test
	void returnsNotFoundForMissingRider() throws Exception {
		mockMvc.perform(get("/api/riders/{id}", 999_999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Rider with id 999999 not found"));
	}

	@Test
	void listsRiders() throws Exception {
		saveRider("Anna", 250);
		saveRider("Piotr", 300);

		mockMvc.perform(get("/api/riders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void updatesRider() throws Exception {
		Long id = saveRider("Anna", 200).getId();

		mockMvc.perform(put("/api/riders/{id}", id).contentType(MediaType.APPLICATION_JSON).content(VALID_RIDER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Jan Kowalski"))
				.andExpect(jsonPath("$.ftpWatts").value(250));
	}

	@Test
	void deletesRider() throws Exception {
		Long id = saveRider("Anna", 200).getId();

		mockMvc.perform(delete("/api/riders/{id}", id))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/riders/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsCogganZones() throws Exception {
		Rider rider = saveRider("Anna", 250);
		rider.setLthrBpm(170);
		riderRepository.save(rider);

		mockMvc.perform(get("/api/riders/{id}/zones", rider.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.powerZones", hasSize(7)))
				.andExpect(jsonPath("$.powerZones[3].name").value("Lactate Threshold"))
				.andExpect(jsonPath("$.powerZones[3].min").value(226))
				.andExpect(jsonPath("$.powerZones[3].max").value(263))
				.andExpect(jsonPath("$.heartRateZones", hasSize(5)))
				.andExpect(jsonPath("$.heartRateZones[4].max").doesNotExist());
	}

	@Test
	void returnsEmptyZonesWhenThresholdsAreMissing() throws Exception {
		Rider rider = riderRepository.save(new Rider("Anna", new BigDecimal("60.0")));

		mockMvc.perform(get("/api/riders/{id}/zones", rider.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.powerZones", hasSize(0)))
				.andExpect(jsonPath("$.heartRateZones", hasSize(0)));
	}

	private Rider saveRider(String name, int ftpWatts) {
		Rider rider = new Rider(name, new BigDecimal("70.0"));
		rider.setFtpWatts(ftpWatts);
		return riderRepository.save(rider);
	}

	private Long singleRiderId() {
		return riderRepository.findAll().getFirst().getId();
	}

}
