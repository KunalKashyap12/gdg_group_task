

### 1. Automated Access Token (`accessToken`)
When you send:
- `POST /api/v1/auth/register` or
- `POST /api/v1/auth/login`

Postman runs the embedded test script:
```javascript
pm.test("Status code is 200/201", function () {
    pm.response.to.be.success;
});

var jsonData = pm.response.json();
if (jsonData.accessToken) {
    pm.environment.set("accessToken", jsonData.accessToken);
    pm.collectionVariables.set("accessToken", jsonData.accessToken);
    console.log("Captured JWT Bearer Token:", jsonData.accessToken.substring(0, 15) + "...");
}
```
All protected endpoints (history, profile, preferences, deleting records) **inherit** this token automatically from the collection root (`Authorization: Bearer {{accessToken}}`).

### 2. Automated Prediction ID (`predictionId`)
When you submit an image via `POST /api/v1/predictions`, Postman stores the returned UUID in `{{predictionId}}`, allowing you to immediately run `GET /api/v1/predictions/:id` or `DELETE /api/v1/predictions/:id` without manual copy-pasting.

---

## 📑 Complete API Endpoints Catalog

### 1. Authentication (`/api/v1/auth`)

| Request Name | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Register Farmer User** | `POST` | `/api/v1/auth/register` | None | Registers account (`email`, `password`, `fullName`). Auto-saves JWT. |
| **Login User** | `POST` | `/api/v1/auth/login` | None | Authenticates with `email` and `password`. Auto-saves JWT. |
| **Request OTP Code** | `POST` | `/api/v1/auth/otp/send` | None | Sends 6-digit OTP code to email. (Also supports `/send-otp`). |
| **Verify OTP & Login/Register** | `POST` | `/api/v1/auth/otp/verify` | None | Verifies OTP code, auto-registers/logs in, auto-saves JWT. (Also `/verify-otp`). |
| **Google Sign-In Exchange** | `POST` | `/api/v1/auth/google` | None | Exchanges Google ID token for standard JWT. |

#### Sample Request Body: Request OTP Code (`POST /api/v1/auth/otp/send`)
```json
{
  "email": "farmer@example.com",
  "purpose": "LOGIN"
}
```

#### Sample Response (`200 OK`):
```json
{
  "status": "success",
  "message": "Verification code sent successfully to farmer@example.com",
  "email": "farmer@example.com",
  "expiresInSeconds": 300,
  "otp": "849201"
}
```

#### Sample Request Body: Verify OTP (`POST /api/v1/auth/otp/verify`)
```json
{
  "email": "farmer@example.com",
  "otp": "849201",
  "fullName": "Ramesh Patel"
}
```

#### Sample Request Body: Registration
```json
{
  "email": "farmer@example.com",
  "password": "Password123!",
  "fullName": "Ramesh Patel"
}
```

#### Sample Response (`201 Created`):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "userId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "email": "farmer@example.com"
}
```

---

### 2. Leaf Diagnosis & Predictions (`/api/v1/predictions` & `/api/crops`)

| Request Name | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Direct Leaf Diagnosis** | `POST` | `/api/crops/diagnose` | Optional | Forwards leaf photo directly to the live ONNX ML microservice. Returns diagnosis, confidence, and top-k candidate classes. |
| **Predict Disease & Guidance** | `POST` | `/api/v1/predictions` | Optional | Predicts disease, formats non-prescriptive advice, and saves to database if authenticated. |
| **Get Prediction History** | `GET` | `/api/v1/predictions?page=0&size=20` | Bearer | Paginated diagnosis history for the authenticated user. |
| **Get Prediction by ID** | `GET` | `/api/v1/predictions/:id` | Bearer | Full details for a single diagnosis. |
| **Delete Prediction by ID** | `DELETE` | `/api/v1/predictions/:id` | Bearer | Removes a prediction from user history. |

#### Testing File Upload in Postman:
1. Select the request **"Direct Leaf Diagnosis"** or **"Predict Leaf Disease & Save Guidance"**.
2. Click on the **Body** tab.
3. In the key `image`, hover over the field and click **Select Files**.
4. Choose any `.jpg`, `.jpeg`, or `.png` leaf photograph (< 5MB).
5. (Optional) In the key `crop`, provide a hint such as `tomato`, `potato`, `apple`, or `pepper`.
6. Click **Send**.

#### Sample ML Response (`200 OK` from `/api/crops/diagnose`):
```json
{
  "status": "success",
  "is_leaf": true,
  "prediction": {
    "class_id": 13,
    "crop": "Pepper",
    "disease": "Bacterial_spot",
    "label": "Pepper___Bacterial_spot",
    "confidence": 0.9854
  },
  "top_k": [
    {
      "label": "Pepper___Bacterial_spot",
      "confidence": 0.9854
    },
    {
      "label": "Pepper___healthy",
      "confidence": 0.0121
    }
  ],
  "low_confidence": false,
  "threshold": 0.7,
  "message": "Disease detected: Bacterial_spot on Pepper",
  "model_version": "mobilenetv3_large_v1",
  "inference_time_ms": 42.5,
  "disclaimer": "This diagnosis is AI-generated for advisory purposes. Always consult a certified local agricultural extension officer before chemical intervention."
}
```

---

### 3. Crop Catalog & Advisory (`/api/v1/crops`)

| Request Name | Method | Path | Auth | Description |
|---|---|---|---|---|
| **List All Supported Crops** | `GET` | `/api/v1/crops` | None | Returns list of crops (Tomato, Potato, Apple, Corn), capture tips & disease registries. |
| **Get Crop by ID** | `GET` | `/api/v1/crops/:id` | None | Returns details for a single crop (e.g. `tomato`). |

#### Sample Crop Response (`200 OK`):
```json
{
  "id": "tomato",
  "name": "Tomato",
  "scientificName": "Solanum lycopersicum",
  "description": "Widely cultivated warm-season crop susceptible to foliar fungal and bacterial blights.",
  "supportedDiseases": [
    "Bacterial Spot",
    "Early Blight",
    "Late Blight",
    "Leaf Mold",
    "Septoria Leaf Spot",
    "Target Spot",
    "Yellow Leaf Curl Virus",
    "Mosaic Virus",
    "Healthy"
  ],
  "captureTips": [
    "Capture both upper and lower surface of affected leaves.",
    "Ensure lesions are clearly focused with minimal shadows."
  ],
  "preventiveTips": [
    "Avoid overhead irrigation; water at soil level.",
    "Rotate with non-solanaceous crops every 2-3 years."
  ]
}
```

---

### 4. User Profile & Preferences (`/api/v1/users/me`)

| Request Name | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Get Current User Profile** | `GET` | `/api/v1/users/me` | Bearer | Returns email, name, provider, and profile details. |
| **Update Profile Full Name** | `PATCH` | `/api/v1/users/me` | Bearer | Updates user's display name. |
| **Get User Preferences** | `GET` | `/api/v1/users/me/preferences` | Bearer | Fetches language code (`en` / `hi`) and notification toggles. |
| **Update User Preferences** | `PUT` | `/api/v1/users/me/preferences` | Bearer | Updates localization and notification settings. |

---

### 5. Health & Diagnostics

| Request Name | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Backend Service Health** | `GET` | `/api/v1/health` | None | Simple `{ "status": "UP" }` probe. |
| **Spring Actuator Health** | `GET` | `/actuator/health` | None | Spring Boot Actuator liveness/readiness status. |
| **OpenAPI 3.0 Specification** | `GET` | `/api/v1/docs/openapi.json` | None | Raw OpenAPI 3.0 specification JSON. |
| **Direct ML Service Health** | `GET` | `https://gdg-grp-task-ml-2.onrender.com/api/v1/health` | None | Pings the live ONNX microservice on Render. |

---

