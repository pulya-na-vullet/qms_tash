package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.model.TestCase;

import java.util.List;
import java.util.Set;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByTestSuiteIdOrderByCreatedAtAsc(Long testSuiteId);
    List<TestCase> findByTestSuiteIdAndNameContainingIgnoreCaseOrderByCreatedAtAsc(Long testSuiteId, String name);

    // *** ИСПРАВЛЕННЫЙ ЗАПРОС ***
    // Получить все тест-кейсы проекта через тест-сьюты
    @Query("SELECT tc FROM TestCase tc JOIN tc.testSuite ts WHERE ts.project.id = :projectId ORDER BY tc.createdAt ASC")
    List<TestCase> findByTestSuite_ProjectIdOrderByCreatedAtAsc(Long projectId);

    // Найти тест-кейсы по ID User Story через промежуточную сущность
    @Query("SELECT DISTINCT tc FROM TestCase tc JOIN tc.testCaseUserStories tcus WHERE tcus.userStory.id = :userStoryId")
    List<TestCase> findByUserStoryId(Long userStoryId);

    // Поиск тест-кейсов проекта по названию
    @Query("SELECT tc FROM TestCase tc JOIN tc.testSuite ts WHERE ts.project.id = :projectId AND tc.name LIKE %:term% ORDER BY tc.createdAt ASC")
    List<TestCase> findByTestSuite_ProjectIdAndNameContainingIgnoreCaseOrderByCreatedAtAsc(Long projectId, String term);
}