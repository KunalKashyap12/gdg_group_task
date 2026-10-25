package com.cropdetector.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "crops")
public class Crop {

    @Id
    @Column(length = 50)
    private String id; // e.g. "tomato", "potato", "apple", "corn", "grape"

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Column(length = 500)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "crop_supported_diseases",
        joinColumns = @JoinColumn(name = "crop_id"))
    @Column(name = "disease_name", length = 150)
    private List<String> supportedDiseases = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "crop_capture_tips",
        joinColumns = @JoinColumn(name = "crop_id"))
    @Column(name = "tip", length = 500)
    private List<String> captureTips = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "crop_preventive_tips",
        joinColumns = @JoinColumn(name = "crop_id"))
    @Column(name = "tip", length = 500)
    private List<String> preventiveTips = new ArrayList<>();

    protected Crop() {}

    public Crop(
        String id,
        String name,
        String scientificName,
        String description,
        List<String> supportedDiseases,
        List<String> captureTips,
        List<String> preventiveTips
    ) {
        this.id = id;
        this.name = name;
        this.scientificName = scientificName;
        this.description = description;
        this.supportedDiseases = supportedDiseases != null ? new ArrayList<>(supportedDiseases) : new ArrayList<>();
        this.captureTips = captureTips != null ? new ArrayList<>(captureTips) : new ArrayList<>();
        this.preventiveTips = preventiveTips != null ? new ArrayList<>(preventiveTips) : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getScientificName() { return scientificName; }
    public String getDescription() { return description; }
    public List<String> getSupportedDiseases() { return supportedDiseases; }
    public List<String> getCaptureTips() { return captureTips; }
    public List<String> getPreventiveTips() { return preventiveTips; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public void setDescription(String description) { this.description = description; }
    public void setSupportedDiseases(List<String> supportedDiseases) { this.supportedDiseases = supportedDiseases; }
    public void setCaptureTips(List<String> captureTips) { this.captureTips = captureTips; }
    public void setPreventiveTips(List<String> preventiveTips) { this.preventiveTips = preventiveTips; }
}
