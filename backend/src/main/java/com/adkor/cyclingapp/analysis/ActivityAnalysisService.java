package com.adkor.cyclingapp.analysis;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adkor.cyclingapp.activity.Activity;
import com.adkor.cyclingapp.activity.ActivityFile;
import com.adkor.cyclingapp.activity.ActivityNotFoundException;
import com.adkor.cyclingapp.activity.ActivityRepository;
import com.adkor.cyclingapp.activity.ActivityService;
import com.adkor.cyclingapp.fit.FitFileParser;
import com.adkor.cyclingapp.rider.Rider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityAnalysisService {

	private final ActivityRepository activityRepository;
	private final ActivityService activityService;

	/**
	 * Analyses the stored FIT file of an activity relative to the current thresholds of its rider.
	 */
	public ActivityAnalysis analyze(Long activityId, Double targetWatts, DetectionOptions options) {
		Activity activity = activityRepository.findById(activityId)
				.orElseThrow(() -> new ActivityNotFoundException(activityId));
		ActivityFile file = activityService.getFile(activityId);
		Rider rider = activity.getRider();
		AthleteThresholds thresholds = new AthleteThresholds(
				rider.getFtpWatts(), rider.getLthrBpm(), rider.getMaxHeartRateBpm(), targetWatts);
		return ActivityAnalyzer.analyze(FitFileParser.parse(file.getContent()), thresholds, options);
	}

}
