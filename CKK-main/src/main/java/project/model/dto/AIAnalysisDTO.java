package project.model.dto;

import java.time.LocalDateTime;

public class AIAnalysisDTO {
    private Long id;
    private Long testSuiteId;
    private String prompt;
    private String aiResponse;
    private LocalDateTime createdAt;

    // Конструкторы
    public AIAnalysisDTO() {}

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(Long testSuiteId) { this.testSuiteId = testSuiteId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}