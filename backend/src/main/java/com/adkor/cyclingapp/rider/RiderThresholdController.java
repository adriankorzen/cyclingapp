package com.adkor.cyclingapp.rider;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/riders/{riderId}/thresholds")
@RequiredArgsConstructor
public class RiderThresholdController {

	private final RiderThresholdService thresholdService;

	@GetMapping
	public List<ThresholdResponse> findAll(@PathVariable Long riderId) {
		return thresholdService.findAll(riderId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ThresholdResponse create(@PathVariable Long riderId, @Valid @RequestBody ThresholdRequest request) {
		return thresholdService.create(riderId, request);
	}

	@PutMapping("/{thresholdId}")
	public ThresholdResponse update(@PathVariable Long riderId, @PathVariable Long thresholdId,
			@Valid @RequestBody ThresholdRequest request) {
		return thresholdService.update(riderId, thresholdId, request);
	}

	@DeleteMapping("/{thresholdId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long riderId, @PathVariable Long thresholdId) {
		thresholdService.delete(riderId, thresholdId);
	}

}
