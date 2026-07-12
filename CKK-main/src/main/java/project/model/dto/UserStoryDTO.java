package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserStoryDTO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long sectionId;

    public UserStoryDTO() {}

    public UserStoryDTO(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, Long sectionId) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sectionId = sectionId;
    }
}