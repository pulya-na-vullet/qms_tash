package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.TestRunTestCase;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestRunTestCaseDTO {
    private Long id;
    private Long testRunId;
    private TestCaseDTO testCase;
    private TestRunTestCase.TestCaseStatus status;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestRunTestCaseDTO() {}

    public TestRunTestCaseDTO(Long id, Long testRunId, TestCaseDTO testCase, 
                             TestRunTestCase.TestCaseStatus status, String comment,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.testRunId = testRunId;
        this.testCase = testCase;
        this.status = status;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}