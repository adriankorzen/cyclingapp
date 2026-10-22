package com.adkor.cyclingapp.fit;

import java.util.Arrays;

import com.garmin.fit.BufferEncoder;
import com.garmin.fit.DateTime;
import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;

/**
 * Builds real FIT files for tests. A {@code NaN} or negative sample means the field is not recorded.
 */
public final class FitTestFiles {

	private static final long START_TIMESTAMP = 1_100_000_000L;

	private FitTestFiles() {
	}

	public static byte[] cyclingActivity(double[] power, double[] heartRate) {
		BufferEncoder encoder = new BufferEncoder();

		FileIdMesg fileId = new FileIdMesg();
		fileId.setType(File.ACTIVITY);
		fileId.setManufacturer(Manufacturer.DEVELOPMENT);
		fileId.setTimeCreated(new DateTime(START_TIMESTAMP));
		encoder.write(fileId);

		for (int i = 0; i < power.length; i++) {
			RecordMesg record = new RecordMesg();
			record.setTimestamp(new DateTime(START_TIMESTAMP + i));
			if (power[i] >= 0) {
				record.setPower((int) power[i]);
				record.setCadence((short) (power[i] > 0 ? 90 : 0));
			}
			if (heartRate != null && heartRate[i] >= 0) {
				record.setHeartRate((short) heartRate[i]);
			}
			record.setDistance(i * 8.0f);
			encoder.write(record);
		}

		SessionMesg session = new SessionMesg();
		session.setTimestamp(new DateTime(START_TIMESTAMP + power.length));
		session.setSport(Sport.CYCLING);
		session.setSubSport(SubSport.ROAD);
		encoder.write(session);

		return encoder.close();
	}

	public static double[] constant(int length, double value) {
		double[] values = new double[length];
		Arrays.fill(values, value);
		return values;
	}

}
