package com.techup.travel_explorer_server.controller;

import com.techup.travel_explorer_server.dto.trip.CreateTripRequest;
import com.techup.travel_explorer_server.dto.trip.TripDetailResponse;
import com.techup.travel_explorer_server.dto.trip.TripSummaryResponse;
import com.techup.travel_explorer_server.dto.trip.UpdateTripRequest;
import com.techup.travel_explorer_server.service.TripService;
import com.techup.travel_explorer_server.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    
    private final TripService tripService;
    private final SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<TripSummaryResponse>> getAllTrips() {
        List<TripSummaryResponse> trips = tripService.getAllTrips();
        return ResponseEntity.ok(trips);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<TripSummaryResponse>> searchTrips(@RequestParam(required = false) String q) {
        List<TripSummaryResponse> trips = tripService.searchTrips(q);
        return ResponseEntity.ok(trips);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TripDetailResponse> getTripById(@PathVariable Long id) {
        TripDetailResponse trip = tripService.getTripById(id);
        return ResponseEntity.ok(trip);
    }
    
    @GetMapping("/mine")
    public ResponseEntity<List<TripDetailResponse>> getMyTrips(HttpServletRequest request) {
        Long userId = securityUtil.getCurrentUserId(request);
        List<TripDetailResponse> trips = tripService.getMyTrips(userId);
        return ResponseEntity.ok(trips);
    }
    
    @PostMapping
    public ResponseEntity<TripDetailResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            HttpServletRequest httpRequest) {
        Long userId = securityUtil.getCurrentUserId(httpRequest);
        TripDetailResponse trip = tripService.createTrip(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(trip);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TripDetailResponse> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripRequest request,
            HttpServletRequest httpRequest) {
        Long userId = securityUtil.getCurrentUserId(httpRequest);
        TripDetailResponse trip = tripService.updateTrip(id, request, userId);
        return ResponseEntity.ok(trip);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTrip(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = securityUtil.getCurrentUserId(httpRequest);
        tripService.deleteTrip(id, userId);
        return ResponseEntity.ok(Map.of("message", "Trip deleted successfully"));
    }
}
