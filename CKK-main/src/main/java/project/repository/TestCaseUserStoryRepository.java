package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import project.model.TestCaseUserStory;

import java.util.List;

@Repository
public interface TestCaseUserStoryRepository extends JpaRepository<TestCaseUserStory, Long> {
    List<TestCaseUserStory> findByTestCaseId(Long testCaseId);

    @Modifying
    @Transactional
    void deleteByTestCaseId(Long testCaseId);

    List<TestCaseUserStory> findByUserStoryId(Long userStoryId);

    @Modifying
    @Transactional
    void deleteByUserStoryId(Long userStoryId);

    // *** ДОРАБОТАННЫЙ МЕТОД ЧЕРЕЗ QUERY ***
    @Modifying
    @Transactional
    @Query("DELETE FROM TestCaseUserStory tcus WHERE tcus.testCase.id = :testCaseId AND tcus.userStory.id = :userStoryId")
    void deleteByTestCaseIdAndUserStoryId(@Param("testCaseId") Long testCaseId, @Param("userStoryId") Long userStoryId);

    @Query("SELECT tcus FROM TestCaseUserStory tcus WHERE tcus.testCase.id = :testCaseId AND tcus.userStory.id = :userStoryId")
    List<TestCaseUserStory> findByTestCaseIdAndUserStoryId(Long testCaseId, Long userStoryId);

    // *** НОВЫЕ МЕТОДЫ ***
    List<TestCaseUserStory> findByProjectId(Long projectId);
    List<TestCaseUserStory> findByProjectIdAndTestCaseId(Long projectId, Long testCaseId);
    List<TestCaseUserStory> findByProjectIdAndUserStoryId(Long projectId, Long userStoryId);


}