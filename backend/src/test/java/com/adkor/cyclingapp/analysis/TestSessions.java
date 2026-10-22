package com.adkor.cyclingapp.analysis;

import java.util.Arrays;

/**
 * Synthetic 1 Hz sessions for analysis tests.
 */
final class TestSessions {

	static final int FTP = 280;
	static final int WARM_UP = 600;
	static final int WORK = 180;
	static final int RECOVERY = 120;
	static final int REPEATS = 5;

	private TestSessions() {
	}

	/** 10 min warm-up, 5 x 3 min at 320 W with 2 min recoveries, 10 min cool-down. */
	static double[][] intervalSession() {
		int length = WARM_UP + REPEATS * (WORK + RECOVERY) + WARM_UP;
		double[] power = new double[length];
		double[] heartRate = new double[length];
		Arrays.fill(power, 150);
		Arrays.fill(heartRate, 120);
		for (int r = 0; r < REPEATS; r++) {
			int start = WARM_UP + r * (WORK + RECOVERY);
			for (int i = start; i < start + WORK; i++) {
				power[i] = 320;
				// HR keeps rising across the repeats
				heartRate[i] = 160 + 2 * r;
			}
			for (int i = start + WORK; i < start + WORK + RECOVERY; i++) {
				power[i] = 140;
			}
		}
		return new double[][] { power, heartRate };
	}

	/** 1 h endurance ride at 200 W with irregular surges, HR not affected by them. */
	static double[][] endurancePlusSurges() {
		double[] power = new double[3600];
		double[] heartRate = new double[3600];
		Arrays.fill(power, 200);
		Arrays.fill(heartRate, 135);
		int[][] surges = { { 300, 20, 420 }, { 900, 45, 330 }, { 1400, 8, 520 }, { 2100, 70, 300 }, { 2600, 15, 460 },
				{ 3100, 30, 360 } };
		for (int[] surge : surges) {
			Arrays.fill(power, surge[0], surge[0] + surge[1], surge[2]);
		}
		return new double[][] { power, heartRate };
	}

}
