package com.adkor.cyclingapp.activity;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

	private final ActivityService activityService;

	@GetMapping("/riders/{riderId}/activities")
	public List<ActivityResponse> findByRider(@PathVariable Long riderId) {
		return activityService.findByRider(riderId);
	}

	@GetMapping("/activities/{id}")
	public ActivityResponse findById(@PathVariable Long id) {
		return activityService.findById(id);
	}

	@GetMapping("/activities/{id}/file")
	public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
		ActivityFile file = activityService.getFile(id);
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename("activity-" + id + "." + file.getFormat().toLowerCase())
				.build();
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(file.getContent());
	}

}
