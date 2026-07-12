package project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "test_run_test_cases")
public class TestRunTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TestCaseStatus status = TestCaseStatus.NOT_RUN;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TestCaseStatus {
        NOT_RUN("Не выполнен"),
        PASSED("Успешно"),
        FAILED("Провален"),
        SKIPPED("Пропущен");

        private final String displayName;

        TestCaseStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        // Метод для безопасного получения enum по строке
        public static TestCaseStatus fromString(String status) {
            if (status == null) return NOT_RUN;
            try {
                return TestCaseStatus.valueOf(status.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return NOT_RUN;
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

    public TestRunTestCase() {}

    public TestRunTestCase(TestRun testRun, TestCase testCase) {
        this.testRun = testRun;
        this.testCase = testCase;
    }
}