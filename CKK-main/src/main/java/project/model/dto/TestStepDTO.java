package project.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestStepDTO {
    private Long id;
    private Integer stepNumber;
    private String action;
    private String expectedResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long testCaseId;

    public TestStepDTO() {}

    public TestStepDTO(Long id, Integer stepNumber, String action, String expectedResult,
                       LocalDateTime createdAt, LocalDateTime updatedAt, Long testCaseId) {
        this.id = id;
        this.stepNumber = stepNumber;
        this.action = action;
        this.expectedResult = expectedResult;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.testCaseId = testCaseId;
    }
}