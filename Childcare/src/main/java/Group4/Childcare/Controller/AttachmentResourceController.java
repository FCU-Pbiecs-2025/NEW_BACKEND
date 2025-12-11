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
public class AttachmentResourceController {

    private Path getStorageLocation() {
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "AttachmentResource");
        return dir.toPath();
    }

    @GetMapping({"/AttachmentResource/{fileName}", "/api/AttachmentResource/{fileName}"})
    public ResponseEntity<?> getFile(@PathVariable String fileName) {
        Path filePath = getStorageLocation().resolve(fileName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            // 從 UUID_原始檔名 格式中提取原始檔名
            String displayFilename = fileName;
            int underscoreIdx = fileName.indexOf('_');
            if (underscoreIdx > 0 && underscoreIdx < fileName.length() - 1) {
                displayFilename = fileName.substring(underscoreIdx + 1);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayFilename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(Files.readAllBytes(filePath));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to read file: " + e.getMessage());
        }
    }
}

