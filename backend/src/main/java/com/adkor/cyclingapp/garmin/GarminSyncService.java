package com.adkor.cyclingapp.garmin;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.adkor.cyclingapp.activity.ActivityData;
import com.adkor.cyclingapp.activity.ActivityService;
import com.adkor.cyclingapp.activity.ActivitySource;
import com.adkor.cyclingapp.rider.RiderNotFoundException;
import com.adkor.cyclingapp.rider.RiderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Imports new cycling activities of a rider from Garmin Connect, including their original FIT files.
 * The sync is idempotent: activities that were already imported are skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarminSyncService {

	static final int PAGE_SIZE = 50;
	private static final String FIT_FORMAT = "FIT";

	private final GarminClient garminClient;
	private final ActivityService activityService;
	private final RiderRepository riderRepository;
	private final GarminProperties properties;

	/**
	 * @param since local date to sync from; when {@code null}, continues from the latest imported activity
	 *              or, for the first sync, goes back {@code initial-sync-days}
	 */
	public GarminSyncResult sync(Long riderId, LocalDate since) {
		if (!riderRepository.existsById(riderId)) {
			throw new RiderNotFoundException(riderId);
		}
		// The latest day is fetched again on purpose, a rider may have more activities that day
		LocalDate startDate = since != null
				? since
				: activityService.findLatestStartDate(riderId, ActivitySource.GARMIN)
						.orElseGet(() -> LocalDate.now().minusDays(properties.initialSyncDays()));

		int fetched = 0;
		int imported = 0;
		List<GarminActivity> page;
		do {
			page = garminClient.searchCyclingActivities(startDate, fetched, PAGE_SIZE);
			for (GarminActivity garminActivity : page) {
				String externalId = String.valueOf(garminActivity.activityId());
				if (!activityService.exists(riderId, ActivitySource.GARMIN, externalId)) {
					byte[] fitFile = garminClient.downloadFitFile(garminActivity.activityId());
					activityService.importActivity(riderId, toActivityData(garminActivity), FIT_FORMAT, fitFile);
					imported++;
				}
			}
			fetched += page.size();
		}
		while (page.size() == PAGE_SIZE);

		log.info("Garmin sync for rider {} since {}: fetched {}, imported {}", riderId, startDate, fetched, imported);
		return new GarminSyncResult(startDate, fetched, imported);
	}

	private static ActivityData toActivityData(GarminActivity activity) {
		return new ActivityData(
				ActivitySource.GARMIN,
				String.valueOf(activity.activityId()),
				activity.activityName(),
				activity.activityType() != null ? activity.activityType().typeKey() : null,
				activity.startTimeGMT().toInstant(ZoneOffset.UTC),
				activity.startTimeLocal(),
				round(activity.duration()),
				round(activity.movingDuration()),
				activity.distance(),
				activity.elevationGain(),
				round(activity.averageHR()),
				round(activity.maxHR()),
				round(activity.avgPower()),
				round(activity.maxPower()),
				round(activity.normPower()),
				round(activity.averageBikingCadenceInRevPerMinute()),
				round(activity.calories()));
	}

	private static Integer round(Double value) {
		return value == null ? null : (int) Math.round(value);
	}

}
