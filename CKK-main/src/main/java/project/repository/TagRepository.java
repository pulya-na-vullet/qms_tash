package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.model.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByProjectIdOrderByCreatedAtAsc(Long projectId);
    Optional<Tag> findByProjectIdAndName(Long projectId, String name);
    List<Tag> findByProjectIdAndNameContainingIgnoreCase(Long projectId, String name);

    @Query("SELECT t FROM Tag t JOIN t.testCases tc WHERE tc.id = :testCaseId")
    List<Tag> findTagsByTestCaseId(Long testCaseId);
}