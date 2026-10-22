package com.adkor.cyclingapp.analysis;

/**
 * Outcome of recalculating the stored analyses of a rider.
 *
 * @param analyzed activities analysed and stored
 * @param skipped activities whose stored analysis was already up to date
 * @param failed activities that could not be analysed (e.g. missing or corrupted file)
 */
public record AnalysisRefreshResult(int analyzed, int skipped, int failed) {
}
