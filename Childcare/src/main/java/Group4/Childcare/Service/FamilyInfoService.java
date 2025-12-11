package Group4.Childcare.Service;

import Group4.Childcare.Model.FamilyInfo;
import Group4.Childcare.Repository.FamilyInfoJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FamilyInfoService {
    @Autowired
    private FamilyInfoJdbcRepository repository;

    public FamilyInfo create(FamilyInfo entity) {
        return repository.save(entity);
    }

    public Optional<FamilyInfo> getById(UUID id) {
        return repository.findById(id);
    }

    public List<FamilyInfo> getAll() {
        return repository.findAll();
    }

    public FamilyInfo update(UUID id, FamilyInfo entity) {
        entity.setFamilyInfoID(id);
        return repository.save(entity);
    }
}

