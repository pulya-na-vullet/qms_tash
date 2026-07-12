package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.model.TestStep;

import java.util.List;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, Long> {
    List<TestStep> findByTestCaseIdOrderByStepNumberAsc(Long testCaseId);
    void deleteByTestCaseId(Long testCaseId);
}