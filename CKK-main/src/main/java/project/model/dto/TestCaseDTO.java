package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.TestCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class TestCaseDTO {
    private Long id;
    private String name;
    private String description;
    private String preconditions;
    private TestCase.Priority priority;
    private TestCase.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long testSuiteId;
    private Set<TagDTO> tags;
    private List<TestStepDTO> steps;
    private Set<UserStoryDTO> userStories;
    // *** ИЗМЕНЕНО НА Set ***
    private Set<CommentDTO> comments;

    public TestCaseDTO() {}

    public TestCaseDTO(Long id, String name, String description, String preconditions,
                       TestCase.Priority priority, TestCase.Status status,
                       LocalDateTime createdAt, LocalDateTime updatedAt, Long testSuiteId,
                       Set<TagDTO> tags, List<TestStepDTO> steps, Set<UserStoryDTO> userStories,
                       Set<CommentDTO> comments) { // Изменен тип параметра
        this.id = id;
        this.name = name;
        this.description = description;
        this.preconditions = preconditions;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.testSuiteId = testSuiteId;
        this.tags = tags;
        this.steps = steps;
        this.userStories = userStories;
        this.comments = comments;
    }
}