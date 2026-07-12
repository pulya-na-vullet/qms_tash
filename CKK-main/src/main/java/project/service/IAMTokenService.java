// IAMTokenService.java
package project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class IAMTokenService {

    @Value("${yandex.gpt.api-key}")
    private String apiKey;

    private final AtomicReference<String> currentIamToken = new AtomicReference<>("");
    private final AtomicReference<LocalDateTime> tokenExpiryTime = new AtomicReference<>();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String IAM_TOKEN_URL = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
    private static final long TOKEN_EXPIRY_HOURS = 12;
    private static final long REFRESH_INTERVAL_HOURS = 10;

    /**
     * Получение текущего IAM-токена
     */
    public String getIamToken() {
        String token = currentIamToken.get();
        if (token == null || token.isEmpty() || isTokenExpired()) {
            refreshToken();
        }
        return currentIamToken.get();
    }

    /**
     * Принудительное обновление токена
     */
    public synchronized void refreshToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("yandexPassportOauthToken", apiKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    IAM_TOKEN_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String newToken = (String) response.getBody().get("iamToken");
                currentIamToken.set(newToken);
                tokenExpiryTime.set(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));

                System.out.println("IAM token successfully updated. Expires at: " + tokenExpiryTime.get());
            } else {
                System.err.println("Failed to refresh IAM token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error refreshing IAM token: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверка истек ли токен
     */
    private boolean isTokenExpired() {
        LocalDateTime expiry = tokenExpiryTime.get();
        return expiry == null || LocalDateTime.now().isAfter(expiry);
    }

    /**
     * Автоматическое обновление токена каждые 10 часов
     */
    @Scheduled(fixedRateString = "${yandex.gpt.token-refresh-interval:36000000}") // 10 часов по умолчанию
    public void scheduledTokenRefresh() {
        System.out.println("Scheduled IAM token refresh started at: " + LocalDateTime.now());
        refreshToken();
    }

    /**
     * Получение времени истечения токена (для мониторинга)
     */
    public LocalDateTime getTokenExpiryTime() {
        return tokenExpiryTime.get();
    }

    /**
     * Проверка валидности токена
     */
    public boolean isTokenValid() {
        return !isTokenExpired() && currentIamToken.get() != null && !currentIamToken.get().isEmpty();
    }
}