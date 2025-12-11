package Group4.Childcare.Service;

import Group4.Childcare.Model.Banners;
import Group4.Childcare.Repository.BannersJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannersService {
    private final BannersJdbcRepository repository;

    @Autowired
    public BannersService(BannersJdbcRepository repository) {
        this.repository = repository;
    }

    public Banners create(Banners entity) {
        return repository.save(entity);
    }

    public Optional<Banners> getById(Integer id) {
        return repository.findById(id);
    }

    public List<Banners> getAll() {
        return repository.findAll();
    }

    public List<Banners> getBannersWithOffsetJdbc(int offset, int limit) {
        return repository.findWithOffset(offset, limit);
    }

    public long getTotalCount() {
        return repository.count();
    }

    public Banners update(Integer id, Banners entity) {
        entity.setSortOrder(id);
        return repository.save(entity);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

    // 取得上架且未過期的 banners
    public List<Banners> findActiveBanners() {
        return repository.findActiveBanners();
    }
}
