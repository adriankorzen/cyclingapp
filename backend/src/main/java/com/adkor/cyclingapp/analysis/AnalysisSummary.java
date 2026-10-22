package com.adkor.cyclingapp.analysis;

import java.time.Instant;
import java.time.LocalDateTime;

import com.adkor.cyclingapp.activity.Activity;

/**
 * Key metrics of an analysed activity, e.g. for training logs and training load over time.
 */
public record AnalysisSummary(
		Long activityId,
		String name,
		String sportType,
		Instant startTime,
		LocalDateTime startTimeLocal,
		Instant analyzedAt,
		Integer ftpWatts,
		int durationSeconds,
		Double averagePower,
		Double normalizedPower,
		Double variabilityIndex,
		Double intensityFactor,
		Double trainingStressScore,
		Double workKilojoules,
		Double averageHeartRate,
		Double maxHeartRate,
		int intervalCount) {

	static AnalysisSummary from(StoredAnalysis analysis) {
		Activity activity = analysis.getActivity();
		return new AnalysisSummary(
				analysis.getActivityId(),
				activity.getName(),
				activity.getSportType(),
				activity.getStartTime(),
				activity.getStartTimeLocal(),
				analysis.getAnalyzedAt(),
				analysis.getFtpWatts(),
				analysis.getDurationSeconds(),
				analysis.getAveragePower(),
				analysis.getNormalizedPower(),
				analysis.getVariabilityIndex(),
				analysis.getIntensityFactor(),
				analysis.getTrainingStressScore(),
				analysis.getWorkKilojoules(),
				analysis.getAverageHeartRate(),
				analysis.getMaxHeartRate(),
				analysis.getIntervalCount());
	}

}
