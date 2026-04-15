package pl.polsl.bland.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "simulation_results")
public class SimulationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private SchematicEntity project;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String dataCsv;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected SimulationResultEntity() {
    }

    public SimulationResultEntity(SchematicEntity project, String status, String dataCsv) {
        this.project = project;
        this.status = status;
        this.dataCsv = dataCsv;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public SchematicEntity getProject() { return project; }
    public String getStatus() { return status; }
    public String getDataCsv() { return dataCsv; }
    public Instant getTimestamp() { return timestamp; }
}
