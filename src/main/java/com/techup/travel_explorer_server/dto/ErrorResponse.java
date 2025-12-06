package com.techup.travel_explorer_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    // Backward compatible fields (ยังคงมีเหมือนเดิม)
    private String message;
    private Map<String, String> errors; // สำหรับ validation errors
    
    // Additional fields (optional, ไม่กระทบ frontend)
    private OffsetDateTime timestamp;
    private Integer status;
    private String error; // HTTP status text เช่น "Not Found", "Bad Request"
    private String path;
}

