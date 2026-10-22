package com.adkor.cyclingapp.analysis;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import com.adkor.cyclingapp.rider.Rider;
import com.adkor.cyclingapp.rider.RiderNotFoundException;
import com.adkor.cyclingapp.rider.RiderRepository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Analyses activities and stores the results. New activities are analysed right after import,
 * relative to the rider's thresholds at that moment.
 */
@Slf4j
@Service
public class ActivityAnalysisService {

	private final ActivityRepository activityRepository;
	private final ActivityService activityService;
	private final StoredAnalysisRepository storedAnalysisRepository;
	private final RiderRepository riderRepository;
	private final JsonMapper jsonMapper;
	/** Every activity is analysed in its own transaction, so one failure does not affect the others. */
	private final TransactionTemplate newTransaction;

	public ActivityAnalysisService(ActivityRepository activityRepository, ActivityService activityService,
			StoredAnalysisRepository storedAnalysisRepository, RiderRepository riderRepository, JsonMapper jsonMapper,
			PlatformTransactionManager transactionManager) {
		this.activityRepository = activityRepository;
		this.activityService = activityService;
		this.storedAnalysisRepository = storedAnalysisRepository;
		this.riderRepository = riderRepository;
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
		AthleteThresholds thresholds = thresholdsOf(activity.getRider(), targetWatts);
		ActivityAnalysis analysis = analyze(activityId, thresholds, options);
		return new ActivityAnalysisResponse(activityId, false, Instant.now(), ActivityAnalyzer.VERSION, thresholds,
				analysis);
	}

	/**
	 * Recalculates and stores the analysis of an activity with the rider's current thresholds.
	 */
	@Transactional
	public ActivityAnalysisResponse reanalyze(Long activityId) {
		return toResponse(analyzeAndStore(activityId));
	}

	/**
	 * Recalculates stored analyses of a rider's activities.
	 *
	 * @param all {@code true} recalculates every activity (e.g. after FTP changed), {@code false} only
	 *            activities without an analysis or with an outdated one
	 */
	public AnalysisRefreshResult refresh(Long riderId, boolean all) {
		if (!riderRepository.existsById(riderId)) {
			throw new RiderNotFoundException(riderId);
		}
		List<Long> activityIds = activityRepository.findByRiderIdOrderByStartTimeDesc(riderId).stream()
				.map(Activity::getId)
				.toList();
		Map<Long, Integer> storedVersions = storedAnalysisRepository.findAllById(activityIds).stream()
				.collect(Collectors.toMap(StoredAnalysis::getActivityId, StoredAnalysis::getAnalyzerVersion));

		int analyzed = 0;
		int skipped = 0;
		int failed = 0;
		for (Long activityId : activityIds) {
			if (!all && Integer.valueOf(ActivityAnalyzer.VERSION).equals(storedVersions.get(activityId))) {
				skipped++;
				continue;
			}
			try {
				newTransaction.executeWithoutResult(status -> analyzeAndStore(activityId));
				analyzed++;
			}
			catch (RuntimeException ex) {
				log.warn("Analysis of activity {} failed: {}", activityId, ex.getMessage());
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

	private StoredAnalysis analyzeAndStore(Long activityId) {
		Activity activity = getActivity(activityId);
		AthleteThresholds thresholds = thresholdsOf(activity.getRider(), null);
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

	private static AthleteThresholds thresholdsOf(Rider rider, Double targetWatts) {
		return new AthleteThresholds(rider.getFtpWatts(), rider.getLthrBpm(), rider.getMaxHeartRateBpm(), targetWatts);
	}

}
