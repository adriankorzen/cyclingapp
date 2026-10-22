package com.adkor.cyclingapp.analysis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	private final ActivityAnalysisService activityAnalysisService;

	/**
	 * @param targetWatts manual target power of work blocks (default: median power of detected blocks)
	 * @param relativeThreshold forces manual block detection at this fraction of FTP (e.g. 0.9)
	 * @param minDuration minimum block length in manual detection (s)
	 * @param maxGap longest dip bridged inside a block in manual detection (s)
	 */
	@GetMapping("/api/activities/{id}/analysis")
	public ActivityAnalysis analyze(@PathVariable Long id,
			@RequestParam(required = false) @Positive Double targetWatts,
			@RequestParam(required = false) @DecimalMin("0.3") @DecimalMax("3.0") Double relativeThreshold,
			@RequestParam(required = false) @Positive @Max(7200) Integer minDuration,
			@RequestParam(required = false) @PositiveOrZero @Max(600) Integer maxGap) {
		return activityAnalysisService.analyze(id, targetWatts,
				new DetectionOptions(relativeThreshold, minDuration, maxGap));
	}

}
