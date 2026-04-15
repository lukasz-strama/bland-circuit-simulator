package pl.polsl.bland.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.polsl.bland.backend.entity.SimulationResultEntity;

import java.util.List;
import java.util.Optional;

public interface SimulationResultRepository extends JpaRepository<SimulationResultEntity, Long> {

    List<SimulationResultEntity> findByProjectIdOrderByTimestampDesc(Long projectId);

    Optional<SimulationResultEntity> findByIdAndProjectId(Long id, Long projectId);
}
