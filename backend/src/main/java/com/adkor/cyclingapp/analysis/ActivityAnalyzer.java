package com.adkor.cyclingapp.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.adkor.cyclingapp.analysis.ActivityAnalysis.Block;
import com.adkor.cyclingapp.analysis.ActivityAnalysis.HeartRateMetrics;
import com.adkor.cyclingapp.analysis.ActivityAnalysis.Intervals;
import com.adkor.cyclingapp.analysis.WorkBlockDetector.Detection;
import com.adkor.cyclingapp.fit.ActivityRecording;
import com.adkor.cyclingapp.zone.CogganZones;

/**
 * Physiological analysis of a recorded activity: Coggan power metrics, time in zones,
 * detected work blocks, VO2max stimulus time and cardiac drift.
 * <p>
 * Every sample is treated as one second, as the recording is expected at 1 Hz.
 */
public final class ActivityAnalyzer {

	private static final double VO2_MAX_HEART_RATE_FRACTION = 0.92;
	private static final int BLOCK_EDGE_SECONDS = 30;

	private ActivityAnalyzer() {
	}

	public static ActivityAnalysis analyze(ActivityRecording recording, AthleteThresholds thresholds,
			DetectionOptions options) {
		double[] power = recording.power();
		double[] heartRate = recording.heartRate();
		double[] cadence = recording.cadence();
		Integer ftp = thresholds.ftpWatts();

		boolean hasPower = Arrays.stream(power).anyMatch(value -> value > 0);
		double maxHeartRate = Signals.nanMax(heartRate, 0, heartRate.length);
		boolean hasHeartRate = !Double.isNaN(maxHeartRate) && maxHeartRate > 0;

		PowerMetrics powerMetrics = hasPower ? PowerMetrics.calculate(power, ftp) : null;
		HeartRateMetrics heartRateMetrics = hasHeartRate
				? new HeartRateMetrics(Signals.nanMean(heartRate, 0, heartRate.length), maxHeartRate,
						thresholds.maxHeartRateBpm() != null ? maxHeartRate / thresholds.maxHeartRateBpm() * 100 : null)
				: null;
		double[] pedalling = Arrays.stream(cadence).filter(value -> value > 0).toArray();
		Double averageCadence = pedalling.length > 0 ? Signals.mean(pedalling) : null;

		List<ZoneTime> powerZones = hasPower && ftp != null
				? ZoneTime.distribution(power, CogganZones.powerZones(ftp))
				: List.of();
		List<ZoneTime> heartRateZones = hasHeartRate && thresholds.lthrBpm() != null
				? ZoneTime.distribution(heartRate, CogganZones.heartRateZones(thresholds.lthrBpm()))
				: List.of();

		Intervals intervals = hasPower && ftp != null
				? intervals(power, hasHeartRate ? heartRate : null, ftp, thresholds, options)
				: null;

		return new ActivityAnalysis(
				recording.sport(),
				recording.subSport(),
				recording.sampleCount(),
				recording.distanceMeters(),
				powerMetrics,
				heartRateMetrics,
				averageCadence,
				powerZones,
				heartRateZones,
				intervals);
	}

	private static Intervals intervals(double[] power, double[] heartRate, int ftp, AthleteThresholds thresholds,
			DetectionOptions options) {
		Detection detection = WorkBlockDetector.detect(power, ftp, heartRate, options);
		List<WorkBlock> workBlocks = detection.blocks();
		if (workBlocks.isEmpty()) {
			return new Intervals(false, detection.mode().name(), detection.relativeThreshold(),
					detection.minDurationSeconds(), detection.maxGapSeconds(), null, null, null,
					null, null, List.of(), null, null, null);
		}

		// The target comes from this session's own blocks (median power across the reps), so every
		// workout gets its own reference point. A manually supplied target always wins.
		boolean manualTarget = thresholds.targetWatts() != null && thresholds.targetWatts() > 0;
		double target = manualTarget
				? thresholds.targetWatts()
				: Signals.median(workBlocks.stream().mapToDouble(b -> Signals.mean(power, b.start(), b.end())).toArray());

		Integer maxHeartRateBpm = thresholds.maxHeartRateBpm();
		Double vo2Threshold = heartRate != null && maxHeartRateBpm != null
				? VO2_MAX_HEART_RATE_FRACTION * maxHeartRateBpm
				: null;

		List<Block> blocks = new ArrayList<>(workBlocks.size());
		int totalVo2Seconds = 0;
		for (int i = 0; i < workBlocks.size(); i++) {
			WorkBlock workBlock = workBlocks.get(i);
			Block block = block(i + 1, workBlock, power, heartRate, target, vo2Threshold);
			blocks.add(block);
			if (block.vo2StimulusSeconds() != null) {
				totalVo2Seconds += block.vo2StimulusSeconds();
			}
		}

		return new Intervals(
				true,
				detection.mode().name(),
				detection.relativeThreshold(),
				detection.minDurationSeconds(),
				detection.maxGapSeconds(),
				detection.meanDurationSeconds(),
				detection.durationConsistency(),
				detection.powerConsistency(),
				target,
				manualTarget ? "MANUAL" : "AUTO",
				blocks,
				vo2Threshold,
				vo2Threshold != null ? totalVo2Seconds : null,
				heartRate != null ? cardiacDrift(power, heartRate, workBlocks) : null);
	}

	private static Block block(int number, WorkBlock workBlock, double[] power, double[] heartRate, double target,
			Double vo2Threshold) {
		int start = workBlock.start();
		int end = workBlock.end();
		double averagePower = Signals.mean(power, start, end);
		double[] nonZero = Arrays.stream(power, start, end).filter(value -> value > 0).toArray();
		int dropouts = (int) Arrays.stream(power, start, end).filter(value -> value == 0).count();

		Double heartRateStart = null;
		Double heartRateEnd = null;
		Double maxHeartRate = null;
		Double efficiency = null;
		Integer vo2Seconds = null;
		if (heartRate != null) {
			heartRateStart = Signals.mean(heartRate, start, Math.min(end, start + BLOCK_EDGE_SECONDS));
			heartRateEnd = Signals.mean(heartRate, Math.max(start, end - BLOCK_EDGE_SECONDS), end);
			maxHeartRate = Signals.max(heartRate, start, end);
			double averageHeartRate = Signals.mean(heartRate, start, end);
			efficiency = averageHeartRate > 0 ? averagePower / averageHeartRate : null;
			if (vo2Threshold != null) {
				vo2Seconds = (int) Arrays.stream(heartRate, start, end).filter(value -> value >= vo2Threshold).count();
			}
		}

		return new Block(
				number,
				start,
				end,
				workBlock.duration(),
				dropouts,
				averagePower,
				nonZero.length > 0 ? Signals.mean(nonZero) : null,
				Signals.max(power, start, end),
				target > 0 ? averagePower / target * 100 : null,
				heartRateStart,
				heartRateEnd,
				maxHeartRate,
				efficiency,
				vo2Seconds);
	}

	/** Change of efficiency (W/bpm) of the last block compared to the first one, in percent. */
	private static Double cardiacDrift(double[] power, double[] heartRate, List<WorkBlock> blocks) {
		if (blocks.size() < 2) {
			return null;
		}
		WorkBlock first = blocks.getFirst();
		WorkBlock last = blocks.getLast();
		double firstHeartRate = Signals.mean(heartRate, first.start(), first.end());
		double lastHeartRate = Signals.mean(heartRate, last.start(), last.end());
		if (!(firstHeartRate > 0) || !(lastHeartRate > 0)) {
			return null;
		}
		double firstEfficiency = Signals.mean(power, first.start(), first.end()) / firstHeartRate;
		double lastEfficiency = Signals.mean(power, last.start(), last.end()) / lastHeartRate;
		if (firstEfficiency == 0) {
			return null;
		}
		return (lastEfficiency - firstEfficiency) / firstEfficiency * 100;
	}

}
