package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.model.*;
import project.model.dto.TestCaseDTO;
import project.model.dto.TagDTO;
import project.model.dto.TestStepDTO;
import project.model.dto.UserStoryDTO;
import project.model.dto.CommentDTO;
import project.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestCaseService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestStepRepository testStepRepository;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private TestCaseUserStoryRepository testCaseUserStoryRepository;

    @Autowired
    private CommentRepository commentRepository;

    // *** НОВЫЙ МЕТОД ДЛЯ CommentService ***
    public TestCase findById(Long id) {
        return testCaseRepository.findById(id).orElse(null);
    }

    public List<TestCaseDTO> getTestCasesByTestSuiteId(Long testSuiteId) {
        List<TestCase> testCases = testCaseRepository.findByTestSuiteIdOrderByCreatedAtAsc(testSuiteId);
        return testCases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public TestCaseDTO createTestCase(Long testSuiteId, String name, String description, String preconditions) {
        Optional<TestSuite> testSuiteOptional = testSuiteRepository.findById(testSuiteId);
        if (testSuiteOptional.isPresent()) {
            TestCase testCase = new TestCase(name.trim(), testSuiteOptional.get());
            testCase.setDescription(description);
            testCase.setPreconditions(preconditions);
            TestCase savedTestCase = testCaseRepository.save(testCase);
            return convertToDTO(savedTestCase);
        }
        return null;
    }

    public Optional<TestCaseDTO> getTestCaseById(Long id) {
        return testCaseRepository.findById(id).map(this::convertToDTO);
    }

    public TestCaseDTO updateTestCase(Long id, String name, String description, String preconditions,
                                      TestCase.Priority priority, TestCase.Status status) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(id);
        if (testCaseOptional.isPresent()) {
            TestCase testCase = testCaseOptional.get();
            if (name != null) testCase.setName(name.trim());
            if (description != null) testCase.setDescription(description);
            if (preconditions != null) testCase.setPreconditions(preconditions);
            if (priority != null) testCase.setPriority(priority);
            if (status != null) testCase.setStatus(status);
            TestCase updatedTestCase = testCaseRepository.save(testCase);
            return convertToDTO(updatedTestCase);
        }
        return null;
    }

    public boolean deleteTestCase(Long id) {
        if (testCaseRepository.existsById(id)) {
            // Удаляем связи с User Stories
            testCaseUserStoryRepository.deleteByTestCaseId(id);
            // Удаляем комментарии
            commentRepository.deleteByTestCaseId(id);
            // Удаляем шаги
            testStepRepository.deleteByTestCaseId(id);
            // Удаляем тест-кейс
            testCaseRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public TestCaseDTO cloneTestCase(Long testCaseId) {
        Optional<TestCase> originalTestCaseOptional = testCaseRepository.findById(testCaseId);
        if (originalTestCaseOptional.isPresent()) {
            TestCase original = originalTestCaseOptional.get();

            TestCase cloned = new TestCase(original.getName() + " (Копия)", original.getTestSuite());
            cloned.setDescription(original.getDescription());
            cloned.setPreconditions(original.getPreconditions());
            cloned.setPriority(original.getPriority());
            cloned.setStatus(TestCase.Status.DRAFT);

            // Клонируем теги
            if (original.getTags() != null) {
                cloned.setTags(new HashSet<>(original.getTags()));
            }

            TestCase savedCloned = testCaseRepository.save(cloned);

            // Клонируем шаги
            if (original.getSteps() != null) {
                for (TestStep step : original.getSteps()) {
                    TestStep clonedStep = new TestStep();
                    clonedStep.setStepNumber(step.getStepNumber());
                    clonedStep.setAction(step.getAction());
                    clonedStep.setExpectedResult(step.getExpectedResult());
                    clonedStep.setTestCase(savedCloned);
                    testStepRepository.save(clonedStep);
                }
            }

            // Клонируем связи с User Stories
            if (original.getTestCaseUserStories() != null) {
                for (TestCaseUserStory tus : original.getTestCaseUserStories()) {
                    TestCaseUserStory clonedTUS = new TestCaseUserStory();
                    clonedTUS.setTestCase(savedCloned);
                    clonedTUS.setUserStory(tus.getUserStory());
                    testCaseUserStoryRepository.save(clonedTUS);
                }
            }

            // Клонируем комментарии (без привязки к пользователю)
            if (original.getComments() != null) {
                for (Comment comment : original.getComments()) {
                    Comment clonedComment = new Comment();
                    clonedComment.setContent("[Копия] " + comment.getContent());
                    clonedComment.setTestCase(savedCloned);
                    commentRepository.save(clonedComment);
                }
            }

            return convertToDTO(savedCloned);
        }
        return null;
    }

    public List<TestCaseDTO> searchTestCases(Long testSuiteId, String searchTerm) {
        List<TestCase> testCases = testCaseRepository.findByTestSuiteIdAndNameContainingIgnoreCaseOrderByCreatedAtAsc(
                testSuiteId, searchTerm);
        return testCases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Методы для работы с тегами
    public TestCaseDTO addTagToTestCase(Long testCaseId, Long tagId) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        Optional<Tag> tagOptional = tagRepository.findById(tagId);

        if (testCaseOptional.isPresent() && tagOptional.isPresent()) {
            TestCase testCase = testCaseOptional.get();
            Set<Tag> tags = testCase.getTags();
            if (tags == null) {
                tags = new HashSet<>();
                testCase.setTags(tags);
            }
            tags.add(tagOptional.get());
            testCaseRepository.save(testCase);
            return convertToDTO(testCase);
        }
        return null;
    }

    public TestCaseDTO removeTagFromTestCase(Long testCaseId, Long tagId) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        if (testCaseOptional.isPresent()) {
            TestCase testCase = testCaseOptional.get();
            Set<Tag> tags = testCase.getTags();
            if (tags != null) {
                tags.removeIf(tag -> tag.getId().equals(tagId));
                testCaseRepository.save(testCase);
                return convertToDTO(testCase);
            }
        }
        return null;
    }

    // Методы для работы с шагами
    public TestCaseDTO addStepToTestCase(Long testCaseId, Integer stepNumber, String action, String expectedResult) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        if (testCaseOptional.isPresent()) {
            TestStep step = new TestStep();
            step.setStepNumber(stepNumber);
            step.setAction(action);
            step.setExpectedResult(expectedResult);
            step.setTestCase(testCaseOptional.get());
            testStepRepository.save(step);

            return getTestCaseById(testCaseId).orElse(null);
        }
        return null;
    }

    public TestCaseDTO updateStep(Long stepId, Integer stepNumber, String action, String expectedResult) {
        Optional<TestStep> stepOptional = testStepRepository.findById(stepId);
        if (stepOptional.isPresent()) {
            TestStep step = stepOptional.get();
            if (stepNumber != null) step.setStepNumber(stepNumber);
            if (action != null) step.setAction(action);
            if (expectedResult != null) step.setExpectedResult(expectedResult);
            testStepRepository.save(step);

            return getTestCaseById(step.getTestCase().getId()).orElse(null);
        }
        return null;
    }

    public TestCaseDTO removeStep(Long stepId) {
        Optional<TestStep> stepOptional = testStepRepository.findById(stepId);
        if (stepOptional.isPresent()) {
            TestStep step = stepOptional.get();
            Long testCaseId = step.getTestCase().getId();
            testStepRepository.deleteById(stepId);
            return getTestCaseById(testCaseId).orElse(null);
        }
        return null;
    }

    // Методы для работы с user stories
    public TestCaseDTO addUserStoryToTestCase(Long testCaseId, Long userStoryId) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        Optional<UserStory> userStoryOptional = userStoryRepository.findById(userStoryId);

        if (testCaseOptional.isPresent() && userStoryOptional.isPresent()) {
            TestCase testCase = testCaseOptional.get();
            UserStory userStory = userStoryOptional.get();

            // Проверяем, существует ли уже такая связь
            List<TestCaseUserStory> existingRelations = testCaseUserStoryRepository
                    .findByTestCaseIdAndUserStoryId(testCaseId, userStoryId);

            if (existingRelations.isEmpty()) {
                TestCaseUserStory testCaseUserStory = new TestCaseUserStory();
                testCaseUserStory.setTestCase(testCase);
                testCaseUserStory.setUserStory(userStory);
                testCaseUserStoryRepository.save(testCaseUserStory);
            }

            return getTestCaseById(testCaseId).orElse(null);
        }
        return null;
    }

    public TestCaseDTO removeUserStoryFromTestCase(Long testCaseId, Long userStoryId) {
        testCaseUserStoryRepository.deleteByTestCaseIdAndUserStoryId(testCaseId, userStoryId);
        return getTestCaseById(testCaseId).orElse(null);
    }

    // Методы для работы с комментариями
    public TestCaseDTO addCommentToTestCase(Long testCaseId, String content) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        if (testCaseOptional.isPresent() && content != null && !content.trim().isEmpty()) {
            TestCase testCase = testCaseOptional.get();
            Comment comment = new Comment(content.trim(), testCase);
            commentRepository.save(comment);
            return getTestCaseById(testCaseId).orElse(null);
        }
        return null;
    }

    public TestCaseDTO updateComment(Long commentId, String content) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent() && content != null && !content.trim().isEmpty()) {
            Comment comment = commentOptional.get();
            comment.setContent(content.trim());
            commentRepository.save(comment);
            return getTestCaseById(comment.getTestCase().getId()).orElse(null);
        }
        return null;
    }

    public TestCaseDTO removeComment(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (commentOptional.isPresent()) {
            Long testCaseId = commentOptional.get().getTestCase().getId();
            commentRepository.deleteById(commentId);
            return getTestCaseById(testCaseId).orElse(null);
        }
        return null;
    }

    // Метод для получения всех тест-кейсов проекта
    public List<TestCaseDTO> getTestCasesByProjectId(Long projectId) {
        List<TestCase> testCases = testCaseRepository.findByTestSuite_ProjectIdOrderByCreatedAtAsc(projectId);
        return testCases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // *** ОБНОВЛЕННЫЙ МЕТОД ПРЕОБРАЗОВАНИЯ С УЧЕТОМ ПОЛЬЗОВАТЕЛЯ ***
    private TestCaseDTO convertToDTO(TestCase testCase) {
        Set<TagDTO> tags = testCase.getTags() != null ?
                testCase.getTags().stream().map(this::convertTagToDTO).collect(Collectors.toSet()) :
                new HashSet<>();

        List<TestStepDTO> steps = testCase.getSteps() != null ?
                testCase.getSteps().stream()
                        .sorted(Comparator.comparing(TestStep::getStepNumber))
                        .map(this::convertStepToDTO)
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        // Преобразование User Stories через промежуточную сущность
        Set<UserStoryDTO> userStories = new HashSet<>();
        if (testCase.getTestCaseUserStories() != null) {
            userStories = testCase.getTestCaseUserStories().stream()
                    .map(TestCaseUserStory::getUserStory)
                    .filter(Objects::nonNull)
                    .map(this::convertUserStoryToDTO)
                    .collect(Collectors.toSet());
        }

        // Преобразование комментариев с информацией о пользователе
        Set<CommentDTO> comments = testCase.getComments() != null ?
                testCase.getComments().stream()
                        .sorted(Comparator.comparing(Comment::getCreatedAt))
                        .map(this::convertCommentToDTO)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        return new TestCaseDTO(
                testCase.getId(),
                testCase.getName(),
                testCase.getDescription(),
                testCase.getPreconditions(),
                testCase.getPriority(),
                testCase.getStatus(),
                testCase.getCreatedAt(),
                testCase.getUpdatedAt(),
                testCase.getTestSuite().getId(),
                tags,
                steps,
                userStories,
                comments
        );
    }

    private TagDTO convertTagToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getCreatedAt(),
                tag.getProject().getId()
        );
    }

    private TestStepDTO convertStepToDTO(TestStep step) {
        return new TestStepDTO(
                step.getId(),
                step.getStepNumber(),
                step.getAction(),
                step.getExpectedResult(),
                step.getCreatedAt(),
                step.getUpdatedAt(),
                step.getTestCase().getId()
        );
    }

    private UserStoryDTO convertUserStoryToDTO(UserStory userStory) {
        return new UserStoryDTO(
                userStory.getId(),
                userStory.getName(),
                userStory.getCreatedAt(),
                userStory.getUpdatedAt(),
                userStory.getSection().getId()
        );
    }

    // *** ОБНОВЛЕННЫЙ МЕТОД ПРЕОБРАЗОВАНИЯ КОММЕНТАРИЯ ***
    private CommentDTO convertCommentToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setTestCaseId(comment.getTestCase().getId());
        dto.setCommentType(comment.getCommentType());

        // Добавляем информацию о пользователе
        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
            dto.setUserUsername(comment.getUser().getUsername());
            dto.setUserFullName(comment.getUser().getFullName());
        } else {
            // Для AI комментариев
            dto.setUserUsername("AI Assistant");
            dto.setUserFullName("AI Assistant");
        }

        return dto;
    }
}