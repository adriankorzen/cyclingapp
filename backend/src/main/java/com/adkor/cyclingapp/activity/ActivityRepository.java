package com.adkor.cyclingapp.activity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

	List<Activity> findByRiderIdOrderByStartTimeDesc(Long riderId);

	Optional<Activity> findFirstByRiderIdAndSourceOrderByStartTimeDesc(Long riderId, ActivitySource source);

	boolean existsByRiderIdAndSourceAndExternalId(Long riderId, ActivitySource source, String externalId);

}
