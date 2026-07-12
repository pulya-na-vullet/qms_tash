// TokenMonitorController.java
package project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.service.IAMTokenService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TokenMonitorController {

    private final IAMTokenService iamTokenService;

    public TokenMonitorController(IAMTokenService iamTokenService) {
        this.iamTokenService = iamTokenService;
    }

    @GetMapping("/api/token/status")
    public Map<String, Object> getTokenStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("tokenValid", iamTokenService.isTokenValid());
        status.put("expiryTime", iamTokenService.getTokenExpiryTime());
        status.put("currentTime", LocalDateTime.now());
        return status;
    }

    @GetMapping("/api/token/refresh")
    public Map<String, String> refreshToken() {
        iamTokenService.refreshToken();
        return Map.of("status", "Token refresh initiated");
    }
}