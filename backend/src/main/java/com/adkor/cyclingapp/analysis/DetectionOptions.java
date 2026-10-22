package com.adkor.cyclingapp.analysis;

/**
 * Optional manual settings of work block detection. When {@code relativeThreshold} is set,
 * the automatic search is skipped and blocks are segmented with the given settings.
 *
 * @param relativeThreshold work threshold as a fraction of FTP, e.g. 0.9
 * @param minDurationSeconds minimum block length, defaults to 60 s in manual mode
 * @param maxGapSeconds longest dip bridged inside a block, defaults to 10 s in manual mode
 */
public record DetectionOptions(Double relativeThreshold, Integer minDurationSeconds, Integer maxGapSeconds) {

	public static final DetectionOptions AUTO = new DetectionOptions(null, null, null);

	boolean isManual() {
		return relativeThreshold != null;
	}

}
