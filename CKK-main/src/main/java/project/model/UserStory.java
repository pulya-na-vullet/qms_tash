package project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "user_stories")
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название user story не может быть пустым")
    @Size(max = 255, message = "Название user story не может превышать 255 символов")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // Исправленная связь - убран mappedBy, так как у нас двунаправленная связь через промежуточную таблицу
    @ManyToMany
    @JoinTable(
            name = "test_case_user_stories",
            joinColumns = @JoinColumn(name = "user_story_id"),
            inverseJoinColumns = @JoinColumn(name = "test_case_id")
    )
    private Set<TestCase> testCases;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public UserStory() {}

    public UserStory(String name, Section section) {
        this.name = name;
        this.section = section;
    }
}