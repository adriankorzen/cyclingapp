package com.adkor.cyclingapp.rider;

import java.util.List;

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

	public List<RiderResponse> findAll() {
		return riderRepository.findAll().stream()
				.map(RiderResponse::from)
				.toList();
	}

	public RiderResponse findById(Long id) {
		return RiderResponse.from(getRider(id));
	}

	@Transactional
	public RiderResponse create(RiderRequest request) {
		Rider rider = new Rider(request.name(), request.weightKg());
		applyRequest(rider, request);
		return RiderResponse.from(riderRepository.saveAndFlush(rider));
	}

	@Transactional
	public RiderResponse update(Long id, RiderRequest request) {
		Rider rider = getRider(id);
		applyRequest(rider, request);
		return RiderResponse.from(riderRepository.saveAndFlush(rider));
	}

	@Transactional
	public void delete(Long id) {
		riderRepository.delete(getRider(id));
	}

	public ZonesResponse getZones(Long id) {
		Rider rider = getRider(id);
		List<TrainingZone> powerZones = rider.getFtpWatts() == null
				? List.of()
				: CogganZones.powerZones(rider.getFtpWatts());
		List<TrainingZone> heartRateZones = rider.getLthrBpm() == null
				? List.of()
				: CogganZones.heartRateZones(rider.getLthrBpm());
		return new ZonesResponse(rider.getFtpWatts(), rider.getLthrBpm(), powerZones, heartRateZones);
	}

	private Rider getRider(Long id) {
		return riderRepository.findById(id)
				.orElseThrow(() -> new RiderNotFoundException(id));
	}

	private static void applyRequest(Rider rider, RiderRequest request) {
		rider.setName(request.name());
		rider.setWeightKg(request.weightKg());
		rider.setFtpWatts(request.ftpWatts());
		rider.setLthrBpm(request.lthrBpm());
		rider.setMaxHeartRateBpm(request.maxHeartRateBpm());
		rider.setRestingHeartRateBpm(request.restingHeartRateBpm());
	}

}
