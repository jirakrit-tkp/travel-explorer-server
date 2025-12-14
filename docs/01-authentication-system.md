# Feature: JWT-Based Authentication System

## 1. Overview

The authentication system provides secure user registration, login, and session management using JWT (JSON Web Tokens). It implements stateless authentication where each request includes a JWT token in the Authorization header, eliminating the need for server-side session storage.

**Purpose:**
- Secure user registration with email validation and password hashing
- User login with credential verification
- JWT token generation and validation
- Current user profile retrieval
- Stateless authentication (no server-side sessions)
- Password security using BCrypt hashing

**Key Capabilities:**
- **User Registration**: Create new accounts with email uniqueness validation
- **User Login**: Authenticate users and generate JWT tokens
- **Token Validation**: Automatic token verification on protected endpoints
- **Current User**: Retrieve authenticated user's profile
- **Password Security**: BCrypt hashing with salt (10 rounds)
- **Token Expiration**: Configurable token expiration (default 24 hours)

---

## 2. Architecture / Flow

### Registration Flow
```
POST /api/auth/register
  → Validate request (email format, password requirements)
  → Check if email already exists
    ├─ Exists? → Throw EmailAlreadyExistsException (409 Conflict)
    └─ Not exists → Continue
  → Hash password with BCrypt
  → Create User entity
  → Save to database
  → Generate JWT token (includes userId and email)
  → Return AuthResponse with token and user info
```

### Login Flow
```
POST /api/auth/login
  → Validate request (email, password)
  → Authenticate using Spring Security AuthenticationManager
    ├─ Invalid credentials? → Throw AuthenticationException (401 Unauthorized)
    └─ Valid? → Continue
  → Load user from database
  → Generate JWT token
  → Return AuthResponse with token and user info
```

### Token Validation Flow (Protected Endpoints)
```
Request with Authorization: Bearer <token>
  → JwtAuthenticationFilter intercepts request
    → Extract token from Authorization header
    → Validate token signature and expiration
      ├─ Invalid/Expired? → Continue without authentication (401 on protected routes)
      └─ Valid? → Extract userId and email
    → Load UserDetails from database
    → Set Authentication in SecurityContext
  → Controller receives authenticated request
  → SecurityUtil extracts userId from token
```

### Current User Flow
```
GET /api/auth/me
  → Extract JWT token from request
  → Extract userId from token
  → Load user from database
    ├─ Not found? → Throw ResourceNotFoundException (404)
    └─ Found? → Return user info (no token in response)
```

---

## 3. Tech Stack & Libraries

### Core Security & Authentication

| Library/Component | Purpose | Why This Choice | How It Works |
|------------------|---------|----------------|--------------|
| **Spring Security** | Authentication and authorization framework | - Industry standard for Java<br>- Built-in security features<br>- Easy integration with JWT<br>- Comprehensive security configuration | Provides authentication filter chain, password encoding, and security context management. Integrates with JWT filter for stateless authentication |
| **JJWT (io.jsonwebtoken)** | JWT token generation and validation | - Most popular JWT library for Java<br>- Supports latest JWT standards<br>- HMAC-SHA256 signing<br>- Easy token parsing | Generates signed JWT tokens with claims (userId, email, expiration). Validates token signature and expiration on each request |
| **BCryptPasswordEncoder** | Password hashing | - Industry standard hashing algorithm<br>- Built-in salt generation<br>- Resistant to rainbow table attacks<br>- Configurable cost factor | Hashes passwords with BCrypt algorithm (10 rounds). Each password gets unique salt. Verifies passwords by comparing hashes |
| **PostgreSQL** | User data persistence | - Reliable relational database<br>- ACID compliance<br>- Foreign key constraints<br>- Unique constraints for email | Stores user credentials and profile data. Enforces email uniqueness at database level |

---

## 4. Core Logic

### 4.1 Password Hashing

```java
// Registration
String hashedPassword = passwordEncoder.encode(plainPassword);
// Example: "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

// Login Verification
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
// Spring Security handles this automatically in AuthenticationManager
```

**BCrypt Properties:**
- **Algorithm**: BCrypt (Blowfish-based)
- **Cost Factor**: 10 (2^10 = 1024 rounds)
- **Salt**: Auto-generated per password
- **Output Format**: `$2a$10$<salt><hash>` (60 characters)

### 4.2 JWT Token Structure

**Token Claims:**
```json
{
  "sub": "user@example.com",        // Subject (email)
  "userId": 123,                    // Custom claim (user ID)
  "iat": 1704067200,                // Issued at (timestamp)
  "exp": 1704153600                 // Expiration (24 hours later)
}
```

**Token Generation:**
```java
String token = Jwts.builder()
    .claims(Map.of("userId", userId))
    .subject(userEmail)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expiration))
    .signWith(secretKey)
    .compact();
```

**Token Validation:**
```java
Claims claims = Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token)
    .getPayload();

// Extract userId
Long userId = claims.get("userId", Long.class);
String email = claims.getSubject();
```

### 4.3 Authentication Filter Chain

```
Request → JwtAuthenticationFilter
  → Extract "Bearer <token>" from Authorization header
  → Parse token
  → Validate signature and expiration
  → Load UserDetails from database
  → Create Authentication object
  → Set in SecurityContext
  → Continue to next filter
```

**Filter Order:**
1. CORS Filter
2. JwtAuthenticationFilter (before UsernamePasswordAuthenticationFilter)
3. Authorization Filter (checks SecurityContext)

### 4.4 Security Configuration

**Public Endpoints (No Authentication):**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/trips` (list all)
- `GET /api/trips/{id}` (detail)
- `GET /api/trips/search` (search)

**Protected Endpoints (Require Authentication):**
- `GET /api/auth/me`
- `GET /api/auth/**` (all other auth endpoints)
- `POST /api/trips` (create)
- `PUT /api/trips/{id}` (update)
- `DELETE /api/trips/{id}` (delete)
- `GET /api/trips/mine` (user's trips)
- `POST /api/files/upload` (file upload)

---

## 5. Data Model / Database Schema

### Table: `users`
```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email);
```

**Entity Mapping:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    @Email
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
```

---

## 6. API Endpoints

### POST /api/auth/register
**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "displayName": "John Doe"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "displayName": "John Doe",
  "userId": 1
}
```

**Error Responses:**
- `400 Bad Request`: Validation errors (invalid email format, missing fields)
- `409 Conflict`: Email already exists

### POST /api/auth/login
**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "displayName": "John Doe",
  "userId": 1
}
```

**Error Responses:**
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid credentials

### GET /api/auth/me
**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "email": "user@example.com",
  "displayName": "John Doe",
  "userId": 1
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token
- `404 Not Found`: User not found

### POST /api/auth/logout
**Note:** JWT is stateless, so logout is handled client-side by removing the token. This endpoint is provided for consistency.

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

---

## 7. Edge Cases / Limitations / TODO

### Edge Cases Handled
1. **Duplicate Email Registration**: Returns 409 Conflict with clear error message
2. **Invalid Token Format**: Returns 401 Unauthorized
3. **Expired Token**: Returns 401 Unauthorized
4. **Missing Authorization Header**: Returns 401 Unauthorized on protected endpoints
5. **User Not Found After Token Validation**: Returns 404 Not Found
6. **Invalid Password on Login**: Returns 401 Unauthorized (generic message for security)

### Current Limitations

1. **No Token Refresh**: Tokens must be re-issued by logging in again
2. **No Password Reset**: No forgot password functionality
3. **No Email Verification**: Email addresses are not verified
4. **No Account Lockout**: No protection against brute force attacks
5. **No Multi-Factor Authentication (MFA)**: Single-factor authentication only
6. **No Session Management**: Cannot revoke tokens before expiration
7. **No Token Blacklisting**: Expired tokens cannot be blacklisted
8. **Fixed Token Expiration**: All tokens expire after same duration (24 hours)

### TODO / Future Enhancements

- [ ] **Implement Token Refresh**: Add refresh token mechanism
- [ ] **Password Reset Flow**: Forgot password with email verification
- [ ] **Email Verification**: Send verification email on registration
- [ ] **Account Lockout**: Lock account after N failed login attempts
- [ ] **Multi-Factor Authentication**: Add 2FA support (SMS/Email/TOTP)
- [ ] **Token Revocation**: Blacklist tokens on logout
- [ ] **Remember Me**: Longer expiration for "remember me" option
- [ ] **OAuth Integration**: Login with Google, Facebook, etc.
- [ ] **Password Strength Meter**: Enforce strong password requirements
- [ ] **Login History**: Track login attempts and locations
- [ ] **Session Management Dashboard**: View and revoke active sessions
- [ ] **Rate Limiting**: Prevent brute force attacks
- [ ] **IP Whitelisting**: Restrict login from specific IPs
- [ ] **Device Management**: Track and manage logged-in devices

### Known Issues

- **Token Secret in Properties**: JWT secret should be in environment variables (not hardcoded)
- **No Token Rotation**: Same token used until expiration (security risk if compromised)
- **Generic Error Messages**: Login errors don't distinguish between wrong email vs wrong password (security best practice, but may confuse users)
- **No Password History**: Users can reuse old passwords
- **No Password Expiration**: Passwords never expire

