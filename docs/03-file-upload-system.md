# Feature: File Upload System (Supabase Storage Integration)

## 1. Overview

The file upload system provides secure image and file upload capabilities using Supabase Storage. It supports single and multiple file uploads, generates unique filenames to prevent conflicts, and returns public URLs for uploaded files. The system integrates with Spring WebFlux for reactive file handling and supports large files up to 10MB.

**Purpose:**
- Upload images and files to cloud storage (Supabase)
- Generate unique filenames to prevent overwrites
- Return public URLs for uploaded files
- Support single and multiple file uploads
- Delete uploaded files from storage
- Handle large files efficiently with streaming

**Key Capabilities:**
- **Single File Upload**: Upload one file at a time
- **Multiple File Upload**: Upload multiple files in one request
- **Unique Filenames**: UUID-based filename generation
- **Public URLs**: Return accessible URLs for uploaded files
- **File Deletion**: Remove files from storage
- **Error Handling**: Comprehensive error handling with custom exceptions
- **Streaming Upload**: Efficient handling of large files using reactive streams

---

## 2. Architecture / Flow

### Single File Upload Flow
```
POST /api/files/upload
  → Receive MultipartFile from request
  → Validate file (not empty)
    ├─ Empty? → Return 400 Bad Request
    └─ Valid? → Continue
  → Generate unique filename (UUID + original extension)
  → Build Supabase Storage upload URL
  → Convert file to DataBuffer Flux (reactive stream)
  → POST to Supabase Storage API
    ├─ Success? → Return public URL
    └─ Error? → Throw FileUploadException
  → Return FileUploadResponse with URL and metadata
```

### Multiple File Upload Flow
```
POST /api/files/upload/multiple
  → Receive MultipartFile[] array
  → Iterate through each file
    → Validate file (not empty)
    → Upload to Supabase (same as single upload)
    → Add response to list (skip failed uploads)
  → Return list of FileUploadResponse
```

### File Deletion Flow
```
DELETE /api/files/upload?url=<file_url>
  → Extract filename from URL
  → Build Supabase Storage delete URL
  → DELETE to Supabase Storage API
    ├─ Success? → Return 204 No Content
    └─ Error? → Return 500 Internal Server Error
```

---

## 3. Tech Stack & Libraries

### Core Technologies

| Library/Component | Purpose | Why This Choice | How It Works |
|------------------|---------|----------------|--------------|
| **Supabase Storage** | Cloud file storage | - Built-in CDN for fast delivery<br>- Simple REST API<br>- Public/private bucket support<br>- Automatic URL generation | Stores files in cloud buckets. Provides REST API for upload/delete. Returns public URLs accessible via HTTP |
| **Spring WebFlux** | Reactive HTTP client | - Non-blocking I/O<br>- Efficient for large files<br>- Streaming support<br>- Better resource utilization | Uses reactive streams (Flux/Mono) for file uploads. Handles large files without blocking threads |
| **WebClient** | HTTP client for Supabase API | - Reactive and non-blocking<br>- Built into Spring WebFlux<br>- Easy to configure<br>- Supports streaming | Makes HTTP requests to Supabase Storage API. Handles file streaming with DataBuffer |
| **DataBuffer** | Reactive file buffer | - Memory-efficient<br>- Supports streaming<br>- Handles large files<br>- Part of Spring WebFlux | Represents file data as reactive stream. Allows processing large files without loading entirely into memory |

---

## 4. Core Logic

### 4.1 Unique Filename Generation

```java
String originalFilename = file.getOriginalFilename();
String extension = "";
if (originalFilename != null && originalFilename.contains(".")) {
    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
}
String uniqueFilename = UUID.randomUUID().toString() + extension;
```

**Example:**
- Original: `vacation-photo.jpg`
- Generated: `a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`

**Benefits:**
- Prevents filename conflicts
- Avoids overwriting existing files
- Maintains original file extension
- URL-safe (UUID is URL-safe)

### 4.2 Supabase Storage URL Structure

**Upload URL:**
```
POST {supabaseUrl}/storage/v1/object/{bucket}/{filename}
Headers:
  Authorization: Bearer {apiKey}
  x-upsert: true
  Content-Type: {fileContentType}
Body: {file binary data}
```

**Public URL (Returned to Client):**
```
{supabaseUrl}/storage/v1/object/public/{bucket}/{filename}
```

**Delete URL:**
```
DELETE {supabaseUrl}/storage/v1/object/{bucket}/{filename}
Headers:
  Authorization: Bearer {apiKey}
```

### 4.3 Reactive File Streaming

```java
// Convert MultipartFile to reactive stream
Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
    () -> file.getInputStream(),
    new DefaultDataBufferFactory(),
    10 * 1024 * 1024  // 10MB buffer size
);

// Upload using WebClient
webClient.post()
    .uri(uploadUrl)
    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
    .contentType(MediaType.parseMediaType(file.getContentType()))
    .body(BodyInserters.fromDataBuffers(dataBufferFlux))
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

**How It Works:**
1. `DataBufferUtils.readInputStream()` creates reactive stream from file input
2. Stream is processed in chunks (10MB buffer)
3. WebClient sends chunks to Supabase as they're read
4. Non-blocking: doesn't load entire file into memory

### 4.4 Error Handling

**FileUploadException:**
```java
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Error Scenarios:**
- Empty file → `IllegalArgumentException` (400 Bad Request)
- Upload failure → `FileUploadException` (500 Internal Server Error)
- Delete failure → `FileUploadException` (500 Internal Server Error)
- Network error → Wrapped in `FileUploadException`

---

## 5. Configuration

### Application Properties
```properties
# Supabase Configuration
supabase.url=https://your-project.supabase.co
supabase.bucket=travel-explorer-images
supabase.apiKey=your-service-role-key
```

### WebClient Configuration
```java
WebClient.builder()
    .defaultHeader(HttpHeaders.USER_AGENT, "TravelExplorerServer/1.0")
    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
    .build();
```

**Configuration Details:**
- **Max In-Memory Size**: 10MB (files larger than this are streamed)
- **User Agent**: Identifies server in Supabase logs
- **Default Headers**: Can add common headers here

---

## 6. API Endpoints

### POST /api/files/upload
**Protected endpoint** - Requires authentication

**Headers:**
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Request:**
```
POST /api/files/upload
Content-Type: multipart/form-data

file: <binary file data>
```

**Response (201 Created):**
```json
{
  "url": "https://your-project.supabase.co/storage/v1/object/public/travel-explorer-images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "filename": "vacation-photo.jpg",
  "size": 245678,
  "contentType": "image/jpeg"
}
```

**Error Responses:**
- `400 Bad Request`: Empty file
- `401 Unauthorized`: Missing or invalid token
- `500 Internal Server Error`: Upload failure

### POST /api/files/upload/multiple
**Protected endpoint** - Requires authentication

**Headers:**
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Request:**
```
POST /api/files/upload/multiple
Content-Type: multipart/form-data

files: <file1 binary>
files: <file2 binary>
files: <file3 binary>
```

**Response (201 Created):**
```json
[
  {
    "url": "https://...",
    "filename": "photo1.jpg",
    "size": 245678,
    "contentType": "image/jpeg"
  },
  {
    "url": "https://...",
    "filename": "photo2.jpg",
    "size": 189234,
    "contentType": "image/jpeg"
  }
]
```

**Note**: Failed uploads are skipped (not included in response). Only successful uploads are returned.

### DELETE /api/files/upload?url=<file_url>
**Protected endpoint** - Requires authentication

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `url` (required): Public URL of file to delete

**Response (204 No Content):**
```
(Empty body)
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token
- `500 Internal Server Error`: Delete failure

---

## 7. Edge Cases / Limitations / TODO

### Edge Cases Handled
1. **Empty File**: Returns 400 Bad Request before attempting upload
2. **Missing File Extension**: Handles files without extensions gracefully
3. **Null Filename**: Handles null original filename
4. **Upload Failure**: Wraps exceptions in FileUploadException
5. **Multiple Upload Partial Failure**: Skips failed files, returns successful ones
6. **Invalid URL for Delete**: Extracts filename from URL safely
7. **Network Timeout**: Handled by WebClient timeout configuration

### Current Limitations

1. **No File Type Validation**: Accepts any file type (no MIME type checking)
2. **No File Size Limit**: No maximum file size enforced (relies on Supabase limits)
3. **No Image Processing**: No resizing, compression, or format conversion
4. **No Virus Scanning**: Files are not scanned for malware
5. **No File Metadata**: Does not store file metadata (dimensions, EXIF, etc.)
6. **No Progress Tracking**: Cannot track upload progress
7. **No Chunked Upload**: Large files uploaded in single request (no resume support)
8. **No File Versioning**: Uploading same filename overwrites (x-upsert: true)
9. **No Access Control**: All uploaded files are public (no private files)
10. **No CDN Cache Control**: No cache headers set on uploaded files

### TODO / Future Enhancements

- [ ] **File Type Validation**: Validate MIME types and file extensions
- [ ] **File Size Limits**: Enforce maximum file size (e.g., 10MB for images)
- [ ] **Image Processing**: Resize, compress, and convert images
- [ ] **Virus Scanning**: Integrate virus scanning service
- [ ] **File Metadata Extraction**: Extract and store image dimensions, EXIF data
- [ ] **Upload Progress**: Track and report upload progress
- [ ] **Chunked Upload**: Support resumable uploads for large files
- [ ] **File Versioning**: Keep version history of uploaded files
- [ ] **Access Control**: Support private files with signed URLs
- [ ] **CDN Cache Control**: Set appropriate cache headers
- [ ] **Thumbnail Generation**: Auto-generate thumbnails for images
- [ ] **File Compression**: Compress files before upload
- [ ] **Duplicate Detection**: Detect and prevent duplicate file uploads
- [ ] **File Cleanup**: Auto-delete orphaned files (not referenced in trips)
- [ ] **Upload Queue**: Queue uploads for batch processing
- [ ] **File Analytics**: Track file access, downloads, bandwidth usage

### Known Issues

- **Blocking Upload**: Uses `.block()` which blocks thread (should use reactive chain)
- **No Retry Logic**: Failed uploads are not retried automatically
- **Memory Usage**: Large files may still consume memory despite streaming
- **URL Extraction**: Filename extraction from URL is fragile (assumes specific URL format)
- **Error Details**: Generic error messages don't provide detailed failure reasons
- **No Rate Limiting**: Users can upload unlimited files (no quota enforcement)

