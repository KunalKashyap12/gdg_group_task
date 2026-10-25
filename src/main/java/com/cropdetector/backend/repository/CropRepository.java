package com.cropdetector.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cropdetector.backend.model.Crop;

public interface CropRepository extends JpaRepository<Crop, String> {
}
