// TestCaseReviewRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.model.TestCaseReview;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestCaseReviewRepository extends JpaRepository<TestCaseReview, Long> {
    Optional<TestCaseReview> findByTestCaseId(Long testCaseId);

    @Query("SELECT tcr FROM TestCaseReview tcr WHERE tcr.testCase.testSuite.id = :testSuiteId")
    List<TestCaseReview> findByTestSuiteId(Long testSuiteId);

    boolean existsByTestCaseId(Long testCaseId);

    void deleteByTestCaseId(Long testCaseId);
}