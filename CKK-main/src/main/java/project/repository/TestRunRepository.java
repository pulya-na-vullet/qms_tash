package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.model.TestRun;

import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    @Query("SELECT tr FROM TestRun tr WHERE tr.project.id = :projectId AND " +
           "(LOWER(tr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(tr.executorName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(tr.creatorName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY tr.createdAt DESC")
    List<TestRun> searchByProjectId(Long projectId, String searchTerm);
}