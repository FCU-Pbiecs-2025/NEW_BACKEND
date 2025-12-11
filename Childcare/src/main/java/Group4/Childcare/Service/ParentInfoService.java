package Group4.Childcare.Service;

import Group4.Childcare.Model.ParentInfo;
import Group4.Childcare.Repository.ParentInfoJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ParentInfoService {
    @Autowired
    private ParentInfoJdbcRepository repository;

    public ParentInfo create(ParentInfo entity) {
        return repository.save(entity);
    }

    public Optional<ParentInfo> getById(UUID id) {
        return repository.findById(id);
    }

    public List<ParentInfo> getAll() {
        return repository.findAll();
    }

    public ParentInfo update(UUID id, ParentInfo entity) {
        entity.setParentID(id);
        return repository.save(entity);
    }

    public List<ParentInfo> getByFamilyInfoID(UUID familyInfoID) {
        return repository.findByFamilyInfoID(familyInfoID);
    }
}

