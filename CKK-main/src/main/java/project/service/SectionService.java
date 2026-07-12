package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.Section;
import project.model.Project;
import project.model.UserStory;
import project.model.TestCaseUserStory;
import project.model.dto.SectionDTO;
import project.repository.SectionRepository;
import project.repository.ProjectRepository;
import project.repository.UserStoryRepository;
import project.repository.TestCaseUserStoryRepository;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
public class SectionService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private TestCaseUserStoryRepository testCaseUserStoryRepository;

    @Autowired
    private UserStoryService userStoryService;

    public List<SectionDTO> getSectionsByProjectId(Long projectId) {
        System.out.println("Поиск секций для projectId: " + projectId);
        List<Section> sections = sectionRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        System.out.println("Найдено в репозитории: " + sections.size());

        return sections.stream()
                .map(this::convertToDTO)
                .collect(toList());
    }

    public SectionDTO createSection(Long projectId, String name) {
        System.out.println("Создание секции для projectId: " + projectId + ", name: " + name);
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isPresent()) {
            Section section = new Section();
            section.setName(name.trim());
            section.setProject(projectOptional.get());
            Section savedSection = sectionRepository.save(section);
            System.out.println("Создана секция с ID: " + savedSection.getId());
            return convertToDTO(savedSection);
        }
        System.out.println("Проект не найден для ID: " + projectId);
        return null;
    }

    @Transactional
    public Map<String, Object> importSectionsWithUserStories(Long projectId, List<Map<String, String>> importData) {
        Map<String, Object> result = new HashMap<>();
        Optional<Project> projectOptional = projectRepository.findById(projectId);

        if (projectOptional.isEmpty()) {
            result.put("importedSections", 0);
            result.put("importedUserStories", 0);
            return result;
        }

        Project project = projectOptional.get();
        int importedSections = 0;
        int importedUserStories = 0;

        // Группируем user stories по секциям
        Map<String, List<String>> sectionsMap = new LinkedHashMap<>();

        for (Map<String, String> row : importData) {
            String sectionName = row.get("sectionName");
            String userStoryName = row.get("userStoryName");

            if (sectionName != null && !sectionName.trim().isEmpty()) {
                sectionsMap.computeIfAbsent(sectionName.trim(), k -> new ArrayList<>());

                if (userStoryName != null && !userStoryName.trim().isEmpty()) {
                    sectionsMap.get(sectionName.trim()).add(userStoryName.trim());
                }
            }
        }

        // Создаем секции и user stories
        for (Map.Entry<String, List<String>> entry : sectionsMap.entrySet()) {
            String sectionName = entry.getKey();
            List<String> userStoryNames = entry.getValue();

            // Создаем секцию
            Section section = new Section();
            section.setName(sectionName);
            section.setProject(project);
            Section savedSection = sectionRepository.save(section);
            importedSections++;

            // Создаем user stories для этой секции
            for (String userStoryName : userStoryNames) {
                userStoryService.createUserStory(savedSection.getId(), userStoryName);
                importedUserStories++;
            }
        }

        result.put("importedSections", importedSections);
        result.put("importedUserStories", importedUserStories);
        return result;
    }

    public Optional<Section> getSectionById(Long id) {
        return sectionRepository.findById(id);
    }

    public SectionDTO updateSection(Long id, String name) {
        Optional<Section> sectionOptional = sectionRepository.findById(id);
        if (sectionOptional.isPresent()) {
            Section section = sectionOptional.get();
            section.setName(name.trim());
            Section updatedSection = sectionRepository.save(section);
            return convertToDTO(updatedSection);
        }
        return null;
    }

    public boolean deleteSection(Long id) {
        if (sectionRepository.existsById(id)) {
            sectionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteSectionWithUserStories(Long id) {
        Optional<Section> sectionOptional = sectionRepository.findById(id);
        if (sectionOptional.isPresent()) {
            Section section = sectionOptional.get();

            // Получаем все user stories секции
            List<UserStory> userStories = userStoryRepository.findBySectionIdOrderByCreatedAtAsc(id);

            // Удаляем все связанные записи в test_case_user_stories
            for (UserStory userStory : userStories) {
                // Удаляем связи с тест-кейсами
                List<TestCaseUserStory> testCaseUserStories =
                        testCaseUserStoryRepository.findByUserStoryId(userStory.getId());
                testCaseUserStoryRepository.deleteAll(testCaseUserStories);

                // Удаляем саму user story
                userStoryRepository.delete(userStory);
            }

            // Удаляем саму секцию
            sectionRepository.delete(section);

            return true;
        }
        return false;
    }

    private SectionDTO convertToDTO(Section section) {
        return new SectionDTO(
                section.getId(),
                section.getName(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                section.getProject() != null ? section.getProject().getId() : null
        );
    }
}