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
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название тега не может быть пустым")
    @Size(max = 100, message = "Название тега не может превышать 100 символов")
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 7, message = "Цвет тега должен быть в формате HEX")
    @Column(name = "color", length = 7)
    private String color = "#6c757d";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // *** СВЯЗЬ С ТЕСТ-КЕЙСАМИ ***
    @ManyToMany(mappedBy = "tags")
    private Set<TestCase> testCases;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Tag() {}

    public Tag(String name, Project project) {
        this.name = name;
        this.project = project;
    }

    public Tag(String name, Project project, String color) {
        this.name = name;
        this.project = project;
        this.color = color;
    }
}