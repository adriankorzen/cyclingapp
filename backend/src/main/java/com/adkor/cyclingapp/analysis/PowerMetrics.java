package com.adkor.cyclingapp.analysis;

/**
 * Coggan power metrics of a whole activity.
 *
 * @param averagePower average power including zeros (W)
 * @param maxPower max power (W)
 * @param normalizedPower Normalized Power, 4th-power mean of the 30 s rolling average (W)
 * @param variabilityIndex NP / average power
 * @param intensityFactor NP / FTP, {@code null} without FTP
 * @param trainingStressScore TSS, {@code null} without FTP
 * @param averagePowerPercentFtp average power as % of FTP, {@code null} without FTP
 * @param workKilojoules mechanical work (kJ)
 */
public record PowerMetrics(
		double averagePower,
		double maxPower,
		double normalizedPower,
		Double variabilityIndex,
		Double intensityFactor,
		Double trainingStressScore,
		Double averagePowerPercentFtp,
		double workKilojoules) {

	private static final int NP_WINDOW_SECONDS = 30;

	/**
	 * @param power one sample per second
	 * @param ftp functional threshold power, may be {@code null}
	 */
	static PowerMetrics calculate(double[] power, Integer ftp) {
		int n = power.length;
		double[] rolling = Signals.trailingRollingMean(power, NP_WINDOW_SECONDS);
		double fourthPowerSum = 0;
		for (double value : rolling) {
			fourthPowerSum += Math.pow(value, 4);
		}
		double normalizedPower = Math.pow(fourthPowerSum / n, 0.25);
		double averagePower = Signals.mean(power);

		Double intensityFactor = null;
		Double trainingStressScore = null;
		Double averagePowerPercentFtp = null;
		if (ftp != null && ftp > 0) {
			intensityFactor = normalizedPower / ftp;
			trainingStressScore = (n * normalizedPower * intensityFactor) / (ftp * 3600.0) * 100;
			averagePowerPercentFtp = averagePower / ftp * 100;
		}
		return new PowerMetrics(
				averagePower,
				Signals.max(power, 0, n),
				normalizedPower,
				averagePower > 0 ? normalizedPower / averagePower : null,
				intensityFactor,
				trainingStressScore,
				averagePowerPercentFtp,
				Signals.sum(power) / 1000.0);
	}

}
