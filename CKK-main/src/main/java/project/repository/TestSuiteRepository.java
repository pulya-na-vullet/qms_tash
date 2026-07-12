package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.model.TestSuite;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    List<TestSuite> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    List<TestSuite> findByProjectIdAndNameContainingIgnoreCaseOrderByCreatedAtAsc(Long projectId, String name);
    Optional<TestSuite> findByProjectIdAndName(Long projectId, String name);
}