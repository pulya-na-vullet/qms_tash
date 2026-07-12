package project.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TagDTO {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
    private Long projectId;

    public TagDTO() {}

    public TagDTO(Long id, String name, String color, LocalDateTime createdAt, Long projectId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
        this.projectId = projectId;
    }
}