# 🔬 Crop Disease Detector - Research, Field Condition Analysis & ML Service Contract

This document provides the foundational agricultural research, computer vision considerations for field conditions, ethical guidance framework, and the technical specification between the **Spring Boot Backend** and the **Python ML Microservice**.

---

## 1. Agricultural Research & Supported Crops

Foliar diseases produce distinct visual symptoms (lesions, necrotic blotches, chlorosis, pustules, mosaic patterns) that can realistically be identified from high-resolution digital leaf images. Based on the widely benchmarked **PlantVillage** dataset and field validation studies:

### Selected Crops & Realistic Disease Targets

| Crop | Scientific Name | Common Realistically Detectable Foliar Diseases | Visual Identifiers on Leaf |
|---|---|---|---|
| **Tomato** | *Solanum lycopersicum* | Early Blight (*Alternaria solani*)<br>Late Blight (*Phytophthora infestans*)<br>Bacterial Spot (*Xanthomonas*)<br>Leaf Mold (*Passalora fulva*)<br>Septoria Leaf Spot<br>Yellow Leaf Curl Virus | Concentric "target-board" rings, water-soaked dark lesions, yellow halos, leaf curling/puckering |
| **Potato** | *Solanum tuberosum* | Early Blight (*Alternaria solani*)<br>Late Blight (*Phytophthora infestans*) | Brown angular spots, rapid foliage necrosis, white fungal sporulation on leaf underside in humid conditions |
| **Apple** | *Malus domestica* | Apple Scab (*Venturia inaequalis*)<br>Black Rot (*Diplodia seriata*)<br>Cedar Apple Rust | Dull olive-green to black velvety spots, reddish-orange pustules with yellow borders |
| **Corn (Maize)**| *Zea mays* | Common Rust (*Puccinia sorghi*)<br>Northern Leaf Blight (*Exserohilum turcicum*)<br>Gray Leaf Spot | Golden-brown to cinnamon raised pustules, long rectangular grayish lesions restricted by veins |

---

## 2. Field Condition Challenges & Image Quality Mitigations

Real-world agricultural photographs taken by farmers in open fields differ drastically from sterile laboratory images. The backend and ML pipeline mitigate these variables:

### Key Field Variables & Technical Mitigations

1. **Direct Sunlight & Specular Glare:**
   - *Problem:* Sun reflections on waxy cuticles wash out color and obscure lesion texture.
   - *Guidance:* Recommend diffused daylight (early morning, late afternoon, or slight shading using hand/body).
2. **Harsh Shadows & High Contrast:**
   - *Problem:* Foliage casting deep dark shadows mimics necrotic lesions.
   - *Guidance:* Flatten the single target leaf against a neutral, steady background.
3. **Motion Blur & Handheld Camera Shake:**
   - *Problem:* Small diagnostic pustules or fungal spores become smudged.
   - *Backend check:* `ImageValidationService` verifies minimum pixel clarity and valid image rendering.
4. **Complex Background Clutter (Soil, Weeds, Feet):**
   - *Problem:* Background clutter causes CNN activation attention to drift away from the plant leaf.
   - *Mitigation:* Advise farmers to frame a single leaf filling 60-80% of the viewport.
5. **Decompression Bombs & Resource Exhaustion:**
   - *Backend Guardrail:* `ImageValidationService` enforces a strict 5 MB file size limit, 6000x6000px dimension ceiling, and 20 MP limit to prevent memory exhaustion and DoS attacks.

---

## 3. Preventive Guidance Framework & Guardrails

### Core Ethical Principle: Triage & Decision-Support, Not Final Diagnosis

1. **AI as a Triage Tool:** The platform explicitly presents predictions as *probabilities*, never as confirmed clinical diagnoses.
2. **Uncertain Prediction Guardrail (`app.prediction.minimum-confidence: 0.70`):**
   - If confidence $\ge 0.70$, the backend assigns status `COMPLETED`.
   - If confidence $< 0.70$, the backend automatically assigns status `UNCERTAIN` and returns specific prompts recommending clearer photographs under different lighting, and urges consulting an agricultural extension officer.
3. **Always-On Safe Guidance:**
   - The backend includes general, non-chemical, preventive cultural practices (proper plant spacing, drip irrigation to prevent wet canopy, crop rotation, sanitizing pruning shears).
   - High-risk chemical pesticide recommendations are intentionally omitted to avoid dangerous misuse or chemical burns.

---

## 4. ML Service Technical Contract

The Backend communicates with the Python ML Service over HTTP/REST using standard `multipart/form-data`.

### 1. Endpoint Specification

- **Method:** `POST`
- **Path:** `/predict` (configurable via `app.ml.predict-path`)
- **Protocol:** HTTP/1.1
- **Timeout:** 3,000 ms connect timeout, 15,000 ms read timeout

### 2. Request Contract (Backend ➔ ML Service)

```http
POST /predict HTTP/1.1
Host: localhost:8000
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="image"; filename="leaf.jpg"
Content-Type: image/jpeg

<binary image bytes>
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="crop"

tomato
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```

### 3. Response Contract (ML Service ➔ Backend)

The Python service (e.g. FastAPI/PyTorch) must respond with HTTP `200 OK` and the following JSON payload:

```json
{
  "predictedClass": "Tomato___Early_blight",
  "confidence": 0.942,
  "modelVersion": "1.0.0"
}
```

#### Field Specifications:
- `predictedClass` *(string, required)*: Standard disease class name formatted as `{Crop}___{Disease}` or `{Crop}___healthy`.
- `confidence` *(float, required)*: Softmax probability between `0.0` and `1.0`.
- `modelVersion` *(string, optional)*: Version tag of the active model weights (e.g., `"1.0.0"` or `"mobilenet_v3_plantvillage_v2"`).

### 4. Error Handling & Resilience Matrix

| Scenario | ML Client Behavior | Backend Response to Client |
|---|---|---|
| **Python Service Down (Connect Timeout / Conn Refused)** | Catches `ResourceAccessException` | If `app.ml.mock-fallback=true`, serves simulated prediction with warning log. If false, returns `504 Gateway Timeout` (`ML_SERVICE_UNAVAILABLE`). |
| **Python Service Crashing (500 Internal / 422 Unprocessable)** | Catches `RestClientException` | Returns `502 Bad Gateway` (`ML_SERVICE_ERROR`). |
| **Invalid Response (Missing fields, confidence > 1.0 or NaN)** | Validates response fields | Returns `502 Bad Gateway` (`INVALID_ML_RESPONSE`). |
