package Group4.Childcare.Controller;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Service.FamilyInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/family-info")
public class FamilyInfoController {
    @Autowired
    private FamilyInfoService service;

    @PostMapping
    public ResponseEntity<FamilyInfo> create(@RequestBody FamilyInfo entity) {
        return ResponseEntity.ok(service.create(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyInfo> getById(@PathVariable UUID id) {
        Optional<FamilyInfo> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<FamilyInfo> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyInfo> update(@PathVariable UUID id, @RequestBody FamilyInfo entity) {
        return ResponseEntity.ok(service.update(id, entity));
    }
}

