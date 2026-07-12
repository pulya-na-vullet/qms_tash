package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.Role;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Set<Role> roles;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String deactivationReason;

    public UserDTO() {}

    public UserDTO(Long id, String username, String fullName, String email,
                   Set<Role> roles, boolean enabled, LocalDateTime createdAt,
                   LocalDateTime updatedAt, String deactivationReason) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deactivationReason = deactivationReason;
    }
}