package com.adkor.cyclingapp.fit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class FitFileParserTest {

	private static final double NOT_RECORDED = -1;

	@Test
	void parsesSampleStreamsAndSession() {
		double[] power = { 100, NOT_RECORDED, 300, 250 };
		double[] heartRate = { 120, 125, 130, 135 };

		ActivityRecording recording = FitFileParser.parse(FitTestFiles.cyclingActivity(power, heartRate));

		assertThat(recording.sport()).isEqualTo("cycling");
		assertThat(recording.subSport()).isEqualTo("road");
		assertThat(recording.sampleCount()).isEqualTo(4);
		// Missing power counts as 0 W
		assertThat(recording.power()).containsExactly(100, 0, 300, 250);
		assertThat(recording.heartRate()).containsExactly(120, 125, 130, 135);
		assertThat(recording.cadence()).containsExactly(90, 0, 90, 90);
		assertThat(recording.distanceMeters()).isEqualTo(24.0);
	}

	@Test
	void fillsHeartRateGapsForwardThenBackward() {
		double[] power = FitTestFiles.constant(6, 200);
		double[] heartRate = { NOT_RECORDED, NOT_RECORDED, 130, NOT_RECORDED, 140, NOT_RECORDED };

		ActivityRecording recording = FitFileParser.parse(FitTestFiles.cyclingActivity(power, heartRate));

		assertThat(recording.heartRate()).containsExactly(130, 130, 130, 130, 140, 140);
	}

	@Test
	void reportsMissingHeartRateAsNaN() {
		ActivityRecording recording = FitFileParser.parse(FitTestFiles.cyclingActivity(FitTestFiles.constant(3, 200), null));

		assertThat(Arrays.stream(recording.heartRate()).allMatch(Double::isNaN)).isTrue();
	}

	@Test
	void readsFitFileFromZipArchive() throws IOException {
		byte[] fit = FitTestFiles.cyclingActivity(FitTestFiles.constant(5, 200), null);

		ActivityRecording recording = FitFileParser.parse(zip("123_ACTIVITY.fit", fit));

		assertThat(recording.sampleCount()).isEqualTo(5);
	}

	@Test
	void rejectsInvalidContent() {
		assertThatExceptionOfType(FitFileException.class)
				.isThrownBy(() -> FitFileParser.parse("not a fit file at all".getBytes()));
	}

	@Test
	void rejectsZipWithoutFitFile() throws IOException {
		assertThatExceptionOfType(FitFileException.class)
				.isThrownBy(() -> FitFileParser.parse(zip("readme.txt", new byte[] { 1, 2, 3 })))
				.withMessageContaining("No .fit file");
	}

	private static byte[] zip(String entryName, byte[] content) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry(entryName));
			zip.write(content);
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

}
