package Group4.Childcare.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

  @Value("${attachment.upload.dir:IdentityResource}")
  private String uploadDir;

  private static final String INSTITUTION_RESOURCE_DIR = "InstitutionResource";

  /**
   * 讀取指定案件的所有檔案名稱
   * 檔案夾位置: IdentityResource/{ApplicationID}/
   * @param applicationId 案件ID
   * @return 檔案名稱列表
   */
  public List<String> getFilesByApplicationId(UUID applicationId) {
    try {
      Path folderPath = Paths.get(uploadDir, applicationId.toString());

      // 如果文件夾不存在，返回空列表
      if (!Files.exists(folderPath)) {
        return new ArrayList<>();
      }

      File folder = folderPath.toFile();
      if (!folder.isDirectory()) {
        return new ArrayList<>();
      }

      // 讀取文件夾中的所有檔案名稱
      File[] files = folder.listFiles(File::isFile);
      if (files == null || files.length == 0) {
        return new ArrayList<>();
      }

      return Arrays.stream(files)
                   .map(File::getName)
                   .sorted()
                   .collect(Collectors.toList());
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  /**
   * 檢查指定案件的文件夾是否存在
   * 檔案夾位置: IdentityResource/{ApplicationID}/
   * @param applicationId 案件ID
   * @return true 如果存在，false 否則
   */
  public boolean folderExists(UUID applicationId) {
    try {
      Path folderPath = Paths.get(uploadDir, applicationId.toString());
      return Files.exists(folderPath) && Files.isDirectory(folderPath);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 建立案件的文件夾
   * 檔案夾位置: IdentityResource/{ApplicationID}/
   * @param applicationId 案件ID
   * @return true 如果成功，false 否則
   */
  public boolean createFolder(UUID applicationId) {
    try {
      Path folderPath = Paths.get(uploadDir, applicationId.toString());
      if (!Files.exists(folderPath)) {
        Files.createDirectories(folderPath);
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 取得案件附件儲存的根目錄路徑
   * 選項A：所有案件共用同一個資料夾（例如 IdentityResource）
   * 不再依照 ApplicationID 建立子資料夾，避免自動建立資料夾的需求
   * @param applicationId 目前不再作為子資料夾使用，保留參數以維持呼叫端介面
   * @return 根目錄路徑（例如 IdentityResource）
   */
  public Path getFolderPath(UUID applicationId) {
    return Paths.get(uploadDir); // 不再附加 applicationId 子資料夾
  }

  /**
   * 儲存機構圖片到 InstitutionResource 目錄
   * 每個機構僅儲放一張圖片，新上傳會覆蓋舊的
   * @param file 上傳的檔案
   * @param institutionId 機構ID
   * @return 儲存後的檔案路徑（相對路徑）
   * @throws IOException 如果儲存失敗
   */
  public String saveInstitutionImage(MultipartFile file, UUID institutionId) throws IOException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("檔案不能為空");
    }

    // 驗證檔案類型（僅允許圖片）
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("僅支援圖片檔案");
    }

    // 建立 InstitutionResource 目錄
    Path institutionDir = Paths.get(System.getProperty("user.dir"), INSTITUTION_RESOURCE_DIR);
    if (!Files.exists(institutionDir)) {
      Files.createDirectories(institutionDir);
    }

    // 清除該機構的舊圖片
    deleteInstitutionImage(institutionId);

    // 生成新檔名：UUID_原始檔名
    String originalFilename = file.getOriginalFilename();
    String fileName = institutionId + "_" + originalFilename;
    Path filePath = institutionDir.resolve(fileName);

    // 儲存檔案
    Files.copy(file.getInputStream(), filePath);

    // 返回相對路徑供 imagePath 欄位儲存
    return "/InstitutionResource/" + fileName;
  }

  /**
   * 刪除機構的圖片
   * @param institutionId 機構ID
   */
  public void deleteInstitutionImage(UUID institutionId) {
    try {
      Path institutionDir = Paths.get(System.getProperty("user.dir"), INSTITUTION_RESOURCE_DIR);
      if (!Files.exists(institutionDir)) {
        return;
      }

      // 查找並刪除以該機構ID開頭的檔案
      File[] files = institutionDir.toFile().listFiles(f ->
          f.isFile() && f.getName().startsWith(institutionId.toString() + "_")
      );

      if (files != null) {
        for (File file : files) {
          Files.deleteIfExists(file.toPath());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 獲取機構的圖片檔名
   * @param institutionId 機構ID
   * @return 檔名，若不存在返回 null
   */
  public String getInstitutionImageFileName(UUID institutionId) {
    try {
      Path institutionDir = Paths.get(System.getProperty("user.dir"), INSTITUTION_RESOURCE_DIR);
      if (!Files.exists(institutionDir)) {
        return null;
      }

      File[] files = institutionDir.toFile().listFiles(f ->
          f.isFile() && f.getName().startsWith(institutionId.toString() + "_")
      );

      if (files != null && files.length > 0) {
        return files[0].getName();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
