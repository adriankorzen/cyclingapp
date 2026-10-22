package com.adkor.cyclingapp.analysis;

import static com.adkor.cyclingapp.analysis.TestSessions.FTP;
import static com.adkor.cyclingapp.analysis.TestSessions.REPEATS;
import static com.adkor.cyclingapp.fit.FitTestFiles.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.adkor.cyclingapp.analysis.ActivityAnalysis.Block;
import com.adkor.cyclingapp.analysis.ActivityAnalysis.Intervals;
import com.adkor.cyclingapp.fit.ActivityRecording;

class ActivityAnalyzerTest {

	private static final AthleteThresholds THRESHOLDS = new AthleteThresholds(FTP, 165, 180, null);

	@Test
	void analysesStructuredSession() {
		double[][] session = TestSessions.intervalSession();

		ActivityAnalysis analysis = ActivityAnalyzer.analyze(recording(session[0], session[1]), THRESHOLDS,
				DetectionOptions.AUTO);

		assertThat(analysis.durationSeconds()).isEqualTo(session[0].length);
		assertThat(analysis.power().trainingStressScore()).isPositive();
		assertThat(analysis.powerZones()).hasSize(7);
		assertThat(analysis.heartRateZones()).hasSize(5);
		assertThat(analysis.averageCadence()).isEqualTo(90.0);

		Intervals intervals = analysis.intervals();
		assertThat(intervals.detected()).isTrue();
		assertThat(intervals.blocks()).hasSize(REPEATS);
		assertThat(intervals.targetSource()).isEqualTo("AUTO");
		assertThat(intervals.targetPower()).isCloseTo(320.0, within(10.0));

		// 92% of 180 bpm = 165.6 bpm, only the last two repeats (166, 168 bpm) reach it
		assertThat(intervals.vo2ThresholdBpm()).isCloseTo(165.6, within(1e-9));
		assertThat(intervals.blocks()).extracting(Block::vo2StimulusSeconds)
				.allMatch(seconds -> seconds != null);
		assertThat(intervals.blocks().get(0).vo2StimulusSeconds()).isZero();
		assertThat(intervals.blocks().get(REPEATS - 1).vo2StimulusSeconds()).isPositive();
		// Same power at a higher HR in the last repeat means efficiency dropped
		assertThat(intervals.cardiacDriftPercent()).isNegative();
	}

	@Test
	void usesManualTargetPower() {
		double[][] session = TestSessions.intervalSession();
		AthleteThresholds thresholds = new AthleteThresholds(FTP, 165, 180, 300.0);

		Intervals intervals = ActivityAnalyzer.analyze(recording(session[0], session[1]), thresholds,
				DetectionOptions.AUTO).intervals();

		assertThat(intervals.targetSource()).isEqualTo("MANUAL");
		assertThat(intervals.targetPower()).isEqualTo(300.0);
		assertThat(intervals.blocks().getFirst().percentOfTarget()).isGreaterThan(100.0);
	}

	@Test
	void reportsUnstructuredRide() {
		double[][] ride = TestSessions.endurancePlusSurges();

		Intervals intervals = ActivityAnalyzer.analyze(recording(ride[0], ride[1]), THRESHOLDS, DetectionOptions.AUTO)
				.intervals();

		assertThat(intervals.detected()).isFalse();
		assertThat(intervals.blocks()).isEmpty();
	}

	@Test
	void analysesHeartRateOnlyRide() {
		double[] noPower = constant(1800, 0);
		double[] heartRate = constant(1800, 140);

		ActivityAnalysis analysis = ActivityAnalyzer.analyze(recording(noPower, heartRate), THRESHOLDS,
				DetectionOptions.AUTO);

		assertThat(analysis.power()).isNull();
		assertThat(analysis.intervals()).isNull();
		assertThat(analysis.powerZones()).isEmpty();
		assertThat(analysis.heartRate().average()).isEqualTo(140.0);
		assertThat(analysis.heartRate().percentOfMax()).isCloseTo(140.0 / 180 * 100, within(1e-9));
		// LTHR 165: Endurance is 113-137 bpm, Tempo 138-155 bpm
		assertThat(analysis.heartRateZones().get(2).seconds()).isEqualTo(1800);
	}

	@Test
	void skipsThresholdBasedSectionsWithoutThresholds() {
		double[][] session = TestSessions.intervalSession();
		AthleteThresholds unknown = new AthleteThresholds(null, null, null, null);

		ActivityAnalysis analysis = ActivityAnalyzer.analyze(recording(session[0], session[1]), unknown,
				DetectionOptions.AUTO);

		assertThat(analysis.power().normalizedPower()).isPositive();
		assertThat(analysis.power().trainingStressScore()).isNull();
		assertThat(analysis.heartRate().percentOfMax()).isNull();
		assertThat(analysis.powerZones()).isEmpty();
		assertThat(analysis.heartRateZones()).isEmpty();
		assertThat(analysis.intervals()).isNull();
	}

	private static ActivityRecording recording(double[] power, double[] heartRate) {
		double[] cadence = new double[power.length];
		for (int i = 0; i < power.length; i++) {
			cadence[i] = power[i] > 0 ? 90 : 0;
		}
		return new ActivityRecording("cycling", "road", power, heartRate, cadence, 40_000.0);
	}

}
