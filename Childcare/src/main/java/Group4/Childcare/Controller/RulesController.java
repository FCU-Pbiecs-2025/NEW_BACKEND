package Group4.Childcare.Controller;

import Group4.Childcare.Model.Rules;
import Group4.Childcare.Service.RulesService;
import Group4.Childcare.Repository.RulesJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rules")
public class RulesController {
    @Autowired
    private RulesService service;

    @Autowired
    private RulesJdbcRepository jdbcRepository;

    /**
     * 新增一筆規則資料
     * @param entity 規則物件
     * @return 新增後的規則
     */
    @PostMapping
    public ResponseEntity<Rules> create(@RequestBody Rules entity) {
        return ResponseEntity.ok(service.create(entity));
    }

    /**
     * 依據ID取得單一規則
     * @param id 規則ID
     * @return 規則物件或404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Rules> getById(@PathVariable Long id) {
        Optional<Rules> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 取得所有規則列表
     * @return 規則列表
     */
    @GetMapping
    public List<Rules> getAll() {
        return service.getAll();
    }

    /**
     * 更新指定ID的規則
     * @param id 規則ID
     * @param entity 新規則內容
     * @return 更新後的規則
     */
    @PutMapping("/{id}")
    public ResponseEntity<Rules> update(@PathVariable Long id, @RequestBody Rules entity) {
        return ResponseEntity.ok(service.update(id, entity));
    }

    /**
     * 使用 JDBC 方式更新指定ID的規則
     * @param id 規則ID
     * @param entity 新規則內容
     * @return 更新後的規則
     */
    @PutMapping("/jdbc/{id}")
    public ResponseEntity<Rules> updateWithJdbc(@PathVariable Long id, @RequestBody Rules entity) {
        try {
            Rules updatedRules = jdbcRepository.updateById(id, entity);
            return ResponseEntity.ok(updatedRules);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
