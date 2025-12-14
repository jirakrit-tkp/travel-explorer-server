# Feature: Error Handling System (Custom Exceptions & Structured Error Responses)

## 1. Overview

The error handling system provides structured, consistent error responses across the entire API using custom exception classes and a unified ErrorResponse DTO. It replaces generic RuntimeException usage with type-safe exceptions and ensures backward compatibility with existing frontend clients.

**Purpose:**
- Provide structured error responses with consistent format
- Use type-safe custom exceptions instead of generic RuntimeExceptions
- Maintain backward compatibility with frontend (message field preserved)
- Include additional metadata (timestamp, status, path) for better debugging
- Centralize error handling logic in GlobalExceptionHandler
- Improve code maintainability and readability

**Key Capabilities:**
- **Custom Exceptions**: Type-safe exception classes for different error scenarios
- **Structured Responses**: Consistent ErrorResponse DTO format
- **Backward Compatible**: Frontend can still read `message` field
- **Rich Metadata**: Includes timestamp, HTTP status, error type, and request path
- **Validation Errors**: Special handling for field-level validation errors
- **JWT Error Handling**: Specific handling for token-related errors

---

## 2. Architecture / Flow

### Exception Flow
```
Service throws Custom Exception
  → Exception propagates to Controller
  → GlobalExceptionHandler catches exception
  → Handler determines HTTP status code
  → Handler creates ErrorResponse DTO
  → Handler returns ResponseEntity with ErrorResponse
  → Client receives structured error response
```

### Error Response Flow
```
Request → Controller → Service
  → Service throws ResourceNotFoundException
    → GlobalExceptionHandler.handleResourceNotFoundException()
      → Create ErrorResponse with:
        - message: exception message
        - timestamp: current time
        - status: 404
        - error: "Not Found"
        - path: request URI
      → Return 404 with ErrorResponse
```

### Validation Error Flow
```
Request with invalid data
  → Spring Validation fails
  → MethodArgumentNotValidException thrown
  → GlobalExceptionHandler.handleValidationExceptions()
    → Extract field errors from BindingResult
    → Create ErrorResponse with:
      - message: "Validation failed"
      - errors: Map<fieldName, errorMessage>
      - timestamp, status, error, path
    → Return 400 with ErrorResponse
```

---

## 3. Tech Stack & Libraries

### Core Technologies

| Library/Component | Purpose | Why This Choice | How It Works |
|------------------|---------|----------------|--------------|
| **Spring @RestControllerAdvice** | Global exception handling | - Centralized error handling<br>- No need to handle in each controller<br>- Consistent error responses<br>- Easy to maintain | Intercepts all exceptions thrown in controllers. Routes to appropriate handler method based on exception type |
| **Jakarta Validation** | Request validation | - Standard Java validation API<br>- Declarative validation<br>- Automatic error collection | Validates request DTOs using annotations. Collects all validation errors in BindingResult |
| **Lombok** | DTO generation | - Reduces boilerplate code<br>- Builder pattern support<br>- Clean, readable code | Generates getters, setters, builders for ErrorResponse DTO automatically |

---

## 4. Core Logic

### 4.1 Custom Exception Classes

**ResourceNotFoundException:**
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s with id %d not found", resource, id));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**Usage:**
```java
// In Service
Trip trip = tripRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
```

**EmailAlreadyExistsException:**
```java
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email already exists");
    }
}
```

**FileUploadException:**
```java
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4.2 ErrorResponse DTO Structure

```java
@Data
@Builder
public class ErrorResponse {
    // Backward compatible fields
    private String message;
    private Map<String, String> errors;  // For validation errors
    
    // Additional metadata
    private OffsetDateTime timestamp;
    private Integer status;
    private String error;  // HTTP status text
    private String path;   // Request URI
}
```

**Response Format:**
```json
{
  "message": "Trip with id 123 not found",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/trips/123"
}
```

**Validation Error Format:**
```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/auth/register"
}
```

### 4.3 Exception Handler Mapping

**Handler Priority:**
1. Specific handlers (ResourceNotFoundException, EmailAlreadyExistsException, etc.)
2. Generic RuntimeException handler (for backward compatibility)
3. Generic Exception handler (catch-all)

**Handler Methods:**
```java
@ExceptionHandler(ResourceNotFoundException.class)
→ 404 Not Found

@ExceptionHandler(EmailAlreadyExistsException.class)
→ 409 Conflict

@ExceptionHandler(FileUploadException.class)
→ 500 Internal Server Error

@ExceptionHandler(MethodArgumentNotValidException.class)
→ 400 Bad Request (with field errors)

@ExceptionHandler({JwtException.class, ...})
→ 401 Unauthorized

@ExceptionHandler(AccessDeniedException.class)
→ 403 Forbidden

@ExceptionHandler(RuntimeException.class)
→ 400/404/409/401 (based on message content)

@ExceptionHandler(Exception.class)
→ 500 Internal Server Error
```

### 4.4 Backward Compatibility

**Old Response Format:**
```json
{
  "message": "Trip not found"
}
```

**New Response Format:**
```json
{
  "message": "Trip not found",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/trips/123"
}
```

**Frontend Compatibility:**
- Frontend reading only `message` field → Works unchanged
- Frontend reading `errors` map → Works unchanged
- New fields are optional and don't break existing code

---

## 5. Exception Classes

### ResourceNotFoundException
**Purpose**: Resource not found errors (404)

**Usage:**
```java
// With resource name and ID
throw new ResourceNotFoundException("Trip", 123);
// Message: "Trip with id 123 not found"

// With custom message
throw new ResourceNotFoundException("User not found");
// Message: "User not found"
```

**HTTP Status**: 404 Not Found

### EmailAlreadyExistsException
**Purpose**: Duplicate email registration (409)

**Usage:**
```java
throw new EmailAlreadyExistsException(email);
// Message: "Email already exists"
```

**HTTP Status**: 409 Conflict

### FileUploadException
**Purpose**: File upload/delete failures (500)

**Usage:**
```java
throw new FileUploadException("Failed to upload file", cause);
// Message: "Failed to upload file: <cause message>"
```

**HTTP Status**: 500 Internal Server Error

---

## 6. Error Response Examples

### 400 Bad Request (Validation Error)
```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters",
    "title": "Title is required"
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/auth/register"
}
```

### 401 Unauthorized (JWT Error)
```json
{
  "message": "Token has expired",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/trips/mine"
}
```

### 403 Forbidden (Access Denied)
```json
{
  "message": "You can only edit your own trips",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/trips/123"
}
```

### 404 Not Found (Resource Not Found)
```json
{
  "message": "Trip with id 123 not found",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/trips/123"
}
```

### 409 Conflict (Duplicate Resource)
```json
{
  "message": "Email already exists",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "path": "/api/auth/register"
}
```

### 500 Internal Server Error (Generic Error)
```json
{
  "message": "An unexpected error occurred",
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/files/upload"
}
```

---

## 7. Edge Cases / Limitations / TODO

### Edge Cases Handled
1. **Unknown Exception Types**: Caught by generic Exception handler
2. **Null Exception Messages**: Handled gracefully with default messages
3. **Validation Errors**: Field-level errors collected and returned
4. **JWT Token Errors**: Specific handling for expired, malformed, invalid signature
5. **Access Denied**: Clear 403 Forbidden responses
6. **RuntimeException Fallback**: Still handles generic RuntimeExceptions for backward compatibility

### Current Limitations

1. **No Error Codes**: Errors use HTTP status codes only (no custom error codes)
2. **No Error Localization**: Error messages are in English only
3. **No Error Logging**: Exceptions are printed to console (no structured logging)
4. **No Error Tracking**: No integration with error tracking services (Sentry, etc.)
5. **Generic 500 Errors**: Generic errors don't expose internal details (security best practice, but limits debugging)
6. **No Error Correlation IDs**: Cannot correlate errors across requests
7. **No Error Rate Limiting**: No protection against error spam
8. **Limited Error Context**: Error responses don't include request ID or correlation ID

### TODO / Future Enhancements

- [ ] **Error Codes**: Add custom error codes (e.g., `TRIP_NOT_FOUND`, `EMAIL_EXISTS`)
- [ ] **Error Localization**: Support multiple languages for error messages
- [ ] **Structured Logging**: Use SLF4J/Logback for structured error logging
- [ ] **Error Tracking**: Integrate with Sentry, Rollbar, or similar services
- [ ] **Error Correlation IDs**: Add request ID to error responses for debugging
- [ ] **Error Rate Limiting**: Prevent error spam from same client
- [ ] **Error Analytics**: Track error frequency, types, and patterns
- [ ] **Error Notifications**: Alert on critical errors (email, Slack, etc.)
- [ ] **Error Recovery**: Automatic retry for transient errors
- [ ] **Error Documentation**: API documentation for all error responses
- [ ] **Error Testing**: Unit tests for all exception handlers
- [ ] **Error Metrics**: Prometheus metrics for error rates
- [ ] **Error Sampling**: Sample errors for high-volume endpoints

### Known Issues

- **RuntimeException Handler**: Still checks message strings (not fully type-safe)
- **Error Message Consistency**: Some error messages may vary slightly
- **No Error Stack Traces**: Stack traces not included in responses (security, but limits debugging)
- **Generic Exception Handler**: Catches all exceptions (may hide unexpected errors)
- **No Error Context**: Error responses don't include request context (headers, body, etc.)

