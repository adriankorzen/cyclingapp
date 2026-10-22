package com.adkor.cyclingapp.fit;

/**
 * Sample streams of a recorded activity, one entry per FIT record message
 * (typically one per second).
 *
 * @param sport sport from the session message, e.g. {@code cycling}
 * @param subSport sub sport from the session message, e.g. {@code road}
 * @param power power in watts, missing samples are 0
 * @param heartRate heart rate in bpm, gaps are forward/backward filled; all {@code NaN} when no HR was recorded
 * @param cadence cadence in rpm, missing samples are 0
 * @param distanceMeters total distance (last recorded value), {@code null} when not recorded
 */
public record ActivityRecording(
		String sport,
		String subSport,
		double[] power,
		double[] heartRate,
		double[] cadence,
		Double distanceMeters) {

	public int sampleCount() {
		return power.length;
	}

}
