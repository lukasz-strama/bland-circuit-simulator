package pl.polsl.bland.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.polsl.bland.backend.entity.SchematicEntity;

import java.util.List;

public interface SchematicRepository extends JpaRepository<SchematicEntity, Long> {

    List<SchematicEntity> findByOwnerId(Long ownerId);
}
