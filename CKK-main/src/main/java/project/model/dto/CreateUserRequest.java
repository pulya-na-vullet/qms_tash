package project.model.dto;

import lombok.Getter;
import lombok.Setter;
import project.model.Role;

import java.util.List;

@Getter
@Setter
public class CreateUserRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private List<Role> roles;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String password, String fullName,
                             String email, List<Role> roles) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
    }
}