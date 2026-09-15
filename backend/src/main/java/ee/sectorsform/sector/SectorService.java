package ee.sectorsform.sector;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectorService {

    static final String CACHE = "sectors";

    private final SectorRepository sectorRepository;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    // Reference data: read from the database once per application start, served from memory afterwards.
    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public List<Sector> findAll() {
        return sectorRepository.findAllByOrderBySortOrderAsc();
    }
}
