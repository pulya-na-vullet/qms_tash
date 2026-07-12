package project.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTestRunRequest {
    private String title;
    private String description;
    private String executorName;
    private String creatorName;
    private List<Long> testCaseIds;
    private List<Long> testSuiteIds;
}