package Group4.Childcare.Controller;

import Group4.Childcare.Model.Announcements;
import Group4.Childcare.DTO.AnnouncementSummaryDTO;
import Group4.Childcare.Service.AnnouncementsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@RequestMapping("/announcements")
public class AnnouncementsController {
    private final AnnouncementsService service;

    private final Path storageLocation;

    @Autowired
    public AnnouncementsController(AnnouncementsService service) throws IOException {
        this.service = service;
        // 使用與 banner 相同的存放資料夾 BannerResource（專案根目錄）
        String basePath = System.getProperty("user.dir");
        // 儲存附件到 AttachmentResource 資料夾
        File dir = new File(basePath, "AttachmentResource");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                throw new IOException("Unable to create storage directory: " + dir.getAbsolutePath());
            }
        }
        storageLocation = dir.toPath();
    }

    //@PostMapping
    //public ResponseEntity<Announcements> create(@RequestBody Announcements entity) {

        //return ResponseEntity.ok(service.create(entity));
    //}

    @GetMapping("/{id}")
    public ResponseEntity<Announcements> getById(@PathVariable UUID id) {
        Optional<Announcements> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Announcements>> getAll() {
        List<Announcements> announcements = service.getAll();
        return ResponseEntity.ok(announcements);
    }

    @GetMapping("/offset")
    public ResponseEntity<Map<String, Object>> getAnnouncementsByOffsetJdbc(@RequestParam(defaultValue = "0") int offset) {
        List<Announcements> announcements = service.getAnnouncementsWithOffsetJdbc(offset);
        long totalCount = service.getTotalCount();

        Map<String, Object> response = new HashMap<>();
        response.put("content", announcements);
        response.put("offset", offset);
        response.put("size", 8);
        response.put("totalElements", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / 8));
        response.put("hasNext", offset + 8 < totalCount);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Announcements> update(@PathVariable UUID id, @RequestBody Announcements entity) {
        return ResponseEntity.ok(service.update(id, entity));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<AnnouncementSummaryDTO>> getSummary() {
        List<AnnouncementSummaryDTO> summaries = service.getSummaryAll();
        return ResponseEntity.ok(summaries);
    }

    // 新增：後台專用 - 取得 active announcements summaries（Type=2, Status=1, EndDate >= today）
    @GetMapping("/active/backend")
    public ResponseEntity<List<AnnouncementSummaryDTO>> getAdminActiveBackend() {
        List<AnnouncementSummaryDTO> summaries = service.getAdminActiveBackend();
        return ResponseEntity.ok(summaries);
    }

    // Existing JSON POST (no file) - use JDBC insertion
    @PostMapping
    public ResponseEntity<Announcements> createAnnouncementJdbc(@RequestBody Announcements entity) {
        Announcements created = service.createAnnouncementJdbc(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Multipart upload: file + meta (announcement JSON as 'meta')
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(@RequestPart(value = "file") MultipartFile file,
                                    @RequestPart(value = "meta", required = false) Announcements meta) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        if (meta == null || meta.getTitle() == null || meta.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().body("meta 必須包含 title 欄位且不可為 null");
        }

        String originalFilename = file.getOriginalFilename();
        String original = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "file";
        // 使用 UUID_原始檔名 格式，保留原始檔名方便前端識別
        String filename = UUID.randomUUID() + "_" + original;
        try {
            Path target = storageLocation.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // 若前端未提供 CreatedTime，則填入現在時間
            if (meta.getCreatedTime() == null) meta.setCreatedTime(LocalDateTime.now());
            meta.setAttachmentPath(filename);
            Announcements created = service.createAnnouncementJdbc(meta);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to store file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create announcement: " + e.getMessage());
        }
    }

    // Update with optional file replacement (multipart)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateWithFile(@PathVariable UUID id,
                                            @RequestPart(value = "file", required = false) MultipartFile file,
                                            @RequestPart(value = "meta", required = false) Announcements meta) {
        Optional<Announcements> existingOpt = service.getById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Announcements existing = existingOpt.get();

        // Merge fields from meta into existing (keep existing values when meta fields are null)
        if (meta != null) {
            if (meta.getTitle() != null) existing.setTitle(meta.getTitle());
            if (meta.getContent() != null) existing.setContent(meta.getContent());
            if (meta.getType() != null) existing.setType(meta.getType());
            if (meta.getStartDate() != null) existing.setStartDate(meta.getStartDate());
            if (meta.getEndDate() != null) existing.setEndDate(meta.getEndDate());
            if (meta.getStatus() != null) existing.setStatus(meta.getStatus());
            if (meta.getCreatedUser() != null) existing.setCreatedUser(meta.getCreatedUser());
            if (meta.getCreatedTime() != null) existing.setCreatedTime(meta.getCreatedTime());
            if (meta.getUpdatedUser() != null) existing.setUpdatedUser(meta.getUpdatedUser());
            if (meta.getUpdatedTime() != null) existing.setUpdatedTime(meta.getUpdatedTime());
            // if meta explicitly sets attachmentPath, allow overwrite
            if (meta.getAttachmentPath() != null) existing.setAttachmentPath(meta.getAttachmentPath());
        }

        // Handle file replacement
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String original = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "file";
            // 使用 UUID_原始檔名 格式，保留原始檔名方便前端識別
            String filename = UUID.randomUUID() + "_" + original;
            try {
                Path target = storageLocation.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                // delete old file if exists
                if (existing.getAttachmentPath() != null) {
                    try { Files.deleteIfExists(storageLocation.resolve(existing.getAttachmentPath())); } catch (IOException ignored) {}
                }
                existing.setAttachmentPath(filename);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Failed to store file: " + e.getMessage());
            }
        }

        try {
            Announcements updated = service.updateWithJdbc(id, existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update announcement: " + e.getMessage());
        }
    }

    // New endpoint: download attachment by announcement ID
    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id) {
        Optional<Announcements> entityOpt = service.getById(id);
        if (entityOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Announcements announcement = entityOpt.get();
        String attachment = announcement.getAttachmentPath();
        if (attachment == null || attachment.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        try {
            Path filePath = storageLocation.resolve(attachment).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = null;
            try { contentType = Files.probeContentType(filePath); } catch (IOException ignored) {}
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            // 從 UUID_原始檔名 格式中提取原始檔名
            String displayFilename = attachment;
            int underscoreIdx = attachment.indexOf('_');
            if (underscoreIdx > 0 && underscoreIdx < attachment.length() - 1) {
                displayFilename = attachment.substring(underscoreIdx + 1);
            }

            // RFC5987 encode for filename* (UTF-8 percent-encoding)
            String encodedFilename;
            try {
                encodedFilename = URLEncoder.encode(displayFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            } catch (java.io.UnsupportedEncodingException e) {
                // Shouldn't happen for UTF-8; fallback to a simple safe replacement
                encodedFilename = displayFilename.replaceAll("\\\"", "'").replaceAll(" ", "%20");
            }
            // provide both filename (fallback) and filename* for proper UTF-8 downloads
            String contentDisposition = "attachment; filename=\"" + displayFilename.replaceAll("\"", "'") + "\"; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Delete announcement by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            // Check if announcement exists
            Optional<Announcements> existing = service.getById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Delete associated attachment file if exists
            Announcements announcement = existing.get();
            if (announcement.getAttachmentPath() != null && !announcement.getAttachmentPath().isEmpty()) {
                try {
                    Files.deleteIfExists(storageLocation.resolve(announcement.getAttachmentPath()));
                } catch (IOException e) {
                    // Log the error but don't fail the deletion
                    System.err.println("Failed to delete attachment file: " + e.getMessage());
                }
            }

            // Delete the announcement from database
            boolean deleted = service.delete(id);
            if (deleted) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "公告已刪除");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body("刪除公告失敗");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("刪除公告失敗: " + e.getMessage());
        }
    }
}
