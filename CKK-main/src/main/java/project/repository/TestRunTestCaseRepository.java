package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.model.TestRunTestCase;

import java.util.List;

@Repository
public interface TestRunTestCaseRepository extends JpaRepository<TestRunTestCase, Long> {
    List<TestRunTestCase> findByTestRunIdOrderByCreatedAtAsc(Long testRunId);
    void deleteByTestRunId(Long testRunId);
    List<TestRunTestCase> findByTestCaseId(Long testCaseId);
}