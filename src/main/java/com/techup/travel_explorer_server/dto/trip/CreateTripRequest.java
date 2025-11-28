package com.techup.travel_explorer_server.dto.trip;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @Builder.Default
    private List<String> photos = new ArrayList<>();
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    private Double latitude;
    
    private Double longitude;
}

