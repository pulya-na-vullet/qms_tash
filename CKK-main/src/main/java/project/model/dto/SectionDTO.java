package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class SectionDTO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long projectId;

    public SectionDTO() {}

    public SectionDTO(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, Long projectId) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.projectId = projectId;
    }
}