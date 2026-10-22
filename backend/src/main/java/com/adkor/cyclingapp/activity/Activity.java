package com.adkor.cyclingapp.activity;

import java.time.Instant;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.adkor.cyclingapp.rider.Rider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "activity")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rider_id", nullable = false, updatable = false)
	private Rider rider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private ActivitySource source;

	/** Identifier of the activity in the source system. */
	@Column(name = "external_id", nullable = false, length = 64, updatable = false)
	private String externalId;

	private String name;

	@Column(name = "sport_type", length = 50)
	private String sportType;

	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@Column(name = "start_time_local")
	private LocalDateTime startTimeLocal;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "moving_duration_seconds")
	private Integer movingDurationSeconds;

	@Column(name = "distance_meters")
	private Double distanceMeters;

	@Column(name = "elevation_gain_meters")
	private Double elevationGainMeters;

	@Column(name = "average_heart_rate")
	private Integer averageHeartRate;

	@Column(name = "max_heart_rate")
	private Integer maxHeartRate;

	@Column(name = "average_power")
	private Integer averagePower;

	@Column(name = "max_power")
	private Integer maxPower;

	@Column(name = "normalized_power")
	private Integer normalizedPower;

	@Column(name = "average_cadence")
	private Integer averageCadence;

	private Integer calories;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Activity(Rider rider, ActivitySource source, String externalId) {
		this.rider = rider;
		this.source = source;
		this.externalId = externalId;
	}

}
