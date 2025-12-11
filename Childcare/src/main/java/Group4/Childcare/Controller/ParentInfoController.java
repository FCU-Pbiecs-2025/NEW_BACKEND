package Group4.Childcare.Controller;

import Group4.Childcare.Model.ParentInfo;
import Group4.Childcare.Service.ParentInfoService;
import Group4.Childcare.Repository.ParentInfoJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/parent-info")
public class ParentInfoController {
    @Autowired
    private ParentInfoService service;

    @Autowired
    private ParentInfoJdbcRepository repository;

    @PostMapping
    public ResponseEntity<ParentInfo> create(@RequestBody ParentInfo entity) {
        // 驗證 familyInfoID 不能為空
        if (entity.getFamilyInfoID() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FamilyInfoID is required");
        }
        entity.setParentID(null);
        return ResponseEntity.ok(repository.save(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParentInfo> getById(@PathVariable UUID id) {
        Optional<ParentInfo> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ParentInfo> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParentInfo> update(@PathVariable UUID id, @RequestBody ParentInfo entity) {
        return ResponseEntity.ok(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteByParentID(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

