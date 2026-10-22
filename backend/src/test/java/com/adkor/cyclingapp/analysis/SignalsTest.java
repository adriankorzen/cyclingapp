package com.adkor.cyclingapp.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Expected values were produced with pandas / numpy, which the original analyzer used.
 */
class SignalsTest {

	@Test
	void centeredRollingMedianMatchesPandas() {
		// pd.Series(np.arange(20)).rolling(10, center=True, min_periods=1).median()
		double[] values = IntStream.range(0, 20).asDoubleStream().toArray();

		assertThat(Signals.centeredRollingMedian(values, 10)).containsExactly(
				2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.5, 6.5, 7.5, 8.5,
				9.5, 10.5, 11.5, 12.5, 13.5, 14.5, 15.0, 15.5, 16.0, 16.5);
	}

	@Test
	void trailingRollingMeanUsesAvailableSamplesAtTheStart() {
		// pd.Series(...).rolling(3, min_periods=1).mean()
		assertThat(Signals.trailingRollingMean(new double[] { 0, 1, 2, 3, 4 }, 3))
				.containsExactly(0.0, 0.5, 1.0, 2.0, 3.0);
	}

	@Test
	void medianOfEvenCountAveragesMiddleValues() {
		assertThat(Signals.median(new double[] { 4, 1, 3, 2 })).isEqualTo(2.5);
		assertThat(Signals.median(new double[] { 3, 1, 2 })).isEqualTo(2.0);
	}

	@Test
	void coefficientOfVariationUsesPopulationStandardDeviation() {
		// np.std([1, 2, 3, 4]) / 2.5
		assertThat(Signals.coefficientOfVariation(new double[] { 1, 2, 3, 4 }))
				.isCloseTo(1.118033988749895 / 2.5, within(1e-12));
		assertThat(Signals.coefficientOfVariation(new double[] { 0, 0 })).isEqualTo(9.9);
	}

	@Test
	void nanAwareAggregatesSkipMissingValues() {
		double[] values = { Double.NaN, 2, 4, Double.NaN };

		assertThat(Signals.nanMean(values, 0, values.length)).isEqualTo(3.0);
		assertThat(Signals.nanMax(values, 0, values.length)).isEqualTo(4.0);
		assertThat(Signals.nanMean(new double[] { Double.NaN }, 0, 1)).isNaN();
	}

}
