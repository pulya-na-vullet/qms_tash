package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.model.TraceabilityMatrix;

import java.util.Optional;

@Repository
public interface TraceabilityMatrixRepository extends JpaRepository<TraceabilityMatrix, Long> {
    Optional<TraceabilityMatrix> findByProjectId(Long projectId);
}
