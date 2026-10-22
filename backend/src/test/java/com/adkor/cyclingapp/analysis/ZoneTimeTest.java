package com.adkor.cyclingapp.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.adkor.cyclingapp.zone.CogganZones;

class ZoneTimeTest {

	@Test
	void countsSecondsUsingDisplayedZoneBounds() {
		// FTP 280: Z1 0-154 W, Z2 155-210 W, ..., Z7 421+ W
		double[] power = { 0, 154, 155, 210, 211, 500, Double.NaN };

		List<ZoneTime> zones = ZoneTime.distribution(power, CogganZones.powerZones(280));

		assertThat(zones).extracting(ZoneTime::seconds).containsExactly(2, 2, 1, 0, 0, 0, 1);
		assertThat(zones.getFirst().percent()).isCloseTo(2 * 100.0 / 7, within(1e-9));
		assertThat(zones.getLast().name()).isEqualTo("Neuromuscular Power");
	}

}
