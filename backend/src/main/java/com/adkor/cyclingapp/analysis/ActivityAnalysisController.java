package com.adkor.cyclingapp.analysis;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ActivityAnalysisController {

	private static final int DEFAULT_SUMMARY_DAYS = 90;

	private final ActivityAnalysisService activityAnalysisService;

	/**
	 * Returns the stored analysis. With any of the optional parameters, the activity is analysed
	 * ad hoc with those settings instead and the result is not stored.
	 *
	 * @param targetWatts manual target power of work blocks (default: median power of detected blocks)
	 * @param relativeThreshold forces manual block detection at this fraction of FTP (e.g. 0.9)
	 * @param minDuration minimum block length in manual detection (s)
	 * @param maxGap longest dip bridged inside a block in manual detection (s)
	 */
	@GetMapping("/api/activities/{id}/analysis")
	public ActivityAnalysisResponse getAnalysis(@PathVariable Long id,
			@RequestParam(required = false) @Positive Double targetWatts,
			@RequestParam(required = false) @DecimalMin("0.3") @DecimalMax("3.0") Double relativeThreshold,
			@RequestParam(required = false) @Positive @Max(7200) Integer minDuration,
			@RequestParam(required = false) @PositiveOrZero @Max(600) Integer maxGap) {
		if (targetWatts == null && relativeThreshold == null && minDuration == null && maxGap == null) {
			return activityAnalysisService.getAnalysis(id);
		}
		return activityAnalysisService.analyzeAdHoc(id, targetWatts,
				new DetectionOptions(relativeThreshold, minDuration, maxGap));
	}

	/** Recalculates and stores the analysis with the rider's current thresholds. */
	@PostMapping("/api/activities/{id}/analysis")
	public ActivityAnalysisResponse reanalyze(@PathVariable Long id) {
		return activityAnalysisService.reanalyze(id);
	}

	/**
	 * Recalculates the rider's analyses: by default only missing or outdated ones,
	 * with {@code all=true} every activity (e.g. after the rider's FTP changed).
	 */
	@PostMapping("/api/riders/{riderId}/analyses/refresh")
	public AnalysisRefreshResult refresh(@PathVariable Long riderId,
			@RequestParam(defaultValue = "false") boolean all) {
		return activityAnalysisService.refresh(riderId, all);
	}

	/**
	 * Key metrics of the rider's analysed activities, oldest first. Defaults to the last 90 days.
	 */
	@GetMapping("/api/riders/{riderId}/analyses")
	public List<AnalysisSummary> findSummaries(@PathVariable Long riderId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		LocalDate end = to != null ? to : LocalDate.now();
		LocalDate start = from != null ? from : end.minusDays(DEFAULT_SUMMARY_DAYS);
		return activityAnalysisService.findSummaries(riderId, start, end);
	}

}
