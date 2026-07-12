package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.CommentType;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long testCaseId;

    // НОВЫЕ ПОЛЯ:
    private Long userId;
    private String userUsername;
    private String userFullName;
    private CommentType commentType;

    public CommentDTO() {}

    public CommentDTO(Long id, String content, LocalDateTime createdAt, LocalDateTime updatedAt,
                      Long testCaseId, Long userId, String userUsername, String userFullName,
                      CommentType commentType) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.testCaseId = testCaseId;
        this.userId = userId;
        this.userUsername = userUsername;
        this.userFullName = userFullName;
        this.commentType = commentType;
    }
}