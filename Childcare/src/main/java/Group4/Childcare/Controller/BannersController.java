package Group4.Childcare.Controller;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Service.BannersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/banners")
public class BannersController {
    private final BannersService service;

    private final Path storageLocation;

    @Autowired
    public BannersController(BannersService service) throws IOException {
        this.service = service;
        // 將 BannerResource 資料夾設在專案根目錄
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "BannerResource");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                throw new IOException("Unable to create storage directory: " + dir.getAbsolutePath());
            }
        }
        storageLocation = dir.toPath();
    }

    // Create with JSON body (no file)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Banners entity) {
        try {
            Banners created = service.create(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create banner: " + e.getMessage());
        }
    }

    // Create with multipart file + metadata
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(@RequestPart(value = "file") MultipartFile file,
                                    @RequestPart(value = "meta", required = false) Banners meta) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        if (meta == null || meta.getSortOrder() <= 0 || meta.getStartTime() == null || meta.getEndTime() == null || meta.getStatus() == null) {
            return ResponseEntity.badRequest().body("meta 必須包含 sortOrder, startTime, endTime, status 欄位且不可為 null");
        }
        String originalFilename = file.getOriginalFilename();
        String original = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "file";
        // 使用 UUID_原始檔名，保留前端檔名以利識別
        String filename = UUID.randomUUID() + "_" + original;
        try {
            Path target = storageLocation.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            meta.setImageName(filename);
            Banners created = service.create(meta);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store file: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Banners> getById(@PathVariable Integer id) {
        Optional<Banners> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Banners> getAll() {
        return service.getAll();
    }

    @GetMapping("/offset")
    public ResponseEntity<Map<String, Object>> getBannersByOffsetJdbc(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size) {
        List<Banners> banners = service.getBannersWithOffsetJdbc(offset, size);
        long totalCount = service.getTotalCount();

        Map<String, Object> response = new HashMap<>();
        response.put("content", banners);
        response.put("offset", offset);
        response.put("size", size);
        response.put("totalElements", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));
        response.put("hasNext", offset + size < totalCount);

        return ResponseEntity.ok(response);
    }

    // Update metadata optionally with a new file (multipart)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateWithFile(@PathVariable Integer id,
                                            @RequestPart(value = "file", required = false) MultipartFile file,
                                            @RequestPart(value = "meta", required = false) Banners meta) {
        Optional<Banners> existing = service.getById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Banners banner = existing.get();

        if (meta != null) {
            banner.setStartTime(meta.getStartTime());
            banner.setEndTime(meta.getEndTime());
            banner.setLinkUrl(meta.getLinkUrl());
            banner.setStatus(meta.getStatus());
            if (meta.getImageName() != null && !meta.getImageName().isEmpty()) {
                banner.setImageName(meta.getImageName());
            }
        }

        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String original2 = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "file";
            // 使用 UUID_原始檔名 命名規則
            String filename = UUID.randomUUID() + "_" + original2;
            try {
                Path target = storageLocation.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                // delete old file if exists
                if (banner.getImageName() != null) {
                    try { Files.deleteIfExists(storageLocation.resolve(banner.getImageName())); } catch (IOException ignored) {}
                }
                banner.setImageName(filename);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store file: " + e.getMessage());
            }
        }

        try {
            Banners updated = service.update(id, banner);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update banner: " + e.getMessage());
        }
    }

    // Update metadata with JSON only (no file upload)
    @PutMapping(value = "/{id}", consumes = {"application/json"})
    public ResponseEntity<?> updateJson(@PathVariable Integer id, @RequestBody Banners meta) {
        Optional<Banners> existing = service.getById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Banners banner = existing.get();
        if (meta != null) {
            if (meta.getStartTime() != null) banner.setStartTime(meta.getStartTime());
            if (meta.getEndTime() != null) banner.setEndTime(meta.getEndTime());
            banner.setLinkUrl(meta.getLinkUrl());
            if (meta.getStatus() != null) banner.setStatus(meta.getStatus());
            // 若前端也想直接指定已存在的 imageName，則可一併更新
            if (meta.getImageName() != null && !meta.getImageName().isEmpty()) {
                banner.setImageName(meta.getImageName());
            }
        }

        try {
            Banners updated = service.update(id, banner);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update banner: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        Optional<Banners> existing = service.getById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Banners banner = existing.get();
        // delete file
        if (banner.getImageName() != null) {
            try { Files.deleteIfExists(storageLocation.resolve(banner.getImageName())); } catch (IOException ignored) {}
        }

        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/image/{imageName}")
    public ResponseEntity<?> getImage(@PathVariable String imageName) {
        Path filePath = storageLocation.resolve(imageName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            // 從 UUID_原始檔名 提取原始檔名顯示
            String displayFilename = imageName;
            int underscoreIdx = imageName.indexOf('_');
            if (underscoreIdx > 0 && underscoreIdx < imageName.length() - 1) {
                displayFilename = imageName.substring(underscoreIdx + 1);
            }
            return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + displayFilename + "\"")
                .contentType(contentType != null ? org.springframework.http.MediaType.parseMediaType(contentType) : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(Files.readAllBytes(filePath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read image: " + e.getMessage());
        }
    }

    // 取得上架且未過期的 banners
    @GetMapping("/active")
    public List<Banners> getActiveBanners() {
        return service.findActiveBanners();
    }
}
