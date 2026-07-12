package project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Текст комментария не может быть пустым")
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    // НОВОЕ ПОЛЕ: Связь с пользователем
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // НОВОЕ ПОЛЕ: Тип комментария (для различения AI и ручных комментариев)
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false)
    private CommentType commentType = CommentType.MANUAL;

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
    public Comment() {}

    // *** ДОБАВЛЕННЫЙ КОНСТРУКТОР ***
    public Comment(String content, TestCase testCase) {
        this.content = content;
        this.testCase = testCase;
        this.commentType = CommentType.MANUAL;
        // user остается null - будет установлен позже
    }

    public Comment(String content, TestCase testCase, User user) {
        this.content = content;
        this.testCase = testCase;
        this.user = user;
        this.commentType = CommentType.MANUAL;
    }

    public Comment(String content, TestCase testCase, User user, CommentType commentType) {
        this.content = content;
        this.testCase = testCase;
        this.user = user;
        this.commentType = commentType;
    }

    // *** ДОПОЛНИТЕЛЬНЫЙ КОНСТРУКТОР ДЛЯ AI КОММЕНТАРИЕВ ***
    public Comment(String content, TestCase testCase, CommentType commentType) {
        this.content = content;
        this.testCase = testCase;
        this.commentType = commentType;
        // user остается null для AI комментариев
    }
}