package com.adkor.cyclingapp.analysis;

/**
 * A detected block of work: samples {@code [start, end)}, i.e. seconds from the activity start.
 */
record WorkBlock(int start, int end) {

	int duration() {
		return end - start;
	}

}
