package com.adkor.cyclingapp.analysis;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.adkor.cyclingapp.activity.Activity;
import com.adkor.cyclingapp.activity.ActivityFile;
import com.adkor.cyclingapp.activity.ActivityImportedEvent;
import com.adkor.cyclingapp.activity.ActivityNotFoundException;
import com.adkor.cyclingapp.activity.ActivityRepository;
import com.adkor.cyclingapp.activity.ActivityService;
import com.adkor.cyclingapp.fit.FitFileParser;
import com.adkor.cyclingapp.rider.RiderNotFoundException;
import com.adkor.cyclingapp.rider.RiderRepository;
import com.adkor.cyclingapp.rider.RiderThreshold;
import com.adkor.cyclingapp.rider.RiderThresholdService;
import com.adkor.cyclingapp.rider.ThresholdTimeline;
import com.adkor.cyclingapp.rider.ThresholdsChangedEvent;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Analyses activities and stores the results. Every activity is analysed relative to the rider's
 * thresholds valid on the day of the activity, so a new FTP never changes the TSS of earlier rides.
 */
@Slf4j
@Service
public class ActivityAnalysisService {

	private final ActivityRepository activityRepository;
	private final ActivityService activityService;
	private final StoredAnalysisRepository storedAnalysisRepository;
	private final RiderRepository riderRepository;
	private final RiderThresholdService thresholdService;
	private final JsonMapper jsonMapper;
	/** Every activity is analysed in its own transaction, so one failure does not affect the others. */
	private final TransactionTemplate newTransaction;

	public ActivityAnalysisService(ActivityRepository activityRepository, ActivityService activityService,
			StoredAnalysisRepository storedAnalysisRepository, RiderRepository riderRepository,
			RiderThresholdService thresholdService, JsonMapper jsonMapper,
			PlatformTransactionManager transactionManager) {
		this.activityRepository = activityRepository;
		this.activityService = activityService;
		this.storedAnalysisRepository = storedAnalysisRepository;
		this.riderRepository = riderRepository;
		this.thresholdService = thresholdService;
		this.jsonMapper = jsonMapper;
		this.newTransaction = new TransactionTemplate(transactionManager);
		this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Returns the stored analysis of an activity; analyses it first when it has none yet
	 * or it was produced by an older version of the analyzer.
	 */
	@Transactional
	public ActivityAnalysisResponse getAnalysis(Long activityId) {
		StoredAnalysis stored = storedAnalysisRepository.findById(activityId)
				.filter(analysis -> analysis.getAnalyzerVersion() == ActivityAnalyzer.VERSION)
				.orElseGet(() -> analyzeAndStore(activityId));
		return toResponse(stored);
	}

	/**
	 * Analyses an activity with custom settings without storing the result.
	 */
	@Transactional(readOnly = true)
	public ActivityAnalysisResponse analyzeAdHoc(Long activityId, Double targetWatts, DetectionOptions options) {
		Activity activity = getActivity(activityId);
		AthleteThresholds thresholds = thresholdsOn(activity, thresholdService.timeline(riderIdOf(activity)),
				targetWatts);
		ActivityAnalysis analysis = analyze(activityId, thresholds, options);
		return new ActivityAnalysisResponse(activityId, false, Instant.now(), ActivityAnalyzer.VERSION, thresholds,
				analysis);
	}

	/**
	 * Recalculates and stores the analysis of an activity.
	 */
	@Transactional
	public ActivityAnalysisResponse reanalyze(Long activityId) {
		return toResponse(analyzeAndStore(activityId));
	}

	/**
	 * Brings the rider's stored analyses up to date: analyses activities that have no analysis, an outdated
	 * one (older analyzer version) or one calculated with thresholds that are no longer valid for their day.
	 *
	 * @param all {@code true} recalculates every activity regardless
	 */
	public AnalysisRefreshResult refresh(Long riderId, boolean all) {
		if (!riderRepository.existsById(riderId)) {
			throw new RiderNotFoundException(riderId);
		}
		ThresholdTimeline timeline = thresholdService.timeline(riderId);
		List<Activity> activities = activityRepository.findByRiderIdOrderByStartTimeDesc(riderId);
		Map<Long, StoredAnalysis> stored = storedAnalysisRepository
				.findAllById(activities.stream().map(Activity::getId).toList()).stream()
				.collect(Collectors.toMap(StoredAnalysis::getActivityId, Function.identity()));

		int analyzed = 0;
		int skipped = 0;
		int failed = 0;
		for (Activity activity : activities) {
			if (!all && isUpToDate(stored.get(activity.getId()), thresholdsOn(activity, timeline, null))) {
				skipped++;
				continue;
			}
			try {
				newTransaction.executeWithoutResult(status -> analyzeAndStore(activity.getId()));
				analyzed++;
			}
			catch (RuntimeException ex) {
				log.warn("Analysis of activity {} failed: {}", activity.getId(), ex.getMessage());
				failed++;
			}
		}
		log.info("Analysis refresh for rider {}: analyzed {}, skipped {}, failed {}", riderId, analyzed, skipped, failed);
		return new AnalysisRefreshResult(analyzed, skipped, failed);
	}

	/**
	 * Key metrics of a rider's analysed activities that started within {@code [from, to]} (local dates).
	 */
	@Transactional(readOnly = true)
	public List<AnalysisSummary> findSummaries(Long riderId, LocalDate from, LocalDate to) {
		if (!riderRepository.existsById(riderId)) {
			throw new RiderNotFoundException(riderId);
		}
		return storedAnalysisRepository
				.findByRiderAndPeriod(riderId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
				.map(AnalysisSummary::from)
				.toList();
	}

	/**
	 * Analyses a newly imported activity once the import is committed. A failing analysis must not
	 * break the import, the activity can be analysed again later.
	 */
	@TransactionalEventListener(fallbackExecution = true)
	public void onActivityImported(ActivityImportedEvent event) {
		try {
			newTransaction.executeWithoutResult(status -> analyzeAndStore(event.activityId()));
		}
		catch (RuntimeException ex) {
			log.warn("Analysis of imported activity {} failed: {}", event.activityId(), ex.getMessage());
		}
	}

	/**
	 * Recalculates only the activities whose valid thresholds changed, e.g. a new FTP test affects
	 * rides from its date until the next entry, earlier rides keep their results.
	 */
	@TransactionalEventListener(fallbackExecution = true)
	public void onThresholdsChanged(ThresholdsChangedEvent event) {
		refresh(event.riderId(), false);
	}

	private static boolean isUpToDate(StoredAnalysis stored, AthleteThresholds thresholds) {
		return stored != null
				&& stored.getAnalyzerVersion() == ActivityAnalyzer.VERSION
				&& Objects.equals(stored.getFtpWatts(), thresholds.ftpWatts())
				&& Objects.equals(stored.getLthrBpm(), thresholds.lthrBpm())
				&& Objects.equals(stored.getMaxHeartRateBpm(), thresholds.maxHeartRateBpm());
	}

	private StoredAnalysis analyzeAndStore(Long activityId) {
		Activity activity = getActivity(activityId);
		AthleteThresholds thresholds = thresholdsOn(activity, thresholdService.timeline(riderIdOf(activity)), null);
		ActivityAnalysis analysis = analyze(activityId, thresholds, DetectionOptions.AUTO);

		StoredAnalysis stored = storedAnalysisRepository.findById(activityId)
				.orElseGet(() -> new StoredAnalysis(activity));
		stored.setAnalyzerVersion(ActivityAnalyzer.VERSION);
		stored.setAnalyzedAt(Instant.now());
		stored.setFtpWatts(thresholds.ftpWatts());
		stored.setLthrBpm(thresholds.lthrBpm());
		stored.setMaxHeartRateBpm(thresholds.maxHeartRateBpm());
		stored.setDurationSeconds(analysis.durationSeconds());

		PowerMetrics power = analysis.power();
		stored.setAveragePower(power != null ? power.averagePower() : null);
		stored.setNormalizedPower(power != null ? power.normalizedPower() : null);
		stored.setVariabilityIndex(power != null ? power.variabilityIndex() : null);
		stored.setIntensityFactor(power != null ? power.intensityFactor() : null);
		stored.setTrainingStressScore(power != null ? power.trainingStressScore() : null);
		stored.setWorkKilojoules(power != null ? power.workKilojoules() : null);
		stored.setAverageHeartRate(analysis.heartRate() != null ? analysis.heartRate().average() : null);
		stored.setMaxHeartRate(analysis.heartRate() != null ? analysis.heartRate().max() : null);
		stored.setIntervalCount(analysis.intervals() != null ? analysis.intervals().blocks().size() : 0);
		stored.setResult(jsonMapper.writeValueAsString(analysis));
		return storedAnalysisRepository.save(stored);
	}

	private ActivityAnalysis analyze(Long activityId, AthleteThresholds thresholds, DetectionOptions options) {
		ActivityFile file = activityService.getFile(activityId);
		return ActivityAnalyzer.analyze(FitFileParser.parse(file.getContent()), thresholds, options);
	}

	private ActivityAnalysisResponse toResponse(StoredAnalysis stored) {
		AthleteThresholds thresholds = new AthleteThresholds(stored.getFtpWatts(), stored.getLthrBpm(),
				stored.getMaxHeartRateBpm(), null);
		return new ActivityAnalysisResponse(stored.getActivityId(), true, stored.getAnalyzedAt(),
				stored.getAnalyzerVersion(), thresholds,
				jsonMapper.readValue(stored.getResult(), ActivityAnalysis.class));
	}

	private Activity getActivity(Long activityId) {
		return activityRepository.findById(activityId)
				.orElseThrow(() -> new ActivityNotFoundException(activityId));
	}

	/** Thresholds valid on the (local) day the activity started. */
	private static AthleteThresholds thresholdsOn(Activity activity, ThresholdTimeline timeline, Double targetWatts) {
		LocalDate day = activity.getStartTimeLocal() != null
				? activity.getStartTimeLocal().toLocalDate()
				: LocalDate.ofInstant(activity.getStartTime(), ZoneOffset.UTC);
		RiderThreshold threshold = timeline.at(day).orElse(null);
		if (threshold == null) {
			return new AthleteThresholds(null, null, null, targetWatts);
		}
		return new AthleteThresholds(threshold.getFtpWatts(), threshold.getLthrBpm(), threshold.getMaxHeartRateBpm(),
				targetWatts);
	}

	private static Long riderIdOf(Activity activity) {
		return activity.getRider().getId();
	}

}
