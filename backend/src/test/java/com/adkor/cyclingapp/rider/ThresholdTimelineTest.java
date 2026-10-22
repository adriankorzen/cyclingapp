package com.adkor.cyclingapp.rider;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ThresholdTimelineTest {

	private static final Rider RIDER = new Rider("Anna", new BigDecimal("60.0"));
	private static final RiderThreshold SEPTEMBER = threshold(LocalDate.of(2026, 9, 1), 250);
	private static final RiderThreshold OCTOBER = threshold(LocalDate.of(2026, 10, 15), 280);

	private final ThresholdTimeline timeline = new ThresholdTimeline(List.of(SEPTEMBER, OCTOBER));

	@Test
	void resolvesEntryValidOnTheDay() {
		assertThat(timeline.at(LocalDate.of(2026, 9, 1))).contains(SEPTEMBER);
		assertThat(timeline.at(LocalDate.of(2026, 10, 14))).contains(SEPTEMBER);
		assertThat(timeline.at(LocalDate.of(2026, 10, 15))).contains(OCTOBER);
		assertThat(timeline.at(LocalDate.of(2027, 1, 1))).contains(OCTOBER);
	}

	@Test
	void fallsBackToOldestEntryBeforeHistoryStarts() {
		assertThat(timeline.at(LocalDate.of(2026, 1, 1))).contains(SEPTEMBER);
	}

	@Test
	void isEmptyWithoutEntries() {
		assertThat(new ThresholdTimeline(List.of()).at(LocalDate.of(2026, 1, 1))).isEmpty();
	}

	private static RiderThreshold threshold(LocalDate effectiveFrom, int ftpWatts) {
		RiderThreshold threshold = new RiderThreshold(RIDER, effectiveFrom);
		threshold.setFtpWatts(ftpWatts);
		return threshold;
	}

}
