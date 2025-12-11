package Group4.Childcare.Service;

import Group4.Childcare.Model.Cancellation;
import Group4.Childcare.Repository.CancellationJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CancellationService {
    @Autowired
    private CancellationJdbcRepository repository;

    public Cancellation create(Cancellation entity) {
        return repository.save(entity);
    }

    public Optional<Cancellation> getById(UUID id) {
        return repository.findById(id);
    }

    public List<Cancellation> getAll() {
        return repository.findAll();
    }

    public Cancellation update(UUID id, Cancellation entity) {
        entity.setCancellationID(id);
        return repository.save(entity);
    }
}

