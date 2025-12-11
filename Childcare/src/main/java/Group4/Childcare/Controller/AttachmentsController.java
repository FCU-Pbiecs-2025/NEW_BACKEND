package Group4.Childcare.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import Group4.Childcare.Repository.ApplicationsJdbcRepository;
import java.util.UUID;


@RestController
public class AttachmentsController {
    // TODO: 請依實際情況注入你的 ApplicationService 或 Repository
    // @Autowired
    // private ApplicationService applicationService;

    @Autowired
    private ApplicationsJdbcRepository applicationsJdbcRepository;

    private final String ATTACHMENT_DIR = "AttachmentResource";

    @PostMapping("/applications/{id}/attachments")
    public ResponseEntity<?> uploadAttachments(
            @PathVariable("id") String applicationId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "file1", required = false) MultipartFile file1,
            @RequestParam(value = "file2", required = false) MultipartFile file2,
            @RequestParam(value = "file3", required = false) MultipartFile file3
    ) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        MultipartFile[] files = {file, file1, file2, file3};
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(f.getOriginalFilename()));
                String uuid = UUID.randomUUID().toString();
                String saveName = uuid + "_" + originalFilename;
                File dir = new File(ATTACHMENT_DIR);
                if (!dir.exists()) dir.mkdirs();
                Path savePath = Paths.get(ATTACHMENT_DIR, saveName);
                Files.copy(f.getInputStream(), savePath);
                savedPaths.add(savePath.toString());
            } else {
                savedPaths.add(null);
            }
        }
        // 使用 JDBC 更新 applications 資料表的 AttachmentPath 欄位
        try {
            UUID appUuid = applicationId != null && !applicationId.isEmpty() ? UUID.fromString(applicationId) : null;
            // 只在 applicationId 不為 null 時才更新
            if (appUuid != null) {
                applicationsJdbcRepository.updateAttachmentPaths(appUuid, savedPaths.get(0), savedPaths.get(1), savedPaths.get(2), savedPaths.get(3));
            }
        } catch (Exception ex) {
            // 若轉換或更新失敗，繼續回傳檔案路徑，但紀錄錯誤（在真實專案請改用 logger）
            System.err.println("Failed to update attachment paths for applicationId=" + applicationId + ": " + ex.getMessage());
        }
        // 回傳路徑陣列
        Map<String, Object> result = new HashMap<>();
        result.put("attachmentPaths", savedPaths);
        return ResponseEntity.ok(result);
    }
}
