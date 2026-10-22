package com.adkor.cyclingapp.rider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Manages the rider's threshold history. Every change publishes {@link ThresholdsChangedEvent},
 * so analyses of the affected activities can be recalculated.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiderThresholdService {

	private final RiderThresholdRepository thresholdRepository;
	private final RiderRepository riderRepository;
	private final ApplicationEventPublisher eventPublisher;

	public ThresholdTimeline timeline(Long riderId) {
		return new ThresholdTimeline(thresholdRepository.findByRiderIdOrderByEffectiveFromAsc(riderId));
	}

	public Optional<RiderThreshold> current(Long riderId) {
		return timeline(riderId).at(LocalDate.now());
	}

	/** History of the rider's thresholds, newest first. */
	public List<ThresholdResponse> findAll(Long riderId) {
		requireRider(riderId);
		return thresholdRepository.findByRiderIdOrderByEffectiveFromAsc(riderId).reversed().stream()
				.map(ThresholdResponse::from)
				.toList();
	}

	/**
	 * Adds an entry. Values missing in the request are carried over from the thresholds valid
	 * on {@code effectiveFrom} so far.
	 */
	@Transactional
	public ThresholdResponse create(Long riderId, ThresholdRequest request) {
		Rider rider = requireRider(riderId);
		LocalDate effectiveFrom = request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now();
		if (thresholdRepository.existsByRiderIdAndEffectiveFrom(riderId, effectiveFrom)) {
			throw new ThresholdConflictException(effectiveFrom);
		}
		Optional<RiderThreshold> previous = timeline(riderId).at(effectiveFrom);

		RiderThreshold threshold = new RiderThreshold(rider, effectiveFrom);
		threshold.setFtpWatts(valueOrPrevious(request.ftpWatts(), previous.map(RiderThreshold::getFtpWatts)));
		threshold.setLthrBpm(valueOrPrevious(request.lthrBpm(), previous.map(RiderThreshold::getLthrBpm)));
		threshold.setMaxHeartRateBpm(
				valueOrPrevious(request.maxHeartRateBpm(), previous.map(RiderThreshold::getMaxHeartRateBpm)));
		RiderThreshold saved = thresholdRepository.saveAndFlush(threshold);

		eventPublisher.publishEvent(new ThresholdsChangedEvent(riderId));
		return ThresholdResponse.from(saved);
	}

	/** Replaces an entry; unlike {@link #create}, omitted values become empty. */
	@Transactional
	public ThresholdResponse update(Long riderId, Long thresholdId, ThresholdRequest request) {
		RiderThreshold threshold = requireThreshold(riderId, thresholdId);
		LocalDate effectiveFrom = request.effectiveFrom() != null ? request.effectiveFrom() : threshold.getEffectiveFrom();
		if (!effectiveFrom.equals(threshold.getEffectiveFrom())
				&& thresholdRepository.existsByRiderIdAndEffectiveFrom(riderId, effectiveFrom)) {
			throw new ThresholdConflictException(effectiveFrom);
		}
		threshold.setEffectiveFrom(effectiveFrom);
		threshold.setFtpWatts(request.ftpWatts());
		threshold.setLthrBpm(request.lthrBpm());
		threshold.setMaxHeartRateBpm(request.maxHeartRateBpm());
		RiderThreshold saved = thresholdRepository.saveAndFlush(threshold);

		eventPublisher.publishEvent(new ThresholdsChangedEvent(riderId));
		return ThresholdResponse.from(saved);
	}

	@Transactional
	public void delete(Long riderId, Long thresholdId) {
		thresholdRepository.delete(requireThreshold(riderId, thresholdId));
		eventPublisher.publishEvent(new ThresholdsChangedEvent(riderId));
	}

	private RiderThreshold requireThreshold(Long riderId, Long thresholdId) {
		requireRider(riderId);
		return thresholdRepository.findByIdAndRiderId(thresholdId, riderId)
				.orElseThrow(() -> new ThresholdNotFoundException(thresholdId));
	}

	private Rider requireRider(Long riderId) {
		return riderRepository.findById(riderId)
				.orElseThrow(() -> new RiderNotFoundException(riderId));
	}

	private static Integer valueOrPrevious(Integer value, Optional<Integer> previous) {
		return value != null ? value : previous.orElse(null);
	}

}
