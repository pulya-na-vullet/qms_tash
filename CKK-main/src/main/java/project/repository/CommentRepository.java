package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.model.Comment;
import project.model.TestCase;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Существующие методы
    List<Comment> findByTestCaseIdOrderByCreatedAtAsc(Long testCaseId);
    void deleteByTestCaseId(Long testCaseId);

    // НОВЫЙ МЕТОД: Получение комментариев с информацией о пользователе
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.testCase.id = :testCaseId ORDER BY c.createdAt ASC")
    List<Comment> findByTestCaseIdWithUser(@Param("testCaseId") Long testCaseId);

    List<Comment> findByTestCase(TestCase testCase);
}