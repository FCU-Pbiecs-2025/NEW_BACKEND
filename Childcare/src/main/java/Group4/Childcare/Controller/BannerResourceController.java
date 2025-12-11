package Group4.Childcare.Controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class BannerResourceController {

    private Path getStorageLocation() {
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");
        return dir.toPath();
    }

    @GetMapping({"/BannerResource/{imageName}", "/api/BannerResource/{imageName}"})
    public ResponseEntity<?> getBannerImage(@PathVariable String imageName) {
        Path filePath = getStorageLocation().resolve(imageName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(Files.readAllBytes(filePath));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read image: " + e.getMessage());
        }
    }
}

