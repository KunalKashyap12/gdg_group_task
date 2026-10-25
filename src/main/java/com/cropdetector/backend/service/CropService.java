package com.cropdetector.backend.service;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.Crop;
import com.cropdetector.backend.repository.CropRepository;
import org.springframework.http.HttpStatus;

@Service
public class CropService {

    private final CropRepository cropRepository;

    public CropService(CropRepository cropRepository) {
        this.cropRepository = cropRepository;
    }

    @PostConstruct
    @Transactional
    public void seedInitialCrops() {
        if (cropRepository.count() > 0) {
            return;
        }

        Crop tomato = new Crop(
            "tomato",
            "Tomato",
            "Solanum lycopersicum",
            "One of the most widely cultivated vegetables susceptible to fungal, bacterial, and viral foliar diseases.",
            List.of(
                "Tomato Early Blight (Alternaria solani)",
                "Tomato Late Blight (Phytophthora infestans)",
                "Tomato Bacterial Spot (Xanthomonas spp.)",
                "Tomato Leaf Mold (Passalora fulva)",
                "Tomato Septoria Leaf Spot (Septoria lycopersici)",
                "Tomato Yellow Leaf Curl Virus",
                "Tomato Mosaic Virus",
                "Tomato Healthy"
            ),
            List.of(
                "Capture images in indirect natural sunlight to avoid glare and harsh shadows.",
                "Frame a single leaf flat against a neutral background (hand or soil).",
                "Ensure focus is sharp on the affected spots or lesions, not blurred.",
                "Include both symptomatic areas and some green leaf tissue."
            ),
            List.of(
                "Avoid overhead irrigation to keep foliage dry.",
                "Ensure adequate plant spacing for proper airflow.",
                "Prune lower leaves that contact moist soil.",
                "Sanitize pruning shears between plants.",
                "Consult local extension services before applying chemical treatments."
            )
        );

        Crop potato = new Crop(
            "potato",
            "Potato",
            "Solanum tuberosum",
            "Major staple tuber crop heavily impacted by moisture-borne blights.",
            List.of(
                "Potato Early Blight (Alternaria solani)",
                "Potato Late Blight (Phytophthora infestans)",
                "Potato Healthy"
            ),
            List.of(
                "Photograph both upper and lower leaf surfaces if water-soaked lesions appear.",
                "Avoid capturing heavily wilted or decomposing leaves.",
                "Keep camera 15-25 cm away from the leaf surface."
            ),
            List.of(
                "Use certified disease-free seed tubers.",
                "Hill soil properly to protect developing tubers from spore wash.",
                "Destroy volunteer potato plants and cull piles.",
                "Seek expert agronomist confirmation for suspected late blight."
            )
        );

        Crop apple = new Crop(
            "apple",
            "Apple",
            "Malus domestica",
            "Deciduous fruit tree subject to scab, cedar rust, and rot pathogens.",
            List.of(
                "Apple Scab (Venturia inaequalis)",
                "Apple Black Rot (Diplodia seriata)",
                "Apple Cedar Rust (Gymnosporangium juniperi-virginianae)",
                "Apple Healthy"
            ),
            List.of(
                "Photograph leaves on the tree branch under diffused daylight.",
                "Focus on olive-green or velvety spots on upper leaf surfaces.",
                "Avoid backlighting from direct sun through the canopy."
            ),
            List.of(
                "Rake and destroy fallen leaves in autumn to reduce overwintering spores.",
                "Prune tree canopy during dormancy to improve light penetration and airflow.",
                "Select disease-resistant apple cultivars when establishing new orchards."
            )
        );

        Crop corn = new Crop(
            "corn",
            "Corn (Maize)",
            "Zea mays",
            "High-yield grain vulnerable to foliar fungal blights and rusts.",
            List.of(
                "Corn Common Rust (Puccinia sorghi)",
                "Corn Northern Leaf Blight (Exserohilum turcicum)",
                "Corn Cercospora Leaf Spot (Gray Leaf Spot)",
                "Corn Healthy"
            ),
            List.of(
                "Capture long leaf blades along their length with stable camera support.",
                "Ensure pustules or rectangular lesions are sharply in focus.",
                "Photograph during early morning or overcast conditions to avoid harsh reflections."
            ),
            List.of(
                "Implement 2 to 3-year crop rotation with non-host crops like legumes.",
                "Manage crop residue through appropriate tillage where soil conservation allows.",
                "Plant hybrid varieties with genetic resistance to local fungal races."
            )
        );

        cropRepository.saveAll(List.of(tomato, potato, apple, corn));
    }

    @Transactional(readOnly = true)
    public List<Crop> getAllCrops() {
        return cropRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Crop getCropById(String id) {
        return cropRepository.findById(id.toLowerCase())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "CROP_NOT_FOUND", "Supported crop not found: " + id));
    }
}
