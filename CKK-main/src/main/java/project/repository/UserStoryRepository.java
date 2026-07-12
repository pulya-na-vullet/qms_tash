package project.repository;

import project.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    // Найти все user stories по ID секции
    List<UserStory> findBySectionIdOrderByCreatedAtAsc(Long sectionId);

    // Найти все user stories по ID проекта через связь с секцией
    List<UserStory> findBySection_ProjectIdOrderByCreatedAtAsc(Long projectId);

    // Проверить существование user story по ID
    boolean existsById(Long id);
}