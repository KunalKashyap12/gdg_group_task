package com.cropdetector.backend.service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import com.cropdetector.backend.dto.CropPredictionResponse;
import com.cropdetector.backend.dto.PredictionItem;
import com.cropdetector.backend.dto.TopKItem;
import com.cropdetector.backend.exception.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;
    private final boolean mockFallback;

    public MlServiceClient(
            @Value("${ml.service.url:${app.ml.base-url:https://gdg-grp-task-ml-2.onrender.com}}") String mlServiceUrl,
            @Value("${ml.service.timeout-ms:${app.ml.read-timeout-ms:30000}}") long timeoutMs,
            @Value("${app.ml.mock-fallback:false}") boolean mockFallback) {
        this.mlServiceUrl = mlServiceUrl;
        this.mockFallback = mockFallback;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * Forwards uploaded leaf image to the Python FastAPI ML microservice.
     *
     * @param imageFile The uploaded leaf photo file from user
     * @param cropHint  Optional user-selected crop hint (e.g. "Tomato", "Potato")
     * @return CropPredictionResponse containing top diagnosis, confidence & top-k
     */
    public CropPredictionResponse predictDisease(MultipartFile imageFile, String cropHint) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_IMAGE", "Please upload a leaf image.");
        }

        byte[] bytes = imageFile.getBytes();
        String filename = imageFile.getOriginalFilename();
        String contentType = imageFile.getContentType();

        return predictDisease(bytes, filename, contentType, cropHint);
    }

    /**
     * Forwards binary leaf image bytes to the Python FastAPI ML microservice.
     */
    public CropPredictionResponse predictDisease(byte[] bytes, String filename, String contentType, String cropHint) {
        String base = mlServiceUrl.replaceAll("/+$", "");
        String targetUrl = base.endsWith("/api/v1/predict")
                ? base
                : (base.endsWith("/predict") ? base : base + "/api/v1/predict");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return (filename != null && !filename.isBlank()) ? filename : "leaf.jpg";
            }
        };

        HttpHeaders partHeaders = new HttpHeaders();
        if (contentType != null && !contentType.isBlank()) {
            try {
                partHeaders.setContentType(MediaType.parseMediaType(contentType));
            } catch (Exception ignored) {
                partHeaders.setContentType(MediaType.IMAGE_JPEG);
            }
        } else {
            partHeaders.setContentType(MediaType.IMAGE_JPEG);
        }

        body.add("image", new HttpEntity<>(fileResource, partHeaders));

        if (cropHint != null && !cropHint.isBlank()) {
            body.add("crop", cropHint.trim());
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<CropPredictionResponse> response = restTemplate.postForEntity(
                    targetUrl,
                    requestEntity,
                    CropPredictionResponse.class
            );

            CropPredictionResponse result = response.getBody();
            if (result == null) {
                if (mockFallback) {
                    log.warn("ML Service returned empty body from {}. Falling back to simulated prediction.", targetUrl);
                    return generateMockPrediction(cropHint);
                }
                throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_ML_RESPONSE", "Prediction service returned an empty response.");
            }

            return result;

        } catch (HttpStatusCodeException ex) {
            log.error("ML service returned HTTP {} from {}: {}", ex.getStatusCode(), targetUrl, ex.getResponseBodyAsString());
            if (mockFallback) {
                log.warn("Falling back to simulated prediction due to ML service HTTP error.");
                return generateMockPrediction(cropHint);
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "The leaf image could not be processed by the ML service.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ML_SERVICE_ERROR", "The ML prediction service encountered an error.");

        } catch (ResourceAccessException ex) {
            log.warn("ML Service unreachable or timed out at {}: {}", targetUrl, ex.getMessage());
            if (mockFallback) {
                log.warn("Falling back to simulated prediction because mock-fallback is enabled.");
                return generateMockPrediction(cropHint);
            }
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "ML_SERVICE_UNAVAILABLE", "The prediction service could not be reached in time.");

        } catch (RestClientException ex) {
            log.error("Error communicating with ML service at {}: {}", targetUrl, ex.getMessage());
            if (mockFallback) {
                log.warn("Falling back to simulated prediction because mock-fallback is enabled.");
                return generateMockPrediction(cropHint);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ML_SERVICE_ERROR", "Error communicating with prediction service.");
        }
    }

    private CropPredictionResponse generateMockPrediction(String cropHint) {
        String c = cropHint != null ? cropHint.trim() : "Tomato";
        String cropName = Character.toUpperCase(c.charAt(0)) + c.substring(1).toLowerCase(Locale.ROOT);

        String disease = switch (cropName.toLowerCase(Locale.ROOT)) {
            case "potato" -> "Early_blight";
            case "apple" -> "Apple_scab";
            case "corn", "maize" -> "Common_rust";
            case "pepper" -> "Bacterial_spot";
            default -> "Late_blight";
        };

        String label = cropName + "___" + disease;
        double confidence = 0.94;

        PredictionItem prediction = new PredictionItem();
        prediction.setClassId(1);
        prediction.setCrop(cropName);
        prediction.setDisease(disease);
        prediction.setLabel(label);
        prediction.setConfidence(confidence);

        CropPredictionResponse response = new CropPredictionResponse();
        response.setStatus("success");
        response.setLeaf(true);
        response.setPrediction(prediction);
        response.setTopK(List.of(
                new TopKItem(label, confidence),
                new TopKItem(cropName + "___Healthy", 0.04)
        ));
        response.setLowConfidence(false);
        response.setThreshold(0.70);
        response.setMessage("Stage 1: " + cropName + " leaf identified. Stage 2: " + disease.replace('_', ' ') + " diagnosed.");
        response.setModelVersion("1.0.0-mock");
        response.setInferenceTimeMs(25.0);
        response.setDisclaimer("AI-assisted indication only, not a guaranteed agricultural diagnosis. Consult a local agricultural expert for confirmation.");

        return response;
    }
}
