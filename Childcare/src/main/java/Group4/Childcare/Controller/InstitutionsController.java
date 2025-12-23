package Group4.Childcare.Controller;

import Group4.Childcare.Model.Institutions;
import Group4.Childcare.DTO.InstitutionSummaryDTO;
import Group4.Childcare.DTO.InstitutionSimpleDTO;
import Group4.Childcare.Service.InstitutionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/institutions")
public class InstitutionsController {
  @Autowired
  private InstitutionsService service;

  /**
   * 建立機構資料（純 JSON）
   * POST /institutions
   * Content-Type: application/json
   *
   * Request Body 範例:
   * {
   *   "institutionName": "小天使托嬰中心",
   *   "contactPerson": "陳淑芬",
   *   "address": "台中市西區公益路100號",
   *   "phoneNumber": "04-23456789",
   *   "fax": "04-23456780",
   *   "email": "angel@daycare.com",
   *   "description": "溫馨的托育環境",
   *   "responsiblePerson": "陳建國",
   *   "latitude": 24.148000,
   *   "longitude": 120.664000,
   *   "institutionsType": true
   * }
   *
   * @param entity 機構資料
   * @return 新建的機構資訊
   */
  @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Institutions> create(@RequestBody Institutions entity) {
    return ResponseEntity.ok(service.create(entity));
  }

  /**
   * 建立機構資料並上傳圖片（multipart/form-data 格式）
   * POST /institutions
   * Content-Type: multipart/form-data
   *
   * Form Data:
   * - data: 機構 JSON 資料（application/json）
   * - image: 圖片檔案（可選）
   *
   * @param data 機構 JSON 資料字符串
   * @param image 圖片檔案
   * @return 新建的機構資訊
   */
  @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createWithImage(
          @RequestPart(value = "data") Institutions entity,
          @RequestPart(value = "image", required = false) MultipartFile image) {
    try {
      // 如果有上傳圖片，使用 createWithImage
      if (image != null && !image.isEmpty()) {
        Institutions created = service.createWithImage(entity, image);
        return ResponseEntity.ok(created);
      }

      // 如果沒有圖片，使用一般的 create
      return ResponseEntity.ok(service.create(entity));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("錯誤: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("建立失敗: " + e.getMessage());
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Institutions> getById(@PathVariable UUID id) {
    Optional<Institutions> entity = service.getById(id);
    return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping
  public List<Institutions> getAll() {
    return service.getAll();
  }

  /**
   * 更新機構資料（支援圖片上傳）
   *
   * 使用方式1 - 純 JSON 更新（Content-Type: application/json）:
   * PUT /institutions/{id}
   * {
   *   "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *   "institutionName": "小天使托嬰中心",
   *   "contactPerson": "陳淑芬",
   *   "address": "台中市西區公益路100號",
   *   "phoneNumber": "04-23456789",
   *   "fax": "04-23456780",
   *   "email": "angel@daycare.com",
   *   "description": "溫馨的托育環境",
   *   "responsiblePerson": "陳建國",
   *   "imagePath": "/InstitutionResource/uuid_image.jpg",
   *   "createdUser": "admin",
   *   "createdTime": "2025-11-20T22:06:47.46",
   *   "updatedUser": "admin",
   *   "updatedTime": "2025-11-20T22:06:47.46",
   *   "latitude": 24.148000,
   *   "longitude": 120.664000,
   *   "institutionsType": true
   * }
   *
   * 使用方式2 - 同時上傳圖片（Content-Type: multipart/form-data）:
   * PUT /institutions/{id}
   * - data: 上述 JSON 資料（application/json）
   * - image: 圖片檔案
   *
   * @param id 機構ID
   * @param entity 機構 JSON 資料（application/json 時傳入）
   * @return 更新後的機構資訊
   */
  @PutMapping(value = "/{id}", consumes = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Institutions> update(@PathVariable UUID id, @RequestBody Institutions entity) {
    try {
      return ResponseEntity.ok(service.update(id, entity));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(null);
    }
  }

  /**
   * PUT /institutions/{id}
   * body範例
   * {
   *     "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *     "institutionName": "小天使托嬰中心",
   *     "contactPerson": "陳淑於",
   *     "address": "台中市西區公益路100號",
   *     "phoneNumber": "04-23456789",
   *     "fax": "04-23456780",
   *     "email": "angel@daycare.com",
   *     "relatedLinks": "https://angel-daycare.com",
   *     "description": "溫馨的托育環境",
   *     "responsiblePerson": "陳建國",
   *     "imagePath": "/images/institution4.jpg",
   *     "createdUser": "admin",
   *     "createdTime": "2025-11-20T22:06:47.46",
   *     "updatedUser": "admin",
   *     "updatedTime": "2025-11-20T22:06:47.46",
   *     "latitude": 24.148000,
   *     "longitude": 120.664000,
   *     "institutionsType": true
   * }
   * 更新機構資料並上傳圖片（multipart/form-data 格式）
   * @param id 機構ID
   * @param data 機構 JSON 資料字符串
   * @param image 圖片檔案
   * @return 更新後的機構資訊
   */
  @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> updateWithImage(
          @PathVariable UUID id,
          @RequestPart(value = "data", required = false) String data,
          @RequestPart(value = "image", required = false) MultipartFile image) {
    try {
      // 獲取現有機構資料
      Optional<Institutions> existing = service.getById(id);
      if (existing.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      Institutions entity = existing.get();

      // 如果有上傳新圖片，更新機構資訊並上傳圖片
      if (image != null && !image.isEmpty()) {
        Institutions updated = service.updateWithImage(id, entity, image);
        return ResponseEntity.ok(updated);
      }

      // 如果只有資料更新，沒有圖片
      if (data != null && !data.isEmpty()) {
        return ResponseEntity.ok(service.update(id, entity));
      }

      return ResponseEntity.badRequest().body("請提供要更新的資料或圖片");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("錯誤: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("更新失敗: " + e.getMessage());
    }
  }

  @GetMapping("/summary")
  public ResponseEntity<List<InstitutionSummaryDTO>> getSummary() {
    return ResponseEntity.ok(service.getSummaryAll());
  }

  /**
   * 取得所有機構的 ID 和 name
   * 使用在個案查詢機構下拉選單
   * @return ResponseEntity<List<InstitutionSimpleDTO>>
   */
  @GetMapping("/simple/all")
  public ResponseEntity<List<InstitutionSimpleDTO>> getAllSimple() {
    return ResponseEntity.ok(service.getAllSimple());
  }
  /**
   * GET /institutions/offset
   * 回傳範例資料為:
   * {
   *     "offset": 0,
   *     "size": 10,
   *     "totalPages": 1,
   *     "hasNext": false,
   *     "content": [
   *         {
   *             "institutionID": "e09f1689-17a4-46f7-ae95-160a368147af",
   *             "institutionName": "小天使托嬰中心",
   *             "contactPerson": "陳淑芬",
   *             "address": "台中市西區公益路100號",
   *             "phoneNumber": "04-23456789",
   *             "fax": "04-23456780",
   *             "email": "angel@daycare.com",
   *             "relatedLinks": "https://angel-daycare.com",
   *             "description": "溫馨的托育環境",
   *             "responsiblePerson": "陳建國",
   *             "imagePath": "/images/institution4.jpg",
   *             "createdUser": "admin",
   *             "createdTime": "2025-11-20T22:06:47.46",
   *             "updatedUser": "admin",
   *             "updatedTime": "2025-11-20T22:06:47.46",
   *             "latitude": 24.148000,
   *             "longitude": 120.664000,
   *             "institutionsType": true
   *         }
   *     ],
   *     "totalElements": 4
   * }
   * 取得機構分頁資料
   * @param offset 起始項目索引
   * @param size 每頁大小
   * @param InstitutionID 機構 ID（可選，admin 角色傳入以過濾特定機構）
   * @param search 搜尋關鍵字（可選，搜尋機構名稱、聯絡人、電話）
   * @return ResponseEntity<InstitutionOffsetDTO>
   */
  @GetMapping("/offset")
  public ResponseEntity<Group4.Childcare.DTO.InstitutionOffsetDTO> getOffset(
          @RequestParam(defaultValue = "0") int offset,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(required = false) UUID InstitutionID,
          @RequestParam(required = false) String search) {

    return ResponseEntity.ok(service.getOffset(offset, size, InstitutionID, search));
  }

  /**
   * GET /institutions/offset/name-search
   * 專門搜尋機構名稱的分頁API
   * 取得機構分頁資料（僅搜尋機構名稱）
   * @param offset 起始項目索引
   * @param size 每頁大小
   * @param InstitutionID 機構 ID（可選，admin 角色傳入以過濾特定機構）
   * @param name 機構名稱搜尋關鍵字（可選，僅搜尋機構名稱）
   * @return ResponseEntity<InstitutionOffsetDTO>
   */
  @GetMapping("/offset/name-search")
  public ResponseEntity<Group4.Childcare.DTO.InstitutionOffsetDTO> getOffsetByName(
          @RequestParam(defaultValue = "0") int offset,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(required = false) UUID InstitutionID,
          @RequestParam(required = false) String name) {

    return ResponseEntity.ok(service.getOffsetByName(offset, size, InstitutionID, name));
  }
}