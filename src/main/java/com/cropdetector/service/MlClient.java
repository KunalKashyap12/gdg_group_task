
package com.cropdetector.backend.service;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cropdetector.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.MultipartBodyBuilder;

@Service
public class MlClient {

    private final RestClient client;
    private final String predictPath;

    public MlClient(
        @Value("${app.ml.base-url}") String baseUrl,
        @Value("${app.ml.predict-path}") String predictPath,
        @Value("${app.ml.connect-timeout-ms}") int connectTimeout,
        @Value("${app.ml.read-timeout-ms}") int readTimeout
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
        MultipartBodyBuilder body = new MultipartBodyBuilder();

        body.part("image", new ByteArrayResource(image.bytes()) {
            @Override
            public String getFilename() {
                return image.contentType().equals("image/png")
                    ? "leaf.png" : "leaf.jpg";
            }
        }).contentType(MediaType.parseMediaType(image.contentType()));

        if (crop != null && !crop.isBlank()) {
            body.part("crop", crop.trim());
        }

        try {
            MlResponse result = client.post()
                .uri(predictPath)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
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
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "ML_SERVICE_UNAVAILABLE",
                "The prediction service could not be reached in time.");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                "ML_SERVICE_ERROR",
                "The prediction service could not process this image.");
        }
    }
}
