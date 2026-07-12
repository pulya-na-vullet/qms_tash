package project.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeactivationRequest {
    private String reason;

    public DeactivationRequest() {}

    public DeactivationRequest(String reason) {
        this.reason = reason;
    }
}