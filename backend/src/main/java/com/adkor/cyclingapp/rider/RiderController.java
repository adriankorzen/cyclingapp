package com.adkor.cyclingapp.rider;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.adkor.cyclingapp.zone.ZonesResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/riders")
@RequiredArgsConstructor
public class RiderController {

	private final RiderService riderService;

	@GetMapping
	public List<RiderResponse> findAll() {
		return riderService.findAll();
	}

	@GetMapping("/{id}")
	public RiderResponse findById(@PathVariable Long id) {
		return riderService.findById(id);
	}

	@PostMapping
	public ResponseEntity<RiderResponse> create(@Valid @RequestBody RiderRequest request) {
		RiderResponse created = riderService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public RiderResponse update(@PathVariable Long id, @Valid @RequestBody RiderRequest request) {
		return riderService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		riderService.delete(id);
	}

	/**
	 * @param date day the zones apply to, defaults to today
	 */
	@GetMapping("/{id}/zones")
	public ZonesResponse getZones(@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return riderService.getZones(id, date != null ? date : LocalDate.now());
	}

}
