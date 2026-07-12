package project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "test_runs")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Заголовок тест-рана не может быть пустым")
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "executor_name", nullable = false)
    private String executorName;

    @Column(name = "creator_name", nullable = false)
    private String creatorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.NOT_STARTED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestRunTestCase> testRunTestCases = new ArrayList<>();

    public enum Status {
        NOT_STARTED("Не начат"),
        IN_PROGRESS("В процессе"),
        COMPLETED("Завершен");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        // Метод для безопасного получения enum по строке
        public static Status fromString(String status) {
            if (status == null) return NOT_STARTED;
            try {
                return Status.valueOf(status.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return NOT_STARTED;
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public TestRun() {}

    public TestRun(String title, String description, String executorName, String creatorName, Project project) {
        this.title = title;
        this.description = description;
        this.executorName = executorName;
        this.creatorName = creatorName;
        this.project = project;
    }
}