package com.adkor.cyclingapp.analysis;

import java.util.List;

/**
 * Result of analysing an activity recording. Sections are {@code null} when the data they need
 * (power, heart rate, thresholds) is not available.
 */
public record ActivityAnalysis(
		String sport,
		String subSport,
		int durationSeconds,
		Double distanceMeters,
		PowerMetrics power,
		HeartRateMetrics heartRate,
		Double averageCadence,
		List<ZoneTime> powerZones,
		List<ZoneTime> heartRateZones,
		Intervals intervals) {

	/**
	 * @param percentOfMax max HR as % of the rider's max HR, {@code null} when unknown
	 */
	public record HeartRateMetrics(double average, double max, Double percentOfMax) {
	}

	/**
	 * Work blocks of a structured session.
	 *
	 * @param detected whether a structured interval set was found
	 * @param mode {@code AUTO} (grid search) or {@code MANUAL} (settings given by the caller)
	 * @param targetPower reference power of the blocks (W)
	 * @param targetSource {@code AUTO} = median power of the blocks, {@code MANUAL} = given by the caller
	 * @param vo2ThresholdBpm 92% of max HR, {@code null} when max HR is unknown
	 * @param totalVo2StimulusSeconds time at or above {@code vo2ThresholdBpm} across all blocks
	 * @param cardiacDriftPercent change of efficiency (W/bpm) between the first and the last block
	 */
	public record Intervals(
			boolean detected,
			String mode,
			Double relativeThreshold,
			Integer minDurationSeconds,
			Integer maxGapSeconds,
			Double meanDurationSeconds,
			Double durationConsistency,
			Double powerConsistency,
			Double targetPower,
			String targetSource,
			List<Block> blocks,
			Double vo2ThresholdBpm,
			Integer totalVo2StimulusSeconds,
			Double cardiacDriftPercent) {
	}

	/**
	 * A single work block; times are seconds from the activity start, {@code [startSecond, endSecond)}.
	 *
	 * @param dropoutSeconds samples with zero power (coasting or power meter dropouts)
	 * @param heartRateStart mean HR of the first 30 s of the block
	 * @param heartRateEnd mean HR of the last 30 s of the block
	 * @param efficiency average power / average HR (W/bpm)
	 */
	public record Block(
			int number,
			int startSecond,
			int endSecond,
			int durationSeconds,
			int dropoutSeconds,
			double averagePower,
			Double averagePowerExcludingZeros,
			double maxPower,
			Double percentOfTarget,
			Double heartRateStart,
			Double heartRateEnd,
			Double maxHeartRate,
			Double efficiency,
			Integer vo2StimulusSeconds) {
	}

}
