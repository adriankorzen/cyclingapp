package com.adkor.cyclingapp.analysis;

import java.time.Instant;

/**
 * Analysis of an activity together with the context it was calculated in.
 *
 * @param stored {@code false} for an ad hoc analysis with custom settings that was not persisted
 * @param thresholds rider thresholds the analysis is relative to
 */
public record ActivityAnalysisResponse(
		Long activityId,
		boolean stored,
		Instant analyzedAt,
		int analyzerVersion,
		AthleteThresholds thresholds,
		ActivityAnalysis analysis) {
}
