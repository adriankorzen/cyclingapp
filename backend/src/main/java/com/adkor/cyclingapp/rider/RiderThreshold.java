package com.adkor.cyclingapp.rider;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rider's training thresholds valid from {@code effectiveFrom} until the next entry of the history.
 */
@Entity
@Table(name = "rider_threshold")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderThreshold {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rider_id", nullable = false, updatable = false)
	private Rider rider;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	/** Functional Threshold Power in watts. */
	@Column(name = "ftp_watts")
	private Integer ftpWatts;

	/** Lactate Threshold Heart Rate in beats per minute. */
	@Column(name = "lthr_bpm")
	private Integer lthrBpm;

	@Column(name = "max_heart_rate_bpm")
	private Integer maxHeartRateBpm;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public RiderThreshold(Rider rider, LocalDate effectiveFrom) {
		this.rider = rider;
		this.effectiveFrom = effectiveFrom;
	}

}
