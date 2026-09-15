package ee.sectorsform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Verifies that the Flyway migrations apply on H2 (PostgreSQL mode) and seed the full sector hierarchy. */
@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seedsEveryOptionOfTheOriginalSelectBox() {
        assertThat(count("SELECT COUNT(*) FROM sector")).isEqualTo(79);
        assertThat(count("SELECT COUNT(DISTINCT sort_order) FROM sector")).isEqualTo(79);
        assertThat(count("SELECT MIN(sort_order) FROM sector")).isEqualTo(1);
        assertThat(count("SELECT MAX(sort_order) FROM sector")).isEqualTo(79);
    }

    @Test
    void keepsTheOriginalRootsAndOrder() {
        var roots =
                jdbc.queryForList("SELECT name FROM sector WHERE parent_id IS NULL ORDER BY sort_order", String.class);
        assertThat(roots).containsExactly("Manufacturing", "Other", "Service");

        var firstFive = jdbc.queryForList("SELECT id FROM sector ORDER BY sort_order LIMIT 5", Long.class);
        assertThat(firstFive).containsExactly(1L, 19L, 18L, 6L, 342L);
    }

    @Test
    void derivesFourLevelsFromTheIndentation() {
        Map<Long, Long> parentById = new HashMap<>();
        jdbc.query("SELECT id, parent_id FROM sector", rs -> {
            parentById.put(rs.getLong("id"), rs.getObject("parent_id", Long.class));
        });

        Map<Integer, Long> countByDepth = new TreeMap<>();
        for (Long id : parentById.keySet()) {
            countByDepth.merge(depthOf(id, parentById), 1L, Long::sum);
        }

        assertThat(countByDepth)
                .containsExactly(Map.entry(0, 3L), Map.entry(1, 19L), Map.entry(2, 47L), Map.entry(3, 10L));
        assertThat(parentOf(271L)).isEqualTo(97L); // Aluminium and steel workboats -> Maritime
        assertThat(parentOf(97L)).isEqualTo(12L); // Maritime -> Machinery
        assertThat(parentOf(12L)).isEqualTo(1L); // Machinery -> Manufacturing
        assertThat(parentOf(113L)).isEqualTo(21L); // Water -> Transport and Logistics
    }

    @Test
    void cleansUpTheOptionLabels() {
        assertThat(nameOf(42L)).isEqualTo("Fish & fish products");
        assertThat(nameOf(390L)).isEqualTo("Children’s room");
        assertThat(nameOf(5L)).isEqualTo("Printing");
        assertThat(jdbc.queryForList("SELECT name FROM sector WHERE name <> TRIM(name)", String.class))
                .isEmpty();
    }

    @Test
    void createsEmptySubmissionTables() {
        assertThat(count("SELECT COUNT(*) FROM submission")).isZero();
        assertThat(count("SELECT COUNT(*) FROM submission_sector")).isZero();
    }

    private static int depthOf(Long id, Map<Long, Long> parentById) {
        int depth = 0;
        for (Long parent = parentById.get(id); parent != null; parent = parentById.get(parent)) {
            depth++;
        }
        return depth;
    }

    private Long parentOf(long id) {
        return jdbc.queryForObject("SELECT parent_id FROM sector WHERE id = ?", Long.class, id);
    }

    private String nameOf(long id) {
        return jdbc.queryForObject("SELECT name FROM sector WHERE id = ?", String.class, id);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
