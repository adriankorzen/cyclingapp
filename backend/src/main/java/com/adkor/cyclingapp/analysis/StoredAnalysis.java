package com.adkor.cyclingapp.analysis;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.adkor.cyclingapp.activity.Activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted analysis of an activity. Key metrics are stored as columns, the complete
 * {@link ActivityAnalysis} as JSON in {@code result}.
 */
@Entity
@Table(name = "activity_analysis")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredAnalysis {

	@Id
	@Column(name = "activity_id")
	private Long activityId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activity_id")
	private Activity activity;

	@Column(name = "analyzer_version", nullable = false)
	private int analyzerVersion;

	@Column(name = "analyzed_at", nullable = false)
	private Instant analyzedAt;

	@Column(name = "ftp_watts")
	private Integer ftpWatts;

	@Column(name = "lthr_bpm")
	private Integer lthrBpm;

	@Column(name = "max_heart_rate_bpm")
	private Integer maxHeartRateBpm;

	@Column(name = "duration_seconds", nullable = false)
	private int durationSeconds;

	@Column(name = "average_power")
	private Double averagePower;

	@Column(name = "normalized_power")
	private Double normalizedPower;

	@Column(name = "variability_index")
	private Double variabilityIndex;

	@Column(name = "intensity_factor")
	private Double intensityFactor;

	@Column(name = "training_stress_score")
	private Double trainingStressScore;

	@Column(name = "work_kilojoules")
	private Double workKilojoules;

	@Column(name = "average_heart_rate")
	private Double averageHeartRate;

	@Column(name = "max_heart_rate")
	private Double maxHeartRate;

	@Column(name = "interval_count", nullable = false)
	private int intervalCount;

	/** Complete {@link ActivityAnalysis} serialized as JSON. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String result;

	public StoredAnalysis(Activity activity) {
		this.activity = activity;
	}

}
