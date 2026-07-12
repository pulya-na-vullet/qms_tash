// TestCaseReviewDTO.java
package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TestCaseReviewDTO {
    private Long id;
    private Long testCaseId;
    private String reviewResult;
    private Integer overallScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestCaseReviewDTO() {}

    public TestCaseReviewDTO(Long id, Long testCaseId, String reviewResult,
                             Integer overallScore, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.testCaseId = testCaseId;
        this.reviewResult = reviewResult;
        this.overallScore = overallScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}