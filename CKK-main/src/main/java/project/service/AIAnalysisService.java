package project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import project.model.AIAnalysis;
import project.model.Comment;
import project.model.TestCase;
import project.model.TestSuite;
import project.model.dto.UserStoryDTO;
import project.repository.AIAnalysisRepository;
import project.repository.CommentRepository;
import project.repository.TestCaseRepository;
import project.repository.TestSuiteRepository;

import java.time.LocalDateTime;
import java.util.*;

import static project.model.CommentType.AI_GENERATED;

@Service
public class AIAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisService.class);

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserStoryService userStoryService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;

    private final IAMTokenService iamTokenService;

    // Настройки Yandex GPT
    @Value("${yandex.gpt.folder-id}")
    private String folderId;

    @Value("${yandex.gpt.model:yandexgpt}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Внедряем зависимость через конструктор
    public AIAnalysisService(IAMTokenService iamTokenService) {
        this.iamTokenService = iamTokenService;
    }

    public Map<String, Object> analyzeTestSuite(Long testSuiteId) {
        Map<String, Object> response = new HashMap<>();

        try {
            TestSuite testSuite = testSuiteRepository.findById(testSuiteId).orElse(null);
            if (testSuite == null) {
                response.put("success", false);
                response.put("message", "Тест-сьют не найден");
                return response;
            }

            List<UserStoryDTO> userStories = userStoryService.getUserStoriesByProjectId(testSuite.getProject().getId());
            List<TestCase> testCases = testCaseRepository.findByTestSuiteIdOrderByCreatedAtAsc(testSuiteId);

            if (testCases.isEmpty()) {
                response.put("success", false);
                response.put("message", "В тест-сьюте нет тест-кейсов для анализа");
                return response;
            }

            String prompt = buildAnalysisPrompt(userStories, testCases);

            AIAnalysis aiAnalysis = new AIAnalysis();
            aiAnalysis.setTestSuite(testSuite);
            aiAnalysis.setPrompt(prompt);
            aiAnalysis = aiAnalysisRepository.save(aiAnalysis);

            logger.info("Starting AI analysis for test suite: {} with {} test cases",
                    testSuite.getName(), testCases.size());

            String aiResponse = callYandexGptAPI(prompt);
            aiAnalysis.setAiResponse(aiResponse);
            aiAnalysisRepository.save(aiAnalysis);

            processAIResponse(aiResponse, testCases);

            logger.info("AI analysis completed successfully for test suite: {}", testSuite.getName());

            response.put("success", true);
            response.put("message", "Анализ тест-кейсов ИИ проведен");
            response.put("analysisId", aiAnalysis.getId());

        } catch (Exception e) {
            logger.error("Error during AI analysis for test suite {}: {}", testSuiteId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Ошибка при анализе тест-сьюта: " + e.getMessage());
        }

        return response;
    }

    private String buildAnalysisPrompt(List<UserStoryDTO> userStories, List<TestCase> testCases) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Проанализируй тест-кейсы на соответствие User Stories. Вот данные:\n\n");

        prompt.append("USER STORIES:\n");
        for (UserStoryDTO us : userStories) {
            prompt.append("- ID: ").append(us.getId()).append(", Название: ").append(us.getName()).append("\n");
        }

        prompt.append("\nTEST CASES:\n");
        for (TestCase tc : testCases) {
            prompt.append("--- Test Case ID: ").append(tc.getId()).append(" ---\n");
            prompt.append("Название: ").append(tc.getName()).append("\n");
            if (tc.getDescription() != null && !tc.getDescription().isEmpty()) {
                prompt.append("Описание: ").append(tc.getDescription()).append("\n");
            }
            if (tc.getPreconditions() != null && !tc.getPreconditions().isEmpty()) {
                prompt.append("Предусловия: ").append(tc.getPreconditions()).append("\n");
            }
            if (tc.getSteps() != null && !tc.getSteps().isEmpty()) {
                prompt.append("Шаги:\n");
                tc.getSteps().forEach(step -> {
                    prompt.append("  ").append(step.getStepNumber()).append(". ").append(step.getAction()).append("\n");
                    if (step.getExpectedResult() != null && !step.getExpectedResult().isEmpty()) {
                        prompt.append("     Ожидаемый результат: ").append(step.getExpectedResult()).append("\n");
                    }
                });
            }
            prompt.append("\n");
        }

        prompt.append("\nЗАДАЧА:\n");
        prompt.append("Для каждого тест-кейса (указывая его ID) определи, соответствует ли он какой-либо User Story.\n");
        prompt.append("Если соответствует - укажи какие именно User Story и обоснуй соответствие.\n");
        prompt.append("Если не соответствует - напиши, что тест-кейс не соответствует ни одной User Story.\n");
        prompt.append("Формат ответа для каждого тест-кейса - СТРОГО соблюдай:\n");
        prompt.append("[ID_ТЕСТ_КЕЙСА]: [комментарий анализа]\n\n");
        prompt.append("Пример:\n");
        prompt.append("1: Этот тест-кейс соответствует User Story 'Успешный запрос в смежную систему' (ID: 5), так как проверяет функционал запроса во внешнюю систему.\n");
        prompt.append("2: Тест-кейс не соответствует ни одной User Story.\n");
        prompt.append("ВАЖНО: Всегда начинай строку с ID тест-кейса, затем двоеточие, затем анализ. Не добавляй другие символы перед ID.\n");

        return prompt.toString();
    }

    private String callYandexGptAPI(String prompt) {
        String iamToken;
        try {
            iamToken = iamTokenService.getIamToken();
        } catch (Exception e) {
            logger.error("Failed to get IAM token for AI analysis: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить токен доступа к Яндекс GPT: " + e.getMessage());
        }

        if (iamToken.isEmpty() || folderId.isEmpty()) {
            throw new RuntimeException("Не настроены учетные данные для Яндекс GPT");
        }

        String apiUrl = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";

        // Формируем тело запроса в формате Yandex GPT
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("modelUri", "gpt://" + folderId + "/" + model + "/latest");

        Map<String, Object> completionOptions = new HashMap<>();
        completionOptions.put("stream", false);
        completionOptions.put("temperature", 0.3);
        completionOptions.put("maxTokens", "2000");
        requestBody.put("completionOptions", completionOptions);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("text", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        try {
            logger.debug("Sending request to Yandex GPT API for analysis");

            // Выполняем синхронный POST-запрос
            String jsonResponse = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + iamToken)
                    .header("Content-Type", "application/json")
                    .header("x-folder-id", folderId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Блокирующий вызов

            if (jsonResponse == null) {
                throw new RuntimeException("Пустой ответ от Yandex GPT API");
            }

            try {
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode alternatives = root.path("result").path("alternatives");

                if (alternatives.isArray() && alternatives.size() > 0) {
                    String responseText = alternatives.get(0)
                            .path("message")
                            .path("text")
                            .asText();
                    logger.debug("Successfully received response from Yandex GPT API");
                    return responseText;
                } else {
                    throw new RuntimeException("Нет альтернативных ответов в результате");
                }
            } catch (Exception e) {
                logger.error("Error parsing Yandex GPT response: {}", jsonResponse);
                throw new RuntimeException("Не удалось извлечь текст из ответа Yandex GPT", e);
            }
        } catch (Exception e) {
            logger.error("Error calling Yandex GPT API: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обращении к API Яндекс GPT: " + e.getMessage(), e);
        }
    }

    private void processAIResponse(String aiResponse, List<TestCase> testCases) {
        logger.info("Processing AI response for {} test cases", testCases.size());

        String[] lines = aiResponse.split("\n");
        int processedCount = 0;

        for (TestCase testCase : testCases) {
            String caseId = String.valueOf(testCase.getId());
            String commentForCase = findCommentForTestCase(lines, caseId);
            if (commentForCase != null && !commentForCase.isEmpty()) {
                createAIComment(testCase, commentForCase);
                processedCount++;
            } else {
                createAIComment(testCase, "ИИ не смог проанализировать соответствие User Stories.");
                logger.warn("No AI comment found for test case ID: {}", caseId);
            }
        }

        logger.info("Processed AI comments for {}/{} test cases", processedCount, testCases.size());
    }

    private String findCommentForTestCase(String[] lines, String caseId) {
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith(caseId + ":") ||
                    trimmedLine.startsWith(caseId + " :") ||
                    trimmedLine.matches("^" + caseId + "[\\s:].*")) {

                int colonIndex = trimmedLine.indexOf(':');
                if (colonIndex >= 0 && colonIndex < trimmedLine.length() - 1) {
                    return trimmedLine.substring(colonIndex + 1).trim();
                }
            }
        }
        return searchInAllText(lines, caseId);
    }

    private String searchInAllText(String[] lines, String caseId) {
        StringBuilder relevantText = new StringBuilder();
        boolean foundCase = false;
        boolean collectingText = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            boolean isNewTestCaseLine = trimmedLine.matches("^\\d+[:\\s].*");

            if (isNewTestCaseLine && trimmedLine.startsWith(caseId + ":")) {
                foundCase = true;
                collectingText = true;
                int colonIndex = trimmedLine.indexOf(':');
                if (colonIndex >= 0) {
                    relevantText.append(trimmedLine.substring(colonIndex + 1).trim());
                }
                continue;
            }

            if (isNewTestCaseLine && !trimmedLine.startsWith(caseId + ":")) {
                if (collectingText) break;
                continue;
            }

            if (collectingText) {
                relevantText.append(" ").append(trimmedLine);
            }
        }

        String result = relevantText.toString().trim();
        return (foundCase && !result.isEmpty()) ? result : null;
    }

    private void createAIComment(TestCase testCase, String content) {
        try {
            Comment comment = new Comment();
            comment.setTestCase(testCase);
            comment.setContent(content);
            comment.setCommentType(AI_GENERATED);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            comment.setUser(null);
            commentRepository.save(comment);

            logger.debug("Created AI comment for test case ID: {}", testCase.getId());
        } catch (Exception e) {
            logger.error("Error creating AI comment for test case {}: {}", testCase.getId(), e.getMessage());
        }
    }

    /**
     * Дополнительный метод для получения статуса анализа
     */
    public Map<String, Object> getAnalysisStatus(Long analysisId) {
        Map<String, Object> status = new HashMap<>();
        try {
            AIAnalysis analysis = aiAnalysisRepository.findById(analysisId).orElse(null);
            if (analysis != null) {
                status.put("exists", true);
                status.put("testSuite", analysis.getTestSuite().getName());
                status.put("createdAt", analysis.getCreatedAt());
                status.put("hasResponse", analysis.getAiResponse() != null);
            } else {
                status.put("exists", false);
            }
        } catch (Exception e) {
            logger.error("Error getting analysis status for ID {}: {}", analysisId, e.getMessage());
            status.put("error", e.getMessage());
        }
        return status;
    }
}