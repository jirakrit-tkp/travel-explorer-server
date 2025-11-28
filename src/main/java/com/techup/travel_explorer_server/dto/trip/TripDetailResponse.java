package com.techup.travel_explorer_server.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailResponse {
    private Long id;
    private String title;
    private String description;
    private List<String> photos;
    private List<String> tags;
    private Double latitude;
    private Double longitude;
    private Long authorId;
    private String authorEmail;
    private String authorDisplayName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

