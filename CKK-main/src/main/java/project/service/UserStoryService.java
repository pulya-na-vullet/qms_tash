package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.UserStory;
import project.model.Section;
import project.model.TestCaseUserStory;
import project.model.dto.UserStoryDTO;
import project.repository.UserStoryRepository;
import project.repository.SectionRepository;
import project.repository.TestCaseUserStoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserStoryService {

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private TestCaseUserStoryRepository testCaseUserStoryRepository;

    public List<UserStoryDTO> getUserStoriesBySectionId(Long sectionId) {
        List<UserStory> userStories = userStoryRepository.findBySectionIdOrderByCreatedAtAsc(sectionId);
        return userStories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserStoryDTO> getUserStoriesByProjectId(Long projectId) {
        List<UserStory> userStories = userStoryRepository.findBySection_ProjectIdOrderByCreatedAtAsc(projectId);
        return userStories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserStoryDTO> getUserStoryById(Long id) {
        return userStoryRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional
    public UserStoryDTO createUserStory(Long sectionId, String name) {
        Optional<Section> sectionOptional = sectionRepository.findById(sectionId);
        if (sectionOptional.isPresent()) {
            UserStory userStory = new UserStory();
            userStory.setName(name.trim());
            userStory.setSection(sectionOptional.get());

            UserStory savedUserStory = userStoryRepository.save(userStory);
            return convertToDTO(savedUserStory);
        }
        return null;
    }

    @Transactional
    public int importUserStories(Long sectionId, List<String> userStoryNames) {
        Optional<Section> sectionOptional = sectionRepository.findById(sectionId);
        if (sectionOptional.isEmpty()) {
            return 0;
        }

        Section section = sectionOptional.get();
        int importedCount = 0;

        for (String name : userStoryNames) {
            if (name != null && !name.trim().isEmpty()) {
                UserStory userStory = new UserStory();
                userStory.setName(name.trim());
                userStory.setSection(section);
                userStoryRepository.save(userStory);
                importedCount++;
            }
        }

        return importedCount;
    }

    @Transactional
    public UserStoryDTO updateUserStory(Long id, String name) {
        Optional<UserStory> userStoryOptional = userStoryRepository.findById(id);
        if (userStoryOptional.isPresent()) {
            UserStory userStory = userStoryOptional.get();
            userStory.setName(name.trim());
            UserStory updatedUserStory = userStoryRepository.save(userStory);
            return convertToDTO(updatedUserStory);
        }
        return null;
    }

    @Transactional
    public boolean deleteUserStory(Long id) {
        if (userStoryRepository.existsById(id)) {
            // Удаляем связи с тест-кейсами
            List<TestCaseUserStory> testCaseUserStories =
                    testCaseUserStoryRepository.findByUserStoryId(id);
            testCaseUserStoryRepository.deleteAll(testCaseUserStories);

            // Удаляем саму user story
            userStoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteUserStoriesBySectionId(Long sectionId) {
        List<UserStory> userStories = userStoryRepository.findBySectionIdOrderByCreatedAtAsc(sectionId);

        for (UserStory userStory : userStories) {
            // Удаляем связи с тест-кейсами
            List<TestCaseUserStory> testCaseUserStories =
                    testCaseUserStoryRepository.findByUserStoryId(userStory.getId());
            testCaseUserStoryRepository.deleteAll(testCaseUserStories);

            // Удаляем саму user story
            userStoryRepository.delete(userStory);
        }
    }

    public UserStoryDTO convertToDTO(UserStory userStory) {
        return new UserStoryDTO(
                userStory.getId(),
                userStory.getName(),
                userStory.getCreatedAt(),
                userStory.getUpdatedAt(),
                userStory.getSection().getId()
        );
    }
}