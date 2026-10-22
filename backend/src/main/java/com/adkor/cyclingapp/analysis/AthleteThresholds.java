package com.adkor.cyclingapp.analysis;

/**
 * Physiological thresholds the analysis is relative to. Any of them may be unknown ({@code null}).
 *
 * @param ftpWatts functional threshold power
 * @param lthrBpm lactate threshold heart rate
 * @param maxHeartRateBpm maximum heart rate
 * @param targetWatts manual target power of work blocks; {@code null} means the median power of detected blocks
 */
public record AthleteThresholds(Integer ftpWatts, Integer lthrBpm, Integer maxHeartRateBpm, Double targetWatts) {
}
