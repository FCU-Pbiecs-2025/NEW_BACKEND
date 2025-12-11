package Group4.Childcare.Controller;

import Group4.Childcare.Model.Cancellation;
import Group4.Childcare.Service.CancellationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/cancellation")
public class CancellationController {
    @Autowired
    private CancellationService service;

    @PostMapping
    public ResponseEntity<Cancellation> create(@RequestBody Cancellation entity) {
        return ResponseEntity.ok(service.create(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cancellation> getById(@PathVariable UUID id) {
        Optional<Cancellation> entity = service.getById(id);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Cancellation> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cancellation> update(@PathVariable UUID id, @RequestBody Cancellation entity) {
        return ResponseEntity.ok(service.update(id, entity));
    }
}

