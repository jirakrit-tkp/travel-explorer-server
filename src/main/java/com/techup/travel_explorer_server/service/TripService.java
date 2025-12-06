package com.techup.travel_explorer_server.service;

import com.techup.travel_explorer_server.dto.trip.CreateTripRequest;
import com.techup.travel_explorer_server.dto.trip.TripDetailResponse;
import com.techup.travel_explorer_server.dto.trip.TripSummaryResponse;
import com.techup.travel_explorer_server.dto.trip.UpdateTripRequest;
import com.techup.travel_explorer_server.entity.Trip;
import com.techup.travel_explorer_server.entity.User;
import com.techup.travel_explorer_server.exception.ResourceNotFoundException;
import com.techup.travel_explorer_server.repository.TripRepository;
import com.techup.travel_explorer_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {
    
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    
    public List<TripSummaryResponse> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }
    
    public List<TripSummaryResponse> searchTrips(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTrips();
        }
        return tripRepository.searchTrips(query.trim()).stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }
    
    public TripDetailResponse getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
        return toDetailResponse(trip);
    }
    
    public List<TripDetailResponse> getMyTrips(Long userId) {
        return tripRepository.findByAuthorId(userId).stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TripDetailResponse createTrip(CreateTripRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Trip trip = Trip.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .photos(request.getPhotos() != null ? request.getPhotos() : List.of())
                .tags(request.getTags() != null ? request.getTags() : List.of())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .author(author)
                .build();
        
        trip = tripRepository.save(trip);
        return toDetailResponse(tripRepository.findById(trip.getId()).orElse(trip));
    }
    
    @Transactional
    public TripDetailResponse updateTrip(Long tripId, UpdateTripRequest request, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        
        if (!trip.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own trips");
        }
        
        if (request.getTitle() != null) {
            trip.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            trip.setDescription(request.getDescription());
        }
        if (request.getPhotos() != null) {
            trip.setPhotos(request.getPhotos());
        }
        if (request.getTags() != null) {
            trip.setTags(request.getTags());
        }
        if (request.getLatitude() != null) {
            trip.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            trip.setLongitude(request.getLongitude());
        }
        
        trip = tripRepository.save(trip);
        return toDetailResponse(tripRepository.findById(trip.getId()).orElse(trip));
    }
    
    @Transactional
    public void deleteTrip(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        
        if (!trip.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own trips");
        }
        
        tripRepository.delete(trip);
    }
    
    private TripSummaryResponse toSummaryResponse(Trip trip) {
        return TripSummaryResponse.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .photos(trip.getPhotos())
                .tags(trip.getTags())
                .build();
    }
    
    private TripDetailResponse toDetailResponse(Trip trip) {
        return TripDetailResponse.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .photos(trip.getPhotos())
                .tags(trip.getTags())
                .latitude(trip.getLatitude())
                .longitude(trip.getLongitude())
                .authorId(trip.getAuthor().getId())
                .authorEmail(trip.getAuthor().getEmail())
                .authorDisplayName(trip.getAuthor().getDisplayName())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}

