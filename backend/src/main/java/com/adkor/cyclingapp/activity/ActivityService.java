package com.adkor.cyclingapp.activity;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adkor.cyclingapp.rider.Rider;
import com.adkor.cyclingapp.rider.RiderNotFoundException;
import com.adkor.cyclingapp.rider.RiderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

	private final ActivityRepository activityRepository;
	private final ActivityFileRepository activityFileRepository;
	private final RiderRepository riderRepository;
	private final ApplicationEventPublisher eventPublisher;

	public List<ActivityResponse> findByRider(Long riderId) {
		requireRider(riderId);
		return activityRepository.findByRiderIdOrderByStartTimeDesc(riderId).stream()
				.map(ActivityResponse::from)
				.toList();
	}

	public ActivityResponse findById(Long id) {
		return activityRepository.findById(id)
				.map(ActivityResponse::from)
				.orElseThrow(() -> new ActivityNotFoundException(id));
	}

	public ActivityFile getFile(Long activityId) {
		return activityFileRepository.findById(activityId)
				.orElseThrow(() -> ActivityNotFoundException.fileOf(activityId));
	}

	/**
	 * Returns the local start date of the rider's most recent activity from the given source.
	 */
	public Optional<LocalDate> findLatestStartDate(Long riderId, ActivitySource source) {
		return activityRepository.findFirstByRiderIdAndSourceOrderByStartTimeDesc(riderId, source)
				.map(activity -> activity.getStartTimeLocal() != null
						? activity.getStartTimeLocal().toLocalDate()
						: LocalDate.ofInstant(activity.getStartTime(), ZoneOffset.UTC));
	}

	public boolean exists(Long riderId, ActivitySource source, String externalId) {
		return activityRepository.existsByRiderIdAndSourceAndExternalId(riderId, source, externalId);
	}

	@Transactional
	public ActivityResponse importActivity(Long riderId, ActivityData data, String fileFormat, byte[] fileContent) {
		Rider rider = requireRider(riderId);
		Activity activity = new Activity(rider, data.source(), data.externalId());
		activity.setName(data.name());
		activity.setSportType(data.sportType());
		activity.setStartTime(data.startTime());
		activity.setStartTimeLocal(data.startTimeLocal());
		activity.setDurationSeconds(data.durationSeconds());
		activity.setMovingDurationSeconds(data.movingDurationSeconds());
		activity.setDistanceMeters(data.distanceMeters());
		activity.setElevationGainMeters(data.elevationGainMeters());
		activity.setAverageHeartRate(data.averageHeartRate());
		activity.setMaxHeartRate(data.maxHeartRate());
		activity.setAveragePower(data.averagePower());
		activity.setMaxPower(data.maxPower());
		activity.setNormalizedPower(data.normalizedPower());
		activity.setAverageCadence(data.averageCadence());
		activity.setCalories(data.calories());
		Activity saved = activityRepository.saveAndFlush(activity);

		if (fileContent != null) {
			activityFileRepository.save(new ActivityFile(saved.getId(), fileFormat, fileContent));
			eventPublisher.publishEvent(new ActivityImportedEvent(saved.getId(), riderId));
		}
		return ActivityResponse.from(saved);
	}

	private Rider requireRider(Long riderId) {
		return riderRepository.findById(riderId)
				.orElseThrow(() -> new RiderNotFoundException(riderId));
	}

}
