package com.adkor.cyclingapp.analysis;

import static com.adkor.cyclingapp.fit.FitTestFiles.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class PowerMetricsTest {

	@Test
	void oneHourAtFtpScoresOneHundredTss() {
		PowerMetrics metrics = PowerMetrics.calculate(constant(3600, 280), 280);

		assertThat(metrics.averagePower()).isEqualTo(280.0);
		assertThat(metrics.normalizedPower()).isCloseTo(280.0, within(1e-9));
		assertThat(metrics.variabilityIndex()).isCloseTo(1.0, within(1e-9));
		assertThat(metrics.intensityFactor()).isCloseTo(1.0, within(1e-9));
		assertThat(metrics.trainingStressScore()).isCloseTo(100.0, within(1e-9));
		assertThat(metrics.averagePowerPercentFtp()).isCloseTo(100.0, within(1e-9));
		assertThat(metrics.workKilojoules()).isCloseTo(1008.0, within(1e-9));
	}

	@Test
	void normalizedPowerWeightsHardEffortsAboveAveragePower() {
		double[] power = new double[1200];
		for (int i = 0; i < power.length; i++) {
			// Alternating 5 min blocks of 100 W and 300 W
			power[i] = (i / 300) % 2 == 0 ? 100 : 300;
		}

		PowerMetrics metrics = PowerMetrics.calculate(power, 250);

		assertThat(metrics.averagePower()).isEqualTo(200.0);
		assertThat(metrics.normalizedPower()).isGreaterThan(230.0);
		assertThat(metrics.variabilityIndex()).isGreaterThan(1.1);
		assertThat(metrics.maxPower()).isEqualTo(300.0);
	}

	@Test
	void skipsFtpRelativeMetricsWithoutFtp() {
		PowerMetrics metrics = PowerMetrics.calculate(constant(600, 200), null);

		assertThat(metrics.normalizedPower()).isCloseTo(200.0, within(1e-9));
		assertThat(metrics.intensityFactor()).isNull();
		assertThat(metrics.trainingStressScore()).isNull();
		assertThat(metrics.averagePowerPercentFtp()).isNull();
	}

}
