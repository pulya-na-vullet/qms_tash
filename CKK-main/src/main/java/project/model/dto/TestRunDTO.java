// Обновленный TestRunDTO.java
package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.TestRun;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TestRunDTO {
    private Long id;
    private String title;
    private String description;
    private String executorName;
    private String creatorName;
    private TestRun.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long projectId;
    private List<TestRunTestCaseDTO> testCases;
    private TestRunStatsDTO stats; // Новая статистика

    public TestRunDTO() {}

    public TestRunDTO(Long id, String title, String description, String executorName,
                      String creatorName, TestRun.Status status, LocalDateTime createdAt,
                      LocalDateTime updatedAt, Long projectId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.executorName = executorName;
        this.creatorName = creatorName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.projectId = projectId;
    }
}