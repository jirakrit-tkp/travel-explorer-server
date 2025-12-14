# Feature: Trip Management System (with Ownership Validation & Advanced Search)

## 1. Overview

The trip management system provides comprehensive CRUD operations for travel trips with ownership-based access control. Users can create, read, update, and delete their own trips, while public endpoints allow browsing and searching all trips. The system includes advanced search capabilities using PostgreSQL array operations to search across titles, descriptions, and tags.

**Purpose:**
- Create and manage personal travel trips
- Browse and search all public trips
- View detailed trip information with author details
- Enforce ownership validation (users can only modify their own trips)
- Advanced search across multiple fields using PostgreSQL UNNEST
- Support for trip photos, tags, and location data

**Key Capabilities:**
- **Public Trip Browsing**: List all trips without authentication
- **Trip Search**: Search trips by title, description, or tags using PostgreSQL UNNEST
- **Ownership Validation**: Users can only edit/delete their own trips
- **Rich Trip Data**: Support for photos (array), tags (array), location (lat/lng)
- **Author Information**: Include author details in trip responses
- **Efficient Queries**: Use EntityGraph to prevent N+1 query problems

---

## 2. Architecture / Flow

### Create Trip Flow
```
POST /api/trips
  → Extract userId from JWT token
  → Validate request (title required, optional fields)
  → Load User entity (author)
  → Create Trip entity
  → Set author relationship
  → Save to database
  → Return TripDetailResponse with author info
```

### Update Trip Flow
```
PUT /api/trips/{id}
  → Extract userId from JWT token
  → Load Trip from database
    ├─ Not found? → Throw ResourceNotFoundException (404)
    └─ Found? → Continue
  → Check ownership (trip.author.id == userId)
    ├─ Not owner? → Throw AccessDeniedException (403 Forbidden)
    └─ Owner? → Continue
  → Update allowed fields (title, description, photos, tags, lat, lng)
  → Save to database
  → Return updated TripDetailResponse
```

### Delete Trip Flow
```
DELETE /api/trips/{id}
  → Extract userId from JWT token
  → Load Trip from database
    ├─ Not found? → Throw ResourceNotFoundException (404)
    └─ Found? → Continue
  → Check ownership (trip.author.id == userId)
    ├─ Not owner? → Throw AccessDeniedException (403 Forbidden)
    └─ Owner? → Continue
  → Delete from database
  → Return success message
```

### Search Flow
```
GET /api/trips/search?q=beach
  → Validate query parameter
  → Execute native SQL query with PostgreSQL UNNEST
    → Search in title (LIKE)
    → Search in description (LIKE)
    → Search in tags array (UNNEST + LIKE)
  → Return matching trips
```

**Search Query Logic:**
```sql
SELECT DISTINCT t.* FROM trips t
LEFT JOIN users u ON t.author_id = u.id
WHERE 
  LOWER(t.title) LIKE LOWER('%beach%') OR
  LOWER(t.description) LIKE LOWER('%beach%') OR
  EXISTS (
    SELECT 1 FROM unnest(t.tags) AS tag 
    WHERE LOWER(tag) LIKE LOWER('%beach%')
  )
```

---

## 3. Tech Stack & Libraries

### Core Technologies

| Library/Component | Purpose | Why This Choice | How It Works |
|------------------|---------|----------------|--------------|
| **Spring Data JPA** | Database access and ORM | - Reduces boilerplate code<br>- Automatic query generation<br>- Entity relationship management<br>- Built-in pagination support | Maps Java entities to database tables. Provides repository pattern for CRUD operations. Handles lazy/eager loading with EntityGraph |
| **PostgreSQL** | Relational database | - Native array support (TEXT[])<br>- Advanced SQL features (UNNEST)<br>- ACID compliance<br>- Foreign key constraints | Stores trip data with array columns for photos and tags. Uses UNNEST for array element search |
| **Hibernate** | JPA implementation | - Automatic schema mapping<br>- Lazy loading support<br>- EntityGraph for optimized queries<br>- Timestamp management | Manages entity lifecycle, generates SQL queries, handles relationships. Uses EntityGraph to fetch related entities in single query |
| **Jakarta Validation** | Request validation | - Standard Java validation API<br>- Declarative validation<br>- Custom validators support | Validates request DTOs using annotations (@NotBlank, @Email, etc.). Returns structured error responses |

---

## 4. Core Logic

### 4.1 Ownership Validation

```java
// In TripService.updateTrip()
Trip trip = tripRepository.findById(tripId)
    .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

if (!trip.getAuthor().getId().equals(userId)) {
    throw new AccessDeniedException("You can only edit your own trips");
}
```

**Validation Points:**
- **Update**: Check ownership before allowing updates
- **Delete**: Check ownership before allowing deletion
- **Read (mine)**: Filter by authorId for user's trips

### 4.2 PostgreSQL Array Search (UNNEST)

**Problem**: Search for query string in tags array (TEXT[])

**Solution**: Use PostgreSQL UNNEST to expand array into rows, then search

```sql
EXISTS (
  SELECT 1 FROM unnest(t.tags) AS tag 
  WHERE LOWER(tag) LIKE LOWER(CONCAT('%', :query, '%'))
)
```

**How It Works:**
1. `unnest(t.tags)` expands array `['beach', 'summer', 'tropical']` into rows:
   ```
   tag
   ----
   beach
   summer
   tropical
   ```
2. `WHERE LOWER(tag) LIKE ...` filters matching tags
3. `EXISTS` returns true if any tag matches

**Example:**
- Trip tags: `['beach', 'summer', 'tropical']`
- Query: `'beach'`
- Result: Match found (tag 'beach' matches)

### 4.3 EntityGraph for N+1 Prevention

**Problem**: Loading trips with authors causes N+1 queries

**Solution**: Use EntityGraph to fetch author in same query

```java
@EntityGraph(attributePaths = {"author"})
List<Trip> findAll();
```

**Without EntityGraph:**
```
Query 1: SELECT * FROM trips
Query 2: SELECT * FROM users WHERE id = 1
Query 3: SELECT * FROM users WHERE id = 2
Query 4: SELECT * FROM users WHERE id = 3
... (N+1 queries)
```

**With EntityGraph:**
```
Query 1: SELECT t.*, u.* FROM trips t LEFT JOIN users u ON t.author_id = u.id
(1 query total)
```

### 4.4 Response DTOs

**TripSummaryResponse** (for list/search):
```json
{
  "id": 1,
  "title": "Beach Paradise",
  "description": "Amazing beach destination",
  "photos": ["https://..."],
  "tags": ["beach", "summer"]
}
```

**TripDetailResponse** (for detail/mine):
```json
{
  "id": 1,
  "title": "Beach Paradise",
  "description": "Amazing beach destination",
  "photos": ["https://..."],
  "tags": ["beach", "summer"],
  "latitude": 13.7563,
  "longitude": 100.5018,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

---

## 5. Data Model / Database Schema

### Table: `trips`
```sql
CREATE TABLE trips (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  photos TEXT[] NOT NULL DEFAULT '{}',
  tags TEXT[] NOT NULL DEFAULT '{}',
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  author_id BIGINT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  CONSTRAINT fk_trip_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_trips_author ON trips(author_id);
CREATE INDEX idx_trips_created_at ON trips(created_at DESC);
```

**Entity Mapping:**
```java
@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "photos", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> photos;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
```

---

## 6. API Endpoints

### GET /api/trips
**Public endpoint** - No authentication required

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Beach Paradise",
    "description": "Amazing beach destination",
    "photos": ["https://..."],
    "tags": ["beach", "summer"]
  }
]
```

### GET /api/trips/search?q=beach
**Public endpoint** - No authentication required

**Query Parameters:**
- `q` (optional): Search query string

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Beach Paradise",
    "description": "Amazing beach destination",
    "photos": ["https://..."],
    "tags": ["beach", "summer"]
  }
]
```

**Note**: If `q` is empty or null, returns all trips (same as GET /api/trips)

### GET /api/trips/{id}
**Public endpoint** - No authentication required

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Beach Paradise",
  "description": "Amazing beach destination",
  "photos": ["https://..."],
  "tags": ["beach", "summer"],
  "latitude": 13.7563,
  "longitude": 100.5018,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

**Error Responses:**
- `404 Not Found`: Trip not found

### GET /api/trips/mine
**Protected endpoint** - Requires authentication

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "My Beach Trip",
    "description": "...",
    "photos": ["https://..."],
    "tags": ["beach"],
    "latitude": 13.7563,
    "longitude": 100.5018,
    "authorId": 1,
    "authorEmail": "user@example.com",
    "authorDisplayName": "John Doe",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
]
```

### POST /api/trips
**Protected endpoint** - Requires authentication

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "title": "My Amazing Trip",
  "description": "Trip description",
  "photos": ["https://..."],
  "tags": ["adventure", "mountains"],
  "latitude": 13.7563,
  "longitude": 100.5018
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "title": "My Amazing Trip",
  "description": "Trip description",
  "photos": ["https://..."],
  "tags": ["adventure", "mountains"],
  "latitude": 13.7563,
  "longitude": 100.5018,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Validation errors (missing title, etc.)
- `401 Unauthorized`: Missing or invalid token

### PUT /api/trips/{id}
**Protected endpoint** - Requires authentication + ownership

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "title": "Updated Title",
  "description": "Updated description",
  "photos": ["https://..."],
  "tags": ["updated", "tags"],
  "latitude": 13.7563,
  "longitude": 100.5018
}
```

**Note**: All fields are optional. Only provided fields will be updated.

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Updated Title",
  "description": "Updated description",
  ...
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Not the trip owner
- `404 Not Found`: Trip not found

### DELETE /api/trips/{id}
**Protected endpoint** - Requires authentication + ownership

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "message": "Trip deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Not the trip owner
- `404 Not Found`: Trip not found

---

## 7. Edge Cases / Limitations / TODO

### Edge Cases Handled
1. **Empty Search Query**: Returns all trips (same as GET /api/trips)
2. **Trip Not Found**: Returns 404 with clear error message
3. **Unauthorized Update/Delete**: Returns 403 Forbidden
4. **Null Optional Fields**: Handles null photos/tags arrays gracefully
5. **Array Search**: Uses UNNEST to search within tags array
6. **Case-Insensitive Search**: Converts to lowercase for matching
7. **Partial Matches**: Uses LIKE with wildcards for flexible search

### Current Limitations

1. **No Pagination**: All trips returned in single response (can be slow for large datasets)
2. **No Sorting Options**: Trips returned in database order (usually by ID)
3. **No Filtering**: Cannot filter by author, date range, tags, etc.
4. **No Full-Text Search**: Uses simple LIKE matching (not PostgreSQL full-text search)
5. **No Search Ranking**: All matches returned equally (no relevance scoring)
6. **No Image Validation**: Photos array accepts any URLs (no validation)
7. **No Tag Limit**: No maximum number of tags enforced
8. **No Location Search**: Cannot search trips by location/radius
9. **No Draft/Published Status**: All trips are immediately public
10. **No Soft Delete**: Trips are permanently deleted (no recovery)

### TODO / Future Enhancements

- [ ] **Pagination**: Add page and size parameters for list endpoints
- [ ] **Sorting**: Add sort parameter (by date, title, author, etc.)
- [ ] **Advanced Filtering**: Filter by author, date range, tags, location
- [ ] **Full-Text Search**: Use PostgreSQL tsvector for better search
- [ ] **Search Ranking**: Rank results by relevance score
- [ ] **Image Upload Validation**: Validate image URLs and formats
- [ ] **Tag Management**: Tag suggestions, popular tags, tag limits
- [ ] **Location Search**: Search trips within radius of coordinates
- [ ] **Draft/Published**: Allow users to save drafts before publishing
- [ ] **Soft Delete**: Add deleted_at column for recovery
- [ ] **Trip Sharing**: Generate shareable links for trips
- [ ] **Trip Favorites**: Allow users to favorite trips
- [ ] **Trip Comments**: Add commenting system
- [ ] **Trip Ratings**: Add rating/review system
- [ ] **Trip Categories**: Categorize trips (adventure, relaxation, etc.)
- [ ] **Trip Itinerary**: Add day-by-day itinerary support
- [ ] **Export Trip**: Export trip as PDF or JSON
- [ ] **Trip Analytics**: Track views, favorites, shares

### Known Issues

- **Search Performance**: UNNEST on large arrays can be slow (no index on array elements)
- **N+1 Query Risk**: If EntityGraph not used, will cause N+1 queries
- **Case Sensitivity**: Search is case-insensitive but may miss some edge cases
- **SQL Injection Risk**: Native query uses parameter binding (safe), but should be reviewed
- **Array Size Limit**: PostgreSQL arrays have practical size limits
- **No Transaction Rollback**: If update fails partway, partial updates may occur

