// AIReviewService.java
package project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import project.model.TestCase;
import project.model.TestStep;
import project.model.dto.TestCaseDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AIReviewService {

    // Настройки Yandex GPT
    @Value("${yandex.gpt.folder-id}")
    private String folderId;

    // Опционально: можно использовать yandexgpt или yandexgpt-lite
    @Value("${yandex.gpt.model:yandexgpt}")
    private String model;

    private final String YANDEX_GPT_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";

    private final RestTemplate restTemplate = new RestTemplate();
    private final IAMTokenService iamTokenService;

    // Внедряем зависимость через конструктор
    public AIReviewService(IAMTokenService iamTokenService) {
        this.iamTokenService = iamTokenService;
    }

    public String reviewTestCase(TestCaseDTO testCase) {
        String iamToken = iamTokenService.getIamToken();

        if (iamToken.isEmpty() || folderId.isEmpty()) {
            return "Ошибка: Не настроены учетные данные для Яндекс GPT. Пожалуйста, проверьте настройки API.";
        }

        try {
            String prompt = buildReviewPrompt(testCase);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + iamToken);
            headers.set("x-folder-id", folderId);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("modelUri", "gpt://" + folderId + "/" + model + "/latest");
            requestBody.put("completionOptions", Map.of(
                    "stream", false,
                    "temperature", 0.3,
                    "maxTokens", 2000
            ));

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("text", prompt);
            requestBody.put("messages", new Map[]{message});

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    YANDEX_GPT_URL, HttpMethod.POST, entity, Map.class);

            return processResponse(response);

        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при выполнении AI ревью: " + e.getMessage();
        }
    }

    /**
     * Обработка ответа от API Яндекс GPT
     */
    private String processResponse(ResponseEntity<Map> response) {
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> result = response.getBody();
            Map<String, Object> responseObj = (Map<String, Object>) result.get("result");

            if (responseObj != null && responseObj.containsKey("alternatives")) {
                java.util.List<Map<String, Object>> alternatives =
                        (java.util.List<Map<String, Object>>) responseObj.get("alternatives");

                if (alternatives != null && !alternatives.isEmpty()) {
                    Map<String, Object> firstAlternative = alternatives.get(0);

                    // Исправленная часть: правильно извлекаем текст сообщения
                    if (firstAlternative.containsKey("message")) {
                        Object messageObj = firstAlternative.get("message");

                        if (messageObj instanceof Map) {
                            Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                            Object textObj = messageMap.get("text");

                            if (textObj instanceof String) {
                                return (String) textObj;
                            } else {
                                return "Ошибка: текст ответа имеет неверный формат";
                            }
                        } else {
                            return "Ошибка: структура сообщения не соответствует ожидаемой";
                        }
                    } else {
                        return "Ошибка: в ответе отсутствует поле 'message'";
                    }
                } else {
                    return "Ошибка: нет альтернативных ответов в результате";
                }
            } else {
                return "Ошибка: неверная структура ответа от API";
            }
        } else {
            return "Ошибка при обращении к API Яндекс GPT: " + response.getStatusCode();
        }
    }

    private String buildReviewPrompt(TestCaseDTO testCase) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Проанализируй тест-кейс по следующим критериям и дай развернутый ответ по каждому пункту, а также общую оценку от 1 до 10:\n\n");

        prompt.append("КРИТЕРИИ ОЦЕНКИ:\n");
        prompt.append("1. Атомарность - тест-кейс должен проверять одну конкретную функциональность\n");
        prompt.append("2. Наличие и качество тегов - должны быть соответствующие теги для категоризации\n");
        prompt.append("3. Наличие и полнота описания\n");
        prompt.append("4. Наличие заполненных шагов с действиями и ожидаемыми результатами\n");
        prompt.append("5. Наличие и адекватность предусловий\n");
        prompt.append("6. Наличие связанных пользовательских историй\n\n");

        prompt.append("ИНФОРМАЦИЯ О ТЕСТ-КЕЙСЕ:\n");
        prompt.append("Название: ").append(testCase.getName()).append("\n");
        prompt.append("Описание: ").append(testCase.getDescription() != null ? testCase.getDescription() : "отсутствует").append("\n");
        prompt.append("Предусловия: ").append(testCase.getPreconditions() != null ? testCase.getPreconditions() : "отсутствуют").append("\n");
        prompt.append("Приоритет: ").append(testCase.getPriority()).append("\n");
        prompt.append("Статус: ").append(testCase.getStatus()).append("\n");

        // Теги
        if (testCase.getTags() != null && !testCase.getTags().isEmpty()) {
            String tags = testCase.getTags().stream()
                    .map(tag -> tag.getName())
                    .collect(Collectors.joining(", "));
            prompt.append("Теги: ").append(tags).append("\n");
        } else {
            prompt.append("Теги: отсутствуют\n");
        }

        // Шаги
        if (testCase.getSteps() != null && !testCase.getSteps().isEmpty()) {
            prompt.append("Шаги тест-кейса:\n");
            for (var step : testCase.getSteps()) {
                prompt.append(step.getStepNumber()).append(". Действие: ").append(step.getAction());
                if (step.getExpectedResult() != null && !step.getExpectedResult().isEmpty()) {
                    prompt.append(" | Ожидаемый результат: ").append(step.getExpectedResult());
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("Шаги: отсутствуют\n");
        }

        // User Stories
        if (testCase.getUserStories() != null && !testCase.getUserStories().isEmpty()) {
            String userStories = testCase.getUserStories().stream()
                    .map(us -> us.getName())
                    .collect(Collectors.joining(", "));
            prompt.append("Связанные User Stories: ").append(userStories).append("\n");
        } else {
            prompt.append("Связанные User Stories: отсутствуют\n");
        }

        prompt.append("\nФОРМАТ ОТВЕТА:\n");
        prompt.append("Пожалуйста, предоставь ответ в следующем формате:\n");
        prompt.append("1. Атомарность: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("2. Теги: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("3. Описание: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("4. Шаги: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("5. Предусловия: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("6. User Stories: [оценка/10] - [развернутый комментарий]\n");
        prompt.append("ОБЩАЯ ОЦЕНКА: [средняя оценка]/10\n");
        prompt.append("РЕКОМЕНДАЦИИ: [общие рекомендации по улучшению тест-кейса]\n");

        return prompt.toString();
    }
}