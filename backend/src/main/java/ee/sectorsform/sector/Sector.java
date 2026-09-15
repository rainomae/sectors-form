package ee.sectorsform.sector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One node of the sector hierarchy shown in the form. Reference data seeded by Flyway from the
 * original select box; never created or edited through the API.
 */
@Entity
@Table(name = "sector")
public class Sector {

    // The original <option value>; a stable business identifier, so not generated.
    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    // Null for the three top-level sectors.
    @Column(name = "parent_id")
    private Long parentId;

    // Position in the original list; ordering by it reproduces the original depth-first order.
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Sector() {
        // required by JPA
    }

    Sector(Long id, String name, Long parentId, int sortOrder) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
