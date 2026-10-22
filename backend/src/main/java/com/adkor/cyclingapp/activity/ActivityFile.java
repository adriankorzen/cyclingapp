package com.adkor.cyclingapp.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Original file of an activity (e.g. FIT) as received from the source system.
 */
@Entity
@Table(name = "activity_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityFile {

	@Id
	@Column(name = "activity_id")
	private Long activityId;

	@Column(nullable = false, length = 10)
	private String format;

	@Column(nullable = false)
	private byte[] content;

	public ActivityFile(Long activityId, String format, byte[] content) {
		this.activityId = activityId;
		this.format = format;
		this.content = content;
	}

}
