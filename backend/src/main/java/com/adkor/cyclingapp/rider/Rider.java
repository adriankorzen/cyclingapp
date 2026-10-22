package com.adkor.cyclingapp.rider;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rider")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rider {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	/** Functional Threshold Power in watts. */
	@Column(name = "ftp_watts")
	private Integer ftpWatts;

	/** Lactate Threshold Heart Rate in beats per minute. */
	@Column(name = "lthr_bpm")
	private Integer lthrBpm;

	@Column(name = "max_heart_rate_bpm")
	private Integer maxHeartRateBpm;

	@Column(name = "resting_heart_rate_bpm")
	private Integer restingHeartRateBpm;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Rider(String name, BigDecimal weightKg) {
		this.name = name;
		this.weightKg = weightKg;
	}

}
