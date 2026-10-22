package com.adkor.cyclingapp.rider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;

import com.adkor.cyclingapp.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RiderThresholdIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RiderRepository riderRepository;

	@Autowired
	private JsonMapper jsonMapper;

	private Long riderId;

	@BeforeEach
	void setUp() {
		riderRepository.deleteAll();
		riderId = riderRepository.save(new Rider("Anna", new BigDecimal("60.0"))).getId();
	}

	@Test
	void carriesOverOmittedValuesFromPreviousEntry() throws Exception {
		createThreshold("""
				{ "effectiveFrom": "2026-09-01", "ftpWatts": 250, "lthrBpm": 165, "maxHeartRateBpm": 185 }
				""").andExpect(status().isCreated());

		// A new FTP test only changes FTP
		createThreshold("""
				{ "effectiveFrom": "2026-10-15", "ftpWatts": 270 }
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ftpWatts").value(270))
				.andExpect(jsonPath("$.lthrBpm").value(165))
				.andExpect(jsonPath("$.maxHeartRateBpm").value(185));

		mockMvc.perform(get("/api/riders/{id}/thresholds", riderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].effectiveFrom").value("2026-10-15"))
				.andExpect(jsonPath("$[1].effectiveFrom").value("2026-09-01"));
	}

	@Test
	void defaultsEffectiveDateToToday() throws Exception {
		createThreshold("""
				{ "ftpWatts": 250 }
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.effectiveFrom").value(LocalDate.now().toString()));
	}

	@Test
	void rejectsDuplicateDateAndFutureDate() throws Exception {
		createThreshold("""
				{ "effectiveFrom": "2026-09-01", "ftpWatts": 250 }
				""");

		createThreshold("""
				{ "effectiveFrom": "2026-09-01", "ftpWatts": 260 }
				""").andExpect(status().isConflict());
		createThreshold("""
				{ "effectiveFrom": "%s", "ftpWatts": 260 }
				""".formatted(LocalDate.now().plusDays(1))).andExpect(status().isBadRequest());
	}

	@Test
	void updatesAndDeletesEntry() throws Exception {
		Long id = idOf(createThreshold("""
				{ "effectiveFrom": "2026-09-01", "ftpWatts": 250, "lthrBpm": 165 }
				"""));

		mockMvc.perform(put("/api/riders/{riderId}/thresholds/{id}", riderId, id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "effectiveFrom": "2026-09-02", "ftpWatts": 255 }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.effectiveFrom").value("2026-09-02"))
				.andExpect(jsonPath("$.ftpWatts").value(255))
				// Update replaces the entry, omitted values become empty
				.andExpect(jsonPath("$.lthrBpm").doesNotExist());

		mockMvc.perform(delete("/api/riders/{riderId}/thresholds/{id}", riderId, id))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/riders/{id}/thresholds", riderId))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void returnsNotFoundForEntryOfAnotherRider() throws Exception {
		Long id = idOf(createThreshold("""
				{ "effectiveFrom": "2026-09-01", "ftpWatts": 250 }
				"""));
		Long otherRiderId = riderRepository.save(new Rider("Piotr", new BigDecimal("70.0"))).getId();

		mockMvc.perform(delete("/api/riders/{riderId}/thresholds/{id}", otherRiderId, id))
				.andExpect(status().isNotFound());
	}

	private ResultActions createThreshold(String json) throws Exception {
		return mockMvc.perform(post("/api/riders/{id}/thresholds", riderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private Long idOf(ResultActions result) throws Exception {
		return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
	}

}
