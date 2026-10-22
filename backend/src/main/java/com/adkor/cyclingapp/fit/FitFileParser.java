package com.adkor.cyclingapp.fit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;

/**
 * Parses FIT files (plain or wrapped in a ZIP archive) into sample streams.
 */
public final class FitFileParser {

	private static final byte[] ZIP_SIGNATURE = { 'P', 'K', 3, 4 };

	private FitFileParser() {
	}

	public static ActivityRecording parse(byte[] content) {
		byte[] fit = looksLikeZip(content) ? extractFitFromZip(content) : content;

		List<RecordMesg> records = new ArrayList<>();
		SessionMesg[] session = new SessionMesg[1];

		Decode decode = new Decode();
		MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
		broadcaster.addListener((RecordMesgListener) records::add);
		broadcaster.addListener((SessionMesgListener) mesg -> {
			if (session[0] == null) {
				session[0] = mesg;
			}
		});
		try {
			decode.read(new ByteArrayInputStream(fit), broadcaster, broadcaster);
		}
		catch (FitRuntimeException ex) {
			throw new FitFileException("Invalid FIT file: " + ex.getMessage(), ex);
		}

		return toRecording(records, session[0]);
	}

	private static ActivityRecording toRecording(List<RecordMesg> records, SessionMesg session) {
		int n = records.size();
		double[] power = new double[n];
		double[] heartRate = new double[n];
		double[] cadence = new double[n];
		Double distance = null;

		for (int i = 0; i < n; i++) {
			RecordMesg record = records.get(i);
			power[i] = record.getPower() != null ? record.getPower() : 0.0;
			heartRate[i] = record.getHeartRate() != null ? record.getHeartRate() : Double.NaN;
			cadence[i] = record.getCadence() != null ? record.getCadence() : 0.0;
			if (record.getDistance() != null) {
				distance = record.getDistance().doubleValue();
			}
		}
		fillGaps(heartRate);

		String sport = null;
		String subSport = null;
		if (session != null) {
			sport = session.getSport() != null ? session.getSport().name().toLowerCase(Locale.ROOT) : null;
			subSport = session.getSubSport() != null ? session.getSubSport().name().toLowerCase(Locale.ROOT) : null;
		}
		return new ActivityRecording(sport, subSport, power, heartRate, cadence, distance);
	}

	/** Forward fills missing values, then backward fills the leading gap. */
	private static void fillGaps(double[] values) {
		double last = Double.NaN;
		for (int i = 0; i < values.length; i++) {
			if (Double.isNaN(values[i])) {
				values[i] = last;
			}
			else {
				last = values[i];
			}
		}
		int firstValid = 0;
		while (firstValid < values.length && Double.isNaN(values[firstValid])) {
			firstValid++;
		}
		if (firstValid < values.length) {
			Arrays.fill(values, 0, firstValid, values[firstValid]);
		}
	}

	private static boolean looksLikeZip(byte[] content) {
		return content.length >= ZIP_SIGNATURE.length
				&& Arrays.equals(content, 0, ZIP_SIGNATURE.length, ZIP_SIGNATURE, 0, ZIP_SIGNATURE.length);
	}

	/** Returns the largest FIT file in the archive. */
	private static byte[] extractFitFromZip(byte[] archive) {
		byte[] largest = null;
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".fit")) {
					byte[] candidate = zip.readAllBytes();
					if (largest == null || candidate.length > largest.length) {
						largest = candidate;
					}
				}
			}
		}
		catch (IOException ex) {
			throw new FitFileException("Cannot read ZIP archive", ex);
		}
		if (largest == null) {
			throw new FitFileException("No .fit file found in the ZIP archive");
		}
		return largest;
	}

}
