# Travel Explorer API Documentation

**Base URL:** `http://localhost:8080/api`

**Authentication:** JWT Bearer Token (required for authenticated endpoints)

---

## Table of Contents

1. [Authentication](#authentication)
2. [Trips](#trips)
3. [File Upload](#file-upload)
4. [Error Responses](#error-responses)

---

## Authentication

### Register

Create a new user account

**Endpoint:** `POST /api/auth/register`

**Authentication:** Not required

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "John Doe"
}
```

**Request Fields:**
- `email` (string, required): User email (must be a valid email format)
- `password` (string, required): Password (minimum 6 characters)
- `displayName` (string, optional): Display name (maximum 100 characters)

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
- `400 Bad Request`: Validation error
- `409 Conflict`: Email already exists

---

### Login

User login

**Endpoint:** `POST /api/auth/login`

**Authentication:** Not required

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Request Fields:**
- `email` (string, required): User email
- `password` (string, required): Password

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
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Invalid credentials

---

### Get Current User

Get current user information

**Endpoint:** `GET /api/auth/me`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "email": "user@example.com",
  "displayName": "John Doe",
  "userId": 1
}
```

**Note:** Response does not include `token` field

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token

---

### Logout

User logout

**Endpoint:** `POST /api/auth/logout`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Note:** JWT is a stateless token, so actual logout requires removing the token on the client side

---

## Trips

### Get All Trips

Get all trips (summary)

**Endpoint:** `GET /api/trips`

**Authentication:** Not required

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Trip to Bangkok",
    "description": "Amazing trip to the capital",
    "photos": ["photo1.jpg", "photo2.jpg"],
    "tags": ["bangkok", "thailand", "city"]
  },
  {
    "id": 2,
    "title": "Beach Paradise",
    "description": "Relaxing beach vacation",
    "photos": ["beach1.jpg"],
    "tags": ["beach", "vacation"]
  }
]
```

---

### Search Trips

Search trips

**Endpoint:** `GET /api/trips/search?q={query}`

**Authentication:** Not required

**Query Parameters:**
- `q` (string, optional): Search query (searches in title, description, and tags)

**Examples:**
- `GET /api/trips/search?q=bangkok`
- `GET /api/trips/search?q=beach`
- `GET /api/trips/search` (if q is not provided, returns all trips)

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Trip to Bangkok",
    "description": "Amazing trip to the capital",
    "photos": ["photo1.jpg"],
    "tags": ["bangkok", "thailand"]
  }
]
```

**Search Behavior:**
- Case-insensitive search (ignores letter case)
- Partial match search (contains the query)
- Searches in: `title`, `description`, and `tags` array

---

### Get Trip by ID

Get trip details

**Endpoint:** `GET /api/trips/{id}`

**Authentication:** Not required

**Path Parameters:**
- `id` (number, required): Trip ID

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Trip to Bangkok",
  "description": "Amazing trip to the capital of Thailand",
  "photos": ["photo1.jpg", "photo2.jpg"],
  "tags": ["bangkok", "thailand", "city"],
  "latitude": 13.7563,
  "longitude": 100.5018,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2025-11-28T10:00:00Z",
  "updatedAt": "2025-11-28T10:00:00Z"
}
```

**Error Responses:**
- `404 Not Found`: Trip not found

---

### Get My Trips

Get current user's trips

**Endpoint:** `GET /api/trips/mine`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "My Trip",
    "description": "My trip description",
    "photos": ["photo1.jpg"],
    "tags": ["tag1"],
    "latitude": 13.7563,
    "longitude": 100.5018,
    "authorId": 1,
    "authorEmail": "user@example.com",
    "authorDisplayName": "John Doe",
    "createdAt": "2025-11-28T10:00:00Z",
    "updatedAt": "2025-11-28T10:00:00Z"
  }
]
```

---

### Create Trip

Create a new trip

**Endpoint:** `POST /api/trips`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "Trip to Phuket",
  "description": "Beautiful beaches and amazing food",
  "photos": ["phuket1.jpg", "phuket2.jpg"],
  "tags": ["phuket", "beach", "thailand"],
  "latitude": 7.8804,
  "longitude": 98.3923
}
```

**Request Fields:**
- `title` (string, required): Trip title
- `description` (string, optional): Description
- `photos` (array of strings, optional): List of image URLs
- `tags` (array of strings, optional): List of tags
- `latitude` (number, optional): Latitude
- `longitude` (number, optional): Longitude

**Response (201 Created):**
```json
{
  "id": 3,
  "title": "Trip to Phuket",
  "description": "Beautiful beaches and amazing food",
  "photos": ["phuket1.jpg", "phuket2.jpg"],
  "tags": ["phuket", "beach", "thailand"],
  "latitude": 7.8804,
  "longitude": 98.3923,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2025-11-28T11:00:00Z",
  "updatedAt": "2025-11-28T11:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Invalid or missing token

---

### Update Trip

Update a trip

**Endpoint:** `PUT /api/trips/{id}`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Parameters:**
- `id` (number, required): Trip ID

**Request Body:**
```json
{
  "title": "Updated Trip Title",
  "description": "Updated description",
  "tags": ["updated", "tags"]
}
```

**Request Fields:**
- All fields are optional (send only fields you want to update)
- `title` (string, optional)
- `description` (string, optional)
- `photos` (array of strings, optional)
- `tags` (array of strings, optional)
- `latitude` (number, optional)
- `longitude` (number, optional)

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Updated Trip Title",
  "description": "Updated description",
  "photos": ["photo1.jpg"],
  "tags": ["updated", "tags"],
  "latitude": 13.7563,
  "longitude": 100.5018,
  "authorId": 1,
  "authorEmail": "user@example.com",
  "authorDisplayName": "John Doe",
  "createdAt": "2025-11-28T10:00:00Z",
  "updatedAt": "2025-11-28T12:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: You can only edit your own trips
- `404 Not Found`: Trip not found

---

### Delete Trip

Delete a trip

**Endpoint:** `DELETE /api/trips/{id}`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
- `id` (number, required): Trip ID

**Response (200 OK):**
```json
{
  "message": "Trip deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: You can only delete your own trips
- `404 Not Found`: Trip not found

---

## File Upload

### Upload Single File

Upload a single file

**Endpoint:** `POST /api/files/upload`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request Body (Form Data):**
- `file` (file, required): File to upload

**Response (201 Created):**
```json
{
  "url": "https://cxtooizbszvmraxzkchu.supabase.co/storage/v1/object/public/uploads/uuid.jpg",
  "filename": "original-filename.jpg",
  "size": 123456,
  "contentType": "image/jpeg"
}
```

**Response Fields:**
- `url` (string): Public URL to access the file
- `filename` (string): Original filename
- `size` (number): File size in bytes
- `contentType` (string): MIME type of the file

**Error Responses:**
- `400 Bad Request`: File is empty
- `401 Unauthorized`: Invalid or missing token
- `500 Internal Server Error`: Upload failed

**Note:**
- Filename will be converted to UUID to prevent name conflicts
- Files are stored in Supabase Storage bucket `uploads`
- Supports all file types (images, documents, etc.)

---

### Upload Multiple Files

Upload multiple files

**Endpoint:** `POST /api/files/upload/multiple`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request Body (Form Data):**
- `files` (array of files, required): Files to upload (multiple files)

**Response (201 Created):**
```json
[
  {
    "url": "https://.../uploads/uuid1.jpg",
    "filename": "file1.jpg",
    "size": 123456,
    "contentType": "image/jpeg"
  },
  {
    "url": "https://.../uploads/uuid2.png",
    "filename": "file2.png",
    "size": 789012,
    "contentType": "image/png"
  }
]
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `500 Internal Server Error`: Upload failed

**Note:** Successfully uploaded files will be added to the response array (failed uploads will be skipped)

---

### Delete File

Delete a file

**Endpoint:** `DELETE /api/files/upload?url={fileUrl}`

**Authentication:** Required (Bearer Token)

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `url` (string, required): URL of the file to delete

**Example:**
```
DELETE /api/files/upload?url=https://.../uploads/filename.jpg
```

**Response (204 No Content):**
No response body

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `500 Internal Server Error`: Delete failed

---

## Error Responses

### Validation Error (400 Bad Request)

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "Email should be valid",
    "password": "Password must be at least 6 characters"
  }
}
```

### Unauthorized (401 Unauthorized)

```json
{
  "message": "Invalid or expired token"
}
```

or

```json
{
  "message": "No authentication token found"
}
```

### Forbidden (403 Forbidden)

```json
{
  "message": "You can only edit your own trips"
}
```

### Not Found (404 Not Found)

```json
{
  "message": "Trip not found"
}
```

### Conflict (409 Conflict)

```json
{
  "message": "Email already exists"
}
```

### Internal Server Error (500 Internal Server Error)

```json
{
  "message": "An unexpected error occurred"
}
```

---

## Authentication Flow

1. **Register/Login** → Receive JWT token
2. **Store token** in localStorage or state management
3. **Send token** in header for every authenticated request:
   ```
   Authorization: Bearer {token}
   ```
4. **Token expiration**: Token expires in 24 hours (86400000 ms)
5. **Logout**: Remove token on the client side

---

## Example Usage (JavaScript/TypeScript)

### Register
```javascript
const response = await fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123',
    displayName: 'John Doe'
  })
});

const data = await response.json();
localStorage.setItem('token', data.token);
```

### Authenticated Request
```javascript
const token = localStorage.getItem('token');

const response = await fetch('http://localhost:8080/api/trips/mine', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const trips = await response.json();
```

### File Upload
```javascript
const token = localStorage.getItem('token');
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('http://localhost:8080/api/files/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});

const uploadResult = await response.json();
console.log('File URL:', uploadResult.url);
```

---

## Notes

- **Base URL**: Change to production URL when deploying
- **CORS**: CORS must be configured on the backend for the frontend domain
- **File Size**: No limit is set, but should be limited on the client side
- **Rate Limiting**: No rate limiting implemented (should be added in production)
- **Pagination**: List endpoints do not have pagination (should be added if data is large)

---

**Last Updated:** November 28, 2025

