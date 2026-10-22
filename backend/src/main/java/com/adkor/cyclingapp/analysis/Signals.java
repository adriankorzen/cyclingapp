package com.adkor.cyclingapp.analysis;

import java.util.Arrays;
import java.util.List;

/**
 * Numeric helpers over sample streams. Semantics follow numpy/pandas
 * (population standard deviation, median of an even count averages the middle values,
 * rolling windows with {@code min_periods=1}).
 */
final class Signals {

	private Signals() {
	}

	static double mean(double[] values, int from, int to) {
		if (to <= from) {
			return Double.NaN;
		}
		double sum = 0;
		for (int i = from; i < to; i++) {
			sum += values[i];
		}
		return sum / (to - from);
	}

	static double mean(double[] values) {
		return mean(values, 0, values.length);
	}

	/** Mean ignoring {@code NaN} values; {@code NaN} when there are none. */
	static double nanMean(double[] values, int from, int to) {
		double sum = 0;
		int count = 0;
		for (int i = from; i < to; i++) {
			if (!Double.isNaN(values[i])) {
				sum += values[i];
				count++;
			}
		}
		return count == 0 ? Double.NaN : sum / count;
	}

	/** Max ignoring {@code NaN} values; {@code NaN} when there are none. */
	static double nanMax(double[] values, int from, int to) {
		double max = Double.NaN;
		for (int i = from; i < to; i++) {
			if (!Double.isNaN(values[i]) && (Double.isNaN(max) || values[i] > max)) {
				max = values[i];
			}
		}
		return max;
	}

	static double max(double[] values, int from, int to) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = from; i < to; i++) {
			max = Math.max(max, values[i]);
		}
		return max;
	}

	static double sum(double[] values) {
		double sum = 0;
		for (double value : values) {
			sum += value;
		}
		return sum;
	}

	static double median(double[] values) {
		if (values.length == 0) {
			return Double.NaN;
		}
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		int middle = sorted.length / 2;
		return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2.0;
	}

	static double median(List<Double> values) {
		return median(values.stream().mapToDouble(Double::doubleValue).toArray());
	}

	/** Coefficient of variation (population std / mean); 9.9 when the mean is 0. */
	static double coefficientOfVariation(double[] values) {
		double mean = mean(values);
		if (mean == 0 || Double.isNaN(mean)) {
			return 9.9;
		}
		double squares = 0;
		for (double value : values) {
			squares += (value - mean) * (value - mean);
		}
		return Math.sqrt(squares / values.length) / mean;
	}

	/** Trailing rolling mean: window {@code [i - window + 1, i]}, clipped at the start. */
	static double[] trailingRollingMean(double[] values, int window) {
		double[] result = new double[values.length];
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
			if (i >= window) {
				sum -= values[i - window];
			}
			result[i] = sum / Math.min(i + 1, window);
		}
		return result;
	}

	/**
	 * Centered rolling median as in pandas {@code rolling(window, center=True, min_periods=1)}:
	 * window {@code [i - window/2, i + (window - 1)/2]}, clipped at both ends.
	 */
	static double[] centeredRollingMedian(double[] values, int window) {
		int before = window / 2;
		int after = (window - 1) / 2;
		double[] result = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			int from = Math.max(0, i - before);
			int to = Math.min(values.length, i + after + 1);
			result[i] = median(Arrays.copyOfRange(values, from, to));
		}
		return result;
	}

}
