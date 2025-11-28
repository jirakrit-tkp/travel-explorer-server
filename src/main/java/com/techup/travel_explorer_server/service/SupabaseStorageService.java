package com.techup.travel_explorer_server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.bucket}")
    private String bucket;
    
    @Value("${supabase.apiKey}")
    private String apiKey;
    
    private final WebClient webClient;
    
    public SupabaseStorageService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "TravelExplorerServer/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
    
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Build the upload URL
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", 
                supabaseUrl, bucket, uniqueFilename);
        
        try {
            // Read file content into DataBuffer (10MB buffer size)
            Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
                    () -> file.getInputStream(),
                    new org.springframework.core.io.buffer.DefaultDataBufferFactory(),
                    10 * 1024 * 1024
            );
            
            // Upload file to Supabase Storage
            webClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("x-upsert", "true") // Allow overwrite
                    .contentType(MediaType.parseMediaType(file.getContentType() != null ? 
                            file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(BodyInserters.fromDataBuffers(dataBufferFlux))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Return public URL
            return String.format("%s/storage/v1/object/public/%s/%s", 
                    supabaseUrl, bucket, uniqueFilename);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Supabase: " + e.getMessage(), e);
        }
    }
    
    public void deleteFile(String fileUrl) {
        try {
            // Extract filename from URL
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s", 
                    supabaseUrl, bucket, filename);
            
            webClient.delete()
                    .uri(deleteUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from Supabase: " + e.getMessage(), e);
        }
    }
}

