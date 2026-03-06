package pl.polsl.bland.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "schematics")
public class SchematicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String schematicJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SchematicEntity() {
    }

    public SchematicEntity(String name, String schematicJson, UserEntity owner) {
        this.name = name;
        this.schematicJson = schematicJson;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchematicJson() {
        return schematicJson;
    }

    public void setSchematicJson(String schematicJson) {
        this.schematicJson = schematicJson;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
