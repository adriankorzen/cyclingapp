package com.adkor.cyclingapp.rider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RiderThresholdRepository extends JpaRepository<RiderThreshold, Long> {

	List<RiderThreshold> findByRiderIdOrderByEffectiveFromAsc(Long riderId);

	Optional<RiderThreshold> findByIdAndRiderId(Long id, Long riderId);

	boolean existsByRiderIdAndEffectiveFrom(Long riderId, LocalDate effectiveFrom);

}
