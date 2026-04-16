package pl.polsl.bland.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class SchematicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String elementsJson;

    @Column(columnDefinition = "TEXT")
    private String wiresJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SchematicEntity() {
    }

    public SchematicEntity(String name, String elementsJson, String wiresJson, UserEntity owner) {
        this.name = name;
        this.elementsJson = elementsJson;
        this.wiresJson = wiresJson;
        this.owner = owner;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getElementsJson() { return elementsJson; }
    public String getWiresJson() { return wiresJson; }
    public UserEntity getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setElementsJson(String elementsJson) { this.elementsJson = elementsJson; }
    public void setWiresJson(String wiresJson) { this.wiresJson = wiresJson; }
}
