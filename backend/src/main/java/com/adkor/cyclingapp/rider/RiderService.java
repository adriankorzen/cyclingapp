package com.adkor.cyclingapp.rider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adkor.cyclingapp.zone.CogganZones;
import com.adkor.cyclingapp.zone.TrainingZone;
import com.adkor.cyclingapp.zone.ZonesResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiderService {

	private final RiderRepository riderRepository;
	private final RiderThresholdService thresholdService;

	public List<RiderResponse> findAll() {
		return riderRepository.findAll(Sort.by("id")).stream()
				.map(this::toResponse)
				.toList();
	}

	public RiderResponse findById(Long id) {
		return toResponse(getRider(id));
	}

	@Transactional
	public RiderResponse create(RiderRequest request) {
		Rider rider = new Rider(request.name(), request.weightKg());
		applyRequest(rider, request);
		return toResponse(riderRepository.saveAndFlush(rider));
	}

	@Transactional
	public RiderResponse update(Long id, RiderRequest request) {
		Rider rider = getRider(id);
		applyRequest(rider, request);
		return toResponse(riderRepository.saveAndFlush(rider));
	}

	@Transactional
	public void delete(Long id) {
		riderRepository.delete(getRider(id));
	}

	/**
	 * Training zones from the thresholds valid on the given date.
	 */
	public ZonesResponse getZones(Long id, LocalDate date) {
		getRider(id);
		Optional<RiderThreshold> thresholds = thresholdService.timeline(id).at(date);
		Integer ftp = thresholds.map(RiderThreshold::getFtpWatts).orElse(null);
		Integer lthr = thresholds.map(RiderThreshold::getLthrBpm).orElse(null);
		List<TrainingZone> powerZones = ftp == null ? List.of() : CogganZones.powerZones(ftp);
		List<TrainingZone> heartRateZones = lthr == null ? List.of() : CogganZones.heartRateZones(lthr);
		return new ZonesResponse(thresholds.map(RiderThreshold::getEffectiveFrom).orElse(null), ftp, lthr,
				powerZones, heartRateZones);
	}

	private RiderResponse toResponse(Rider rider) {
		return RiderResponse.from(rider, thresholdService.current(rider.getId()).orElse(null));
	}

	private Rider getRider(Long id) {
		return riderRepository.findById(id)
				.orElseThrow(() -> new RiderNotFoundException(id));
	}

	private static void applyRequest(Rider rider, RiderRequest request) {
		rider.setName(request.name());
		rider.setWeightKg(request.weightKg());
		rider.setRestingHeartRateBpm(request.restingHeartRateBpm());
	}

}
