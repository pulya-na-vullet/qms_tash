package project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название тест-кейса не может быть пустым")
    @Size(max = 255, message = "Название тест-кейса не может превышать 255 символов")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "preconditions", columnDefinition = "TEXT")
    private String preconditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    // *** СВЯЗЬ С ТЕГАМИ ***
    @ManyToMany
    @JoinTable(
            name = "test_case_tags",
            joinColumns = @JoinColumn(name = "test_case_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    private List<TestStep> steps;

    // *** СВЯЗЬ ЧЕРЕЗ ПРОМЕЖУТОЧНУЮ СУЩНОСТЬ ***
    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestCaseUserStory> testCaseUserStories;

    // *** НОВАЯ СВЯЗЬ С КОММЕНТАРИЯМИ ***
    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Comment> comments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        DRAFT, REVIEW, ACTIVE, ARCHIVE
    }

    // Constructors
    public TestCase() {}

    public TestCase(String name, TestSuite testSuite) {
        this.name = name;
        this.testSuite = testSuite;
    }
}