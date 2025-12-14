# Travel Explorer Server - Feature Documentation

This directory contains detailed documentation for each major feature of the Travel Explorer backend server.

## Documentation Index

### 1. [Authentication System](./01-authentication-system.md)
JWT-based authentication with user registration, login, and session management.
- User registration and login
- JWT token generation and validation
- Password hashing with BCrypt
- Current user profile retrieval

### 2. [Trip Management System](./02-trip-management-system.md)
Comprehensive CRUD operations for travel trips with ownership validation and advanced search.
- Public trip browsing and search
- Trip creation, update, and deletion
- Ownership-based access control
- PostgreSQL UNNEST for array search

### 3. [File Upload System](./03-file-upload-system.md)
Secure file upload to Supabase Storage with support for single and multiple files.
- Image and file upload to cloud storage
- Unique filename generation
- Public URL generation
- File deletion

### 4. [Error Handling System](./04-error-handling-system.md)
Structured error responses with custom exceptions and ErrorResponse DTO.
- Custom exception classes
- Structured error responses
- Backward-compatible error format
- Validation error handling

## Quick Links

- **API Endpoints**: See individual feature docs for endpoint details
- **Database Schema**: See "Data Model" section in each feature doc
- **Error Responses**: See [Error Handling System](./04-error-handling-system.md)
- **Authentication**: See [Authentication System](./01-authentication-system.md)

## Tech Stack Overview

- **Framework**: Spring Boot 4.0.0
- **Database**: PostgreSQL
- **Security**: Spring Security + JWT (JJWT)
- **Storage**: Supabase Storage
- **Validation**: Jakarta Validation
- **ORM**: Spring Data JPA / Hibernate

## Getting Started

1. Read the [Authentication System](./01-authentication-system.md) to understand how to authenticate
2. Review [Trip Management System](./02-trip-management-system.md) for trip operations
3. Check [File Upload System](./03-file-upload-system.md) for file handling
4. Refer to [Error Handling System](./04-error-handling-system.md) for error response formats

