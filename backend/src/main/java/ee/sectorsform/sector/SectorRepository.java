package ee.sectorsform.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    List<Sector> findAllByOrderBySortOrderAsc();
}
