
package com.cropdetector.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.cropdetector.backend.model.Prediction;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {
    Page<Prediction> findByUserId(UUID userId, Pageable pageable);
    Optional<Prediction> findByIdAndUserId(UUID id, UUID userId);
}
