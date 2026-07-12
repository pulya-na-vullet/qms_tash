package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.model.AIAnalysis;

import java.util.List;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, Long> {
    List<AIAnalysis> findByTestSuiteIdOrderByCreatedAtDesc(Long testSuiteId);
}