package com.adkor.cyclingapp.analysis;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoredAnalysisRepository extends JpaRepository<StoredAnalysis, Long> {

	/**
	 * Analyses of a rider's activities that started (local time) within {@code [from, to)}, oldest first.
	 */
	@Query("""
			select analysis from StoredAnalysis analysis
			join fetch analysis.activity activity
			where activity.rider.id = :riderId
			  and activity.startTimeLocal >= :from
			  and activity.startTimeLocal < :to
			order by activity.startTime
			""")
	List<StoredAnalysis> findByRiderAndPeriod(Long riderId, LocalDateTime from, LocalDateTime to);

}
