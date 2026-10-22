package com.adkor.cyclingapp.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Automatically finds the work blocks of any structured session.
 * <p>
 * Instead of one fixed threshold (which labels every surge off a traffic light as an interval
 * and misses 30 s repeats), it searches a grid of threshold / minimum duration / gap settings and
 * keeps the set whose repeats are most alike - that is what distinguishes a planned interval set
 * from incidental surges.
 */
final class WorkBlockDetector {

	/** Search space covering 15 s sprints up to 20 min threshold blocks, from tempo (70% FTP) up. */
	private static final double[] SEARCH_THRESHOLDS = { 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00, 1.05, 1.10, 1.15, 1.25 };
	private static final int[] SEARCH_DURATIONS = { 15, 20, 30, 45, 60, 90, 120, 180, 240, 300 };
	private static final int[] SEARCH_GAPS = { 5, 15, 30, 60, 90 };

	private static final int SMOOTHING_WINDOW = 10;
	private static final int MAX_BLOCKS = 40;
	private static final int MANUAL_DEFAULT_MIN_DURATION = 60;
	private static final int MANUAL_DEFAULT_MAX_GAP = 10;

	enum Mode {
		AUTO, MANUAL
	}

	/**
	 * Detected blocks (empty when the session is not structured) together with the settings
	 * that produced them and, in auto mode, the coefficients of variation of the repeats.
	 */
	record Detection(
			Mode mode,
			List<WorkBlock> blocks,
			Double relativeThreshold,
			Integer minDurationSeconds,
			Integer maxGapSeconds,
			Double meanDurationSeconds,
			Double durationConsistency,
			Double powerConsistency) {

		static Detection none(Mode mode) {
			return new Detection(mode, List.of(), null, null, null, null, null, null);
		}

	}

	private WorkBlockDetector() {
	}

	/**
	 * @param power one sample per second
	 * @param heartRate optional heart rate of the same length, used to confirm the blocks
	 */
	static Detection detect(double[] power, double ftp, double[] heartRate, DetectionOptions options) {
		if (ftp <= 0 || power.length == 0) {
			return Detection.none(options.isManual() ? Mode.MANUAL : Mode.AUTO);
		}
		double[] smoothed = smoothPower(power);
		PrefixSums powerSums = new PrefixSums(power);

		if (options.isManual()) {
			int minDuration = options.minDurationSeconds() != null ? options.minDurationSeconds() : MANUAL_DEFAULT_MIN_DURATION;
			int maxGap = options.maxGapSeconds() != null ? options.maxGapSeconds() : MANUAL_DEFAULT_MAX_GAP;
			List<WorkBlock> blocks = segment(threshold(smoothed, options.relativeThreshold() * ftp), minDuration, maxGap);
			return new Detection(Mode.MANUAL, blocks, options.relativeThreshold(), minDuration, maxGap,
					blocks.isEmpty() ? null : meanDuration(blocks), null, null);
		}

		List<WorkBlock> bestBlocks = List.of();
		double bestScore = -1.0;
		double bestThreshold = 0;
		int bestMinDuration = 0;
		int bestGap = 0;
		for (double relativeThreshold : SEARCH_THRESHOLDS) {
			boolean[] mask = threshold(smoothed, relativeThreshold * ftp);
			if (!any(mask)) {
				continue;
			}
			for (int minDuration : SEARCH_DURATIONS) {
				for (int gap : SEARCH_GAPS) {
					List<WorkBlock> blocks = segment(mask, minDuration, gap);
					if (blocks.size() < 2 || blocks.size() > MAX_BLOCKS) {
						continue;
					}
					double score = score(powerSums, blocks);
					if (score > bestScore) {
						bestBlocks = blocks;
						bestScore = score;
						bestThreshold = relativeThreshold;
						bestMinDuration = minDuration;
						bestGap = gap;
					}
				}
			}
		}

		if (!isStructured(powerSums, bestBlocks, heartRate)) {
			return Detection.none(Mode.AUTO);
		}
		return new Detection(Mode.AUTO, bestBlocks, bestThreshold, bestMinDuration, bestGap,
				meanDuration(bestBlocks),
				Signals.coefficientOfVariation(durations(bestBlocks)),
				Signals.coefficientOfVariation(blockMeans(powerSums, bestBlocks)));
	}

	/**
	 * Rolling median - removes 1-3 s spikes (accelerations from lights, potholes, power meter dropouts)
	 * without shifting the level of real intervals.
	 */
	static double[] smoothPower(double[] power) {
		return Signals.centeredRollingMedian(power, SMOOTHING_WINDOW);
	}

	/**
	 * Turns a work mask into blocks: finds the runs of work first, then merges neighbouring runs
	 * separated by at most {@code maxGap} samples, and finally drops blocks shorter than {@code minDuration}.
	 */
	static List<WorkBlock> segment(boolean[] mask, int minDuration, int maxGap) {
		List<int[]> merged = new ArrayList<>();
		int i = 0;
		while (i < mask.length) {
			if (!mask[i]) {
				i++;
				continue;
			}
			int start = i;
			while (i < mask.length && mask[i]) {
				i++;
			}
			int[] last = merged.isEmpty() ? null : merged.getLast();
			if (last != null && start - last[1] <= maxGap) {
				// Short dip - same effort
				last[1] = i;
			}
			else {
				merged.add(new int[] { start, i });
			}
		}
		return merged.stream()
				.filter(run -> run[1] - run[0] >= minDuration)
				.map(run -> new WorkBlock(run[0], run[1]))
				.toList();
	}

	/**
	 * A real interval set consists of repeats that resemble each other in both duration and power.
	 * Random surges are irregular, so they score badly. Mild bonus for capturing more work time
	 * so the detector does not settle for one tiny fragment of a long interval.
	 */
	private static double score(PrefixSums power, List<WorkBlock> blocks) {
		if (blocks.size() < 2) {
			return -1.0;
		}
		double[] durations = durations(blocks);
		double consistency = 1.0 / (1.0 + 3.0 * Signals.coefficientOfVariation(durations)
				+ 3.0 * Signals.coefficientOfVariation(blockMeans(power, blocks)));
		return consistency * Math.pow(Signals.sum(durations), 0.25);
	}

	/** Rejects a candidate set that is just noise from an unstructured ride. */
	private static boolean isStructured(PrefixSums power, List<WorkBlock> blocks, double[] heartRate) {
		if (blocks.size() < 2) {
			return false;
		}
		double[] durations = durations(blocks);
		double total = Signals.sum(durations);
		double meanDuration = Signals.mean(durations);

		if (total < 90) {
			// Too little work to be a session
			return false;
		}
		if (meanDuration < 60 && blocks.size() < 3) {
			// Short reps always come as a set
			return false;
		}
		if (meanDuration < 25 && blocks.size() < 4) {
			return false;
		}
		if (Signals.coefficientOfVariation(durations) > 0.60
				|| Signals.coefficientOfVariation(blockMeans(power, blocks)) > 0.30) {
			// Repeats too dissimilar
			return false;
		}

		// Heart rate confirmation: a planned interval drags HR clearly above the ride average,
		// a 20 s surge off a traffic light does not move it at all
		if (heartRate != null && heartRate.length == power.length()) {
			double overall = Signals.nanMean(heartRate, 0, heartRate.length);
			if (!Double.isNaN(overall) && overall > 0) {
				double[] blockHeartRates = blocks.stream()
						.mapToDouble(block -> Signals.nanMean(heartRate, block.start(), block.end()))
						.toArray();
				double medianBlockHeartRate = Signals.median(blockHeartRates);
				// A NaN median (block without HR) does not reject the set, as in numpy comparisons
				if (medianBlockHeartRate < overall * 1.03) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean[] threshold(double[] values, double threshold) {
		boolean[] mask = new boolean[values.length];
		for (int i = 0; i < values.length; i++) {
			mask[i] = values[i] >= threshold;
		}
		return mask;
	}

	private static boolean any(boolean[] mask) {
		for (boolean value : mask) {
			if (value) {
				return true;
			}
		}
		return false;
	}

	private static double[] durations(List<WorkBlock> blocks) {
		return blocks.stream().mapToDouble(WorkBlock::duration).toArray();
	}

	private static double meanDuration(List<WorkBlock> blocks) {
		return Signals.mean(durations(blocks));
	}

	private static double[] blockMeans(PrefixSums power, List<WorkBlock> blocks) {
		return blocks.stream().mapToDouble(block -> power.mean(block.start(), block.end())).toArray();
	}

	/** Constant time range means - the grid search evaluates hundreds of candidate block sets. */
	private static final class PrefixSums {

		private final double[] sums;

		PrefixSums(double[] values) {
			sums = new double[values.length + 1];
			for (int i = 0; i < values.length; i++) {
				sums[i + 1] = sums[i] + values[i];
			}
		}

		double mean(int from, int to) {
			return (sums[to] - sums[from]) / (to - from);
		}

		int length() {
			return sums.length - 1;
		}

	}

}
