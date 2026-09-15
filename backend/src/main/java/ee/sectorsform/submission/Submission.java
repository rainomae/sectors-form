package ee.sectorsform.submission;

import ee.sectorsform.sector.Sector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** One saved form. Ownership is not stored: the id lives in the HTTP session that created the submission. */
@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored trimmed; blank names are rejected before they get here.
    @Column(name = "name", nullable = false)
    private String name;

    // Always true for a stored row; the form cannot be saved without agreeing.
    @Column(name = "agreed_to_terms", nullable = false)
    private boolean agreedToTerms;

    @ManyToMany
    @JoinTable(name = "submission_sector", joinColumns = @JoinColumn(name = "submission_id"), inverseJoinColumns = @JoinColumn(name = "sector_id"))
    private Set<Sector> sectors = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Submission() {
        // required by JPA
    }

    Submission(String name, Set<Sector> sectors, boolean agreedToTerms) {
        this.name = name;
        this.sectors.addAll(sectors);
        this.agreedToTerms = agreedToTerms;
    }

    void update(String name, Set<Sector> sectors, boolean agreedToTerms) {
        this.name = name;
        this.sectors.clear();
        this.sectors.addAll(sectors);
        this.agreedToTerms = agreedToTerms;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAgreedToTerms() {
        return agreedToTerms;
    }

    public Set<Sector> getSectors() {
        return sectors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
