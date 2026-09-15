package ee.sectorsform.sector;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public Set<Sector> findAllById(Set<Long> ids) {
        Set<Sector> sectors = new HashSet<>(sectorRepository.findAllById(ids));
        if (sectors.size() != ids.size()) {
            Set<Long> found = sectors.stream().map(Sector::getId).collect(Collectors.toSet());
            throw new UnknownSectorException(ids.stream().filter(id -> !found.contains(id)).sorted().toList());
        }
        return sectors;
    }
}
