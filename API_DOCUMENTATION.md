# 🌾 Local Crop Disease Detector - API Documentation

This document defines the complete RESTful API contract for the **Crop Disease Detector Backend**. It is designed for frontend web developers, mobile application developers, and QA engineers.

---

## 📌 Base URL & Environment

- **Local Development:** `http://localhost:8080/api/v1`
- **Interactive Web Docs:** `http://localhost:8080/api/v1/docs`
- **Postman Collection (v2.1):** [`postman/Crop_Disease_Detector_API.postman_collection.json`](file:///f:/gdg/crop/postman/Crop_Disease_Detector_API.postman_collection.json) or live at `http://localhost:8080/api/v1/docs/postman.json`
- **Postman Documentation & Guide:** [POSTMAN_DOCUMENTATION.md](file:///f:/gdg/crop/POSTMAN_DOCUMENTATION.md)
- **OpenAPI 3.0 Specification:** `http://localhost:8080/api/v1/docs/openapi.json`
- **Health Check:** `http://localhost:8080/actuator/health`

---

## 🔐 Authentication & Headers

Protected endpoints require a standard JSON Web Token (JWT) in the `Authorization` header:

```http
Authorization: Bearer <access_token>
```

Tokens are valid for **1 hour** by default and can be obtained via `/api/v1/auth/login` or `/api/v1/auth/register`.

---

## 📋 Endpoints Overview

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/predictions` | **Public / Auth** | Upload leaf image & get disease prediction with guidance |
| `GET` | `/api/v1/predictions` | **Authenticated** | Get paginated prediction history for current user |
| `GET` | `/api/v1/predictions/{id}` | **Authenticated** | Get specific prediction details by UUID |
| `DELETE` | `/api/v1/predictions/{id}` | **Authenticated** | Delete a prediction record |
| `GET` | `/api/v1/crops` | **Public** | List all supported crops and their disease catalogs |
| `GET` | `/api/v1/crops/{cropId}` | **Public** | Get details, capture tips & guidance for a specific crop |
| `POST` | `/api/v1/auth/register` | **Public** | Register new farmer account |
| `POST` | `/api/v1/auth/login` | **Public** | Authenticate existing user |
| `POST` | `/api/v1/auth/google` | **Public** | Authenticate or register via Google ID token (SPA / Mobile) |
| `GET` | `/api/v1/users/me` | **Authenticated** | Get authenticated user profile |
| `PATCH` | `/api/v1/users/me` | **Authenticated** | Update user profile full name |
| `GET` | `/api/v1/users/me/preferences` | **Authenticated** | Get language & notification preferences |
| `PUT` | `/api/v1/users/me/preferences` | **Authenticated** | Update user preferences (`en` or `hi`) |
| `GET` | `/api/v1/docs` | **Public** | Interactive HTML documentation dashboard |
| `GET` | `/api/v1/docs/openapi.json` | **Public** | Machine-readable OpenAPI 3.0 JSON schema |

---

## 📷 1. Image Prediction API

### `POST /api/v1/predictions`
Submit a leaf image for disease diagnosis and preventive guidance.

> **Farmer-friendly design:** Can be called without authentication (guest mode) or with a Bearer token (automatically saves prediction to farmer's account history).

#### Request Details
- **Content-Type:** `multipart/form-data`
- **Parts:**
  - `image` *(required, binary file)*: Leaf image in JPEG (`.jpg`, `.jpeg`) or PNG (`.png`) format. Maximum **5 MB**. Dimensions must not exceed **6000x6000px** or **20 Megapixels**.
  - `crop` *(optional, string)*: Crop identifier (e.g., `tomato`, `potato`, `apple`, `corn`). Improves classification accuracy when known.

#### Example Request (`curl`)
```bash
curl -X POST http://localhost:8080/api/v1/predictions \
  -H "Authorization: Bearer <token>" \
  -F "image=@/path/to/leaf.jpg" \
  -F "crop=tomato"
```

#### Success Response (`201 Created`) - High Confidence
```json
{
  "predictionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "crop": "tomato",
  "predictedDisease": "Tomato Early Blight (Alternaria solani)",
  "confidence": 0.942,
  "status": "COMPLETED",
  "modelVersion": "1.0.0",
  "guidance": [
    "Inspect plants regularly and monitor changes in leaf appearance.",
    "Avoid unnecessary wetting of leaves when watering.",
    "Handle affected plant material appropriately.",
    "Seek confirmation from a local agricultural expert if symptoms persist."
  ],
  "expertConfirmationRecommended": true,
  "createdAt": "2026-10-25T14:30:00Z"
}
```

#### Success Response (`201 Created`) - Low Confidence Guardrail
When the ML confidence is under threshold (`< 0.70`), the backend automatically marks the result as `UNCERTAIN` and prompts for expert inspection and better photo capture:
```json
{
  "predictionId": "c8d0a2f1-6789-4b12-98ab-3456789abcde",
  "crop": "tomato",
  "predictedDisease": "Tomato Early Blight (Alternaria solani)",
  "confidence": 0.54,
  "status": "UNCERTAIN",
  "modelVersion": "1.0.0",
  "guidance": [
    "The result is uncertain. Try a clearer leaf image.",
    "Ask a local agricultural expert to confirm the symptoms."
  ],
  "expertConfirmationRecommended": true,
  "createdAt": "2026-10-25T14:31:00Z"
}
```

---

## 📜 2. Prediction History APIs

### `GET /api/v1/predictions`
Retrieves paginated history for the authenticated user.

#### Query Parameters
- `page` *(optional, default: `0`)*: Page index (0-indexed).
- `size` *(optional, default: `20`, max: `50`)*: Items per page.

#### Example Response (`200 OK`)
```json
{
  "content": [
    {
      "predictionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "crop": "tomato",
      "predictedDisease": "Tomato Early Blight (Alternaria solani)",
      "confidence": 0.94,
      "status": "COMPLETED",
      "modelVersion": "1.0.0",
      "guidance": ["Inspect plants regularly..."],
      "expertConfirmationRecommended": true,
      "createdAt": "2026-10-25T14:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### `GET /api/v1/predictions/{id}`
Returns details for a single prediction owned by the authenticated user.

### `DELETE /api/v1/predictions/{id}`
Deletes a prediction record owned by the authenticated user (`204 No Content`).

---

## 🌿 3. Crop Catalog & Disease Guidance

### `GET /api/v1/crops`
List all crops supported by the detector, along with their common diseases and photography tips.

#### Example Response (`200 OK`)
```json
[
  {
    "id": "tomato",
    "name": "Tomato",
    "scientificName": "Solanum lycopersicum",
    "description": "Widely cultivated vegetable susceptible to foliar diseases.",
    "supportedDiseases": [
      "Tomato Early Blight (Alternaria solani)",
      "Tomato Late Blight (Phytophthora infestans)",
      "Tomato Bacterial Spot (Xanthomonas spp.)",
      "Tomato Leaf Mold (Passalora fulva)",
      "Tomato Septoria Leaf Spot (Septoria lycopersici)",
      "Tomato Yellow Leaf Curl Virus",
      "Tomato Mosaic Virus",
      "Tomato Healthy"
    ],
    "captureTips": [
      "Capture images in indirect natural sunlight to avoid glare and harsh shadows.",
      "Frame a single leaf flat against a neutral background (hand or soil).",
      "Ensure focus is sharp on the affected spots or lesions, not blurred.",
      "Include both symptomatic areas and some green leaf tissue."
    ],
    "preventiveTips": [
      "Avoid overhead irrigation to keep foliage dry.",
      "Ensure adequate plant spacing for proper airflow.",
      "Prune lower leaves that contact moist soil.",
      "Sanitize pruning shears between plants.",
      "Consult local extension services before applying chemical treatments."
    ]
  }
]
```

### `GET /api/v1/crops/{cropId}`
Fetch a specific crop's metadata and guidelines (e.g., `/api/v1/crops/potato`, `/api/v1/crops/apple`, `/api/v1/crops/corn`).

---

## 👤 4. Authentication & Profile APIs

### `POST /api/v1/auth/register`
#### Request Body
```json
{
  "email": "farmer@example.com",
  "password": "SecurePassword123!",
  "fullName": "Kisan Patel"
}
```
#### Response (`201 Created`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "userId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "email": "farmer@example.com"
}
```

### `POST /api/v1/auth/login`
#### Request Body
```json
{
  "email": "farmer@example.com",
  "password": "SecurePassword123!"
}
```
#### Response (`200 OK`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "userId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "email": "farmer@example.com"
}
```

### `POST /api/v1/auth/google`
Authenticate or register a user using a Google ID token. Ideal for Single Page Applications (React, Vue), mobile apps (Flutter, Android), or Google One Tap / Google Identity Services (GIS).

- **Strict Server Verification:** The backend strictly verifies the token against Google's token verification infrastructure, confirms that `aud` matches the configured server `GOOGLE_CLIENT_ID`, and validates that the farmer's Google email address is verified.
- **Auto-Provisioning & Account Linking:** If the farmer does not have an account, one is automatically created with language preferences and `authProvider: "GOOGLE"`. If an account already exists with the same email, it is linked and profile picture updated.

#### Request Body
Accepts either `idToken` or `credential` (from Google Identity Services):
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```
*or*
```json
{
  "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

#### Response (`200 OK`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "userId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "email": "farmer@example.com"
}
```

#### Error Codes
- `400 INVALID_INPUT`: ID token is missing or empty.
- `401 INVALID_GOOGLE_TOKEN`: Expired, malformed, or fake Google token.
- `401 INVALID_GOOGLE_AUDIENCE`: Token `aud` does not match the configured `GOOGLE_CLIENT_ID`.
- `401 UNVERIFIED_GOOGLE_EMAIL`: The Google account email has not been verified by Google.
- `500 GOOGLE_AUTH_NOT_CONFIGURED`: Server operator has not configured `GOOGLE_CLIENT_ID`.
- `502 GOOGLE_SERVICE_ERROR`: Failed to reach Google verification endpoint.

#### Frontend Client Integration Example (HTML / JS GIS)
```javascript
// Using Google Identity Services (GIS)
google.accounts.id.initialize({
  client_id: "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com",
  callback: async (response) => {
    const res = await fetch("http://localhost:8080/api/v1/auth/google", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential: response.credential })
    });
    const authData = await res.json();
    localStorage.setItem("token", authData.accessToken);
  }
});
```

### `GET /api/v1/users/me`
Fetches user ID, email, full name, auth provider (`LOCAL` or `GOOGLE`), and profile picture URL.

### `PATCH /api/v1/users/me`
Updates user profile name: `{"fullName": "New Name"}`.

### `GET /api/v1/users/me/preferences`
Fetches farmer app preferences:
```json
{
  "language": "en",
  "notificationsEnabled": true,
  "emailUpdatesEnabled": false
}
```

### `PUT /api/v1/users/me/preferences`
Updates language (`en` or `hi`) and notifications:
```json
{
  "language": "hi",
  "notificationsEnabled": true,
  "emailUpdatesEnabled": true
}
```

---

## ⚠️ Error Responses & Error Codes

All errors follow a unified JSON envelope:
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable explanation for the user or developer.",
  "timestamp": "2026-10-25T14:32:00Z"
}
```

### Common Error Codes

| Status | Code | Meaning |
|---|---|---|
| `400` | `EMPTY_IMAGE` | No file or an empty zero-byte file was uploaded |
| `400` | `INVALID_IMAGE` | File is not a valid JPEG or PNG image (magic byte validation failed) |
| `400` | `IMAGE_DIMENSIONS_INVALID` | Width/height exceeds 6000px or 20 megapixels |
| `400` | `VALIDATION_ERROR` | Request body or parameters failed bean validation constraints |
| `400` | `INVALID_LANGUAGE` | Unsupported preference language (supported: `en`, `hi`) |
| `400` | `INVALID_ID` | Malformed UUID passed in path |
| `401` | `UNAUTHORIZED` | Missing, expired, or invalid JWT Bearer token |
| `401` | `INVALID_CREDENTIALS` | Incorrect email or password during login |
| `403` | `FORBIDDEN` | Access denied |
| `404` | `PREDICTION_NOT_FOUND` | Prediction not found or not owned by requesting user |
| `404` | `CROP_NOT_FOUND` | Crop identifier is not in the supported catalog |
| `409` | `EMAIL_EXISTS` | An account with this email is already registered |
| `413` | `FILE_TOO_LARGE` | Image size exceeds the 5 MB limit |
| `502` | `ML_SERVICE_ERROR` | Downstream Python ML prediction service returned an error |
| `502` | `INVALID_ML_RESPONSE` | ML service returned invalid JSON or out-of-range confidence |
| `504` | `ML_SERVICE_UNAVAILABLE` | ML prediction service timed out or could not be reached |
