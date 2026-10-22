package com.adkor.cyclingapp.analysis;

import static com.adkor.cyclingapp.analysis.TestSessions.FTP;
import static com.adkor.cyclingapp.analysis.TestSessions.RECOVERY;
import static com.adkor.cyclingapp.analysis.TestSessions.REPEATS;
import static com.adkor.cyclingapp.analysis.TestSessions.WARM_UP;
import static com.adkor.cyclingapp.analysis.TestSessions.WORK;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.adkor.cyclingapp.analysis.WorkBlockDetector.Detection;
import com.adkor.cyclingapp.analysis.WorkBlockDetector.Mode;

class WorkBlockDetectorTest {

	@Test
	void segmentBridgesShortGapsAndDropsShortBlocks() {
		boolean[] mask = mask("..####.###......##.....######..");

		assertThat(WorkBlockDetector.segment(mask, 3, 2))
				.containsExactly(new WorkBlock(2, 10), new WorkBlock(23, 29));
		assertThat(WorkBlockDetector.segment(mask, 1, 0))
				.containsExactly(new WorkBlock(2, 6), new WorkBlock(7, 10), new WorkBlock(16, 18), new WorkBlock(23, 29));
		assertThat(WorkBlockDetector.segment(new boolean[0], 1, 1)).isEmpty();
	}

	@Test
	void detectsRepeatsOfStructuredSession() {
		double[][] session = TestSessions.intervalSession();

		Detection detection = WorkBlockDetector.detect(session[0], FTP, session[1], DetectionOptions.AUTO);

		assertThat(detection.mode()).isEqualTo(Mode.AUTO);
		assertThat(detection.blocks()).hasSize(REPEATS);
		for (int r = 0; r < REPEATS; r++) {
			WorkBlock block = detection.blocks().get(r);
			int expectedStart = WARM_UP + r * (WORK + RECOVERY);
			assertThat(block.start()).isBetween(expectedStart - 5, expectedStart + 5);
			assertThat(block.duration()).isBetween(WORK - 5, WORK + 5);
		}
		assertThat(detection.durationConsistency()).isLessThan(0.05);
		assertThat(detection.powerConsistency()).isLessThan(0.05);
	}

	@Test
	void ignoresIrregularSurgesOfEnduranceRide() {
		double[][] ride = TestSessions.endurancePlusSurges();

		Detection detection = WorkBlockDetector.detect(ride[0], FTP, ride[1], DetectionOptions.AUTO);

		assertThat(detection.blocks()).isEmpty();
	}

	@Test
	void usesManualSettingsWhenThresholdIsGiven() {
		double[][] session = TestSessions.intervalSession();

		Detection detection = WorkBlockDetector.detect(session[0], FTP, session[1], new DetectionOptions(1.1, null, null));

		assertThat(detection.mode()).isEqualTo(Mode.MANUAL);
		assertThat(detection.relativeThreshold()).isEqualTo(1.1);
		assertThat(detection.minDurationSeconds()).isEqualTo(60);
		assertThat(detection.maxGapSeconds()).isEqualTo(10);
		assertThat(detection.blocks()).hasSize(REPEATS);
	}

	@Test
	void returnsNothingWithoutFtpOrData() {
		assertThat(WorkBlockDetector.detect(new double[0], FTP, null, DetectionOptions.AUTO).blocks()).isEmpty();
		assertThat(WorkBlockDetector.detect(new double[] { 300, 300 }, 0, null, DetectionOptions.AUTO).blocks()).isEmpty();
	}

	private static boolean[] mask(String pattern) {
		boolean[] mask = new boolean[pattern.length()];
		for (int i = 0; i < pattern.length(); i++) {
			mask[i] = pattern.charAt(i) == '#';
		}
		return mask;
	}

}
