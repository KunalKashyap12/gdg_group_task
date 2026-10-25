package com.cropdetector.backend.service;

import java.time.Duration;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cropdetector.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class MlClient {

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final RestClient client;
    private final String predictPath;
    private final boolean mockFallback;

    public MlClient(
        @Value("${app.ml.base-url}") String baseUrl,
        @Value("${app.ml.predict-path}") String predictPath,
        @Value("${app.ml.connect-timeout-ms}") int connectTimeout,
        @Value("${app.ml.read-timeout-ms}") int readTimeout,
        @Value("${app.ml.mock-fallback:false}") boolean mockFallback
    ) {
        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
        this.predictPath = predictPath;
        this.mockFallback = mockFallback;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MlResponse(
        String predictedClass,
        double confidence,
        String modelVersion
    ) {}

    public MlResponse predict(
        ImageValidationService.ValidatedImage image, String crop
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.parseMediaType(image.contentType()));

        ByteArrayResource imageResource = new ByteArrayResource(image.bytes()) {
            @Override
            public String getFilename() {
                return "image/png".equalsIgnoreCase(image.contentType())
                    ? "leaf.png" : "leaf.jpg";
            }
        };

        body.add("image", new HttpEntity<>(imageResource, imageHeaders));

        if (crop != null && !crop.isBlank()) {
            body.add("crop", crop.trim());
        }

        try {
            MlResponse result = client.post()
                .uri(predictPath)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(MlResponse.class);

            if (result == null || result.predictedClass() == null ||
                result.predictedClass().isBlank() ||
                !Double.isFinite(result.confidence()) ||
                result.confidence() < 0 || result.confidence() > 1) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "INVALID_ML_RESPONSE",
                    "The prediction service returned an invalid response.");
            }

            return result;

        } catch (ApiException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            if (mockFallback) {
                log.warn("ML Service unavailable at {}. Falling back to simulated prediction because mock-fallback is enabled.",
                    predictPath);
                return generateMockPrediction(crop);
            }
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "ML_SERVICE_UNAVAILABLE",
                "The prediction service could not be reached in time.");
        } catch (RestClientException ex) {
            if (mockFallback) {
                log.warn("ML Service error at {}. Falling back to simulated prediction because mock-fallback is enabled.",
                    predictPath);
                return generateMockPrediction(crop);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                "ML_SERVICE_ERROR",
                "The prediction service could not process this image.");
        }
    }

    private MlResponse generateMockPrediction(String crop) {
        String c = crop != null ? crop.trim().toLowerCase(Locale.ROOT) : "tomato";
        return switch (c) {
            case "potato" -> new MlResponse("Potato___Early_blight", 0.88, "1.0.0-mock");
            case "apple" -> new MlResponse("Apple___Apple_scab", 0.92, "1.0.0-mock");
            case "corn", "maize" -> new MlResponse("Corn___Common_rust", 0.85, "1.0.0-mock");
            default -> new MlResponse("Tomato___Early_blight", 0.91, "1.0.0-mock");
        };
    }
}
