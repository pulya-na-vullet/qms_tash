package project.repository;

import project.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    // Найти все секции по ID проекта
    List<Section> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    // Проверить существование секции по ID
    boolean existsById(Long id);
}