package Group4.Childcare.Service;

import Group4.Childcare.Model.Rules;
import Group4.Childcare.Repository.RulesJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RulesService {
    @Autowired
    private RulesJdbcRepository repository;

    public Rules create(Rules entity) {
        return repository.save(entity);
    }

    public Optional<Rules> getById(Long id) {
        return repository.findById(id);
    }

    public List<Rules> getAll() {
        return repository.findAll();
    }

    public Rules update(Long id, Rules entity) {
        entity.setId(id);
        return repository.save(entity);
    }
}

