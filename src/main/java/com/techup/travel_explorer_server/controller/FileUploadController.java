package com.techup.travel_explorer_server.controller;

import com.techup.travel_explorer_server.dto.upload.FileUploadResponse;
import com.techup.travel_explorer_server.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {
    
    private final SupabaseStorageService storageService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder()
                                .filename(file.getOriginalFilename())
                                .build());
            }
            
            String url = storageService.uploadFile(file);
            
            FileUploadResponse response = FileUploadResponse.builder()
                    .url(url)
                    .filename(file.getOriginalFilename())
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files) {
        List<FileUploadResponse> responses = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    String url = storageService.uploadFile(file);
                    responses.add(FileUploadResponse.builder()
                            .url(url)
                            .filename(file.getOriginalFilename())
                            .size(file.getSize())
                            .contentType(file.getContentType())
                            .build());
                }
            } catch (IOException e) {
                // Skip failed uploads
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    
    @DeleteMapping("/upload")
    public ResponseEntity<Void> deleteFile(@RequestParam String url) {
        try {
            storageService.deleteFile(url);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

