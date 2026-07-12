// src/main/java/project/model/TestCaseUserStory.java
package project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "test_case_user_stories")
public class TestCaseUserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_story_id", nullable = false)
    private UserStory userStory;

    // *** НОВОЕ ПОЛЕ ***
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true) // Можно сделать nullable = false после заполнения
    private Project project;

    public TestCaseUserStory() {}

    // *** НОВЫЙ КОНСТРУКТОР ***
    public TestCaseUserStory(TestCase testCase, UserStory userStory) {
        this.testCase = testCase;
        this.userStory = userStory;
        // *** УСТАНАВЛИВАЕМ PROJECT ***
        if (testCase != null && testCase.getTestSuite() != null) {
            this.project = testCase.getTestSuite().getProject();
        }
    }

    // *** НОВЫЙ КОНСТРУКТОР С PROJECT ***
    public TestCaseUserStory(TestCase testCase, UserStory userStory, Project project) {
        this.testCase = testCase;
        this.userStory = userStory;
        this.project = project;
    }
}