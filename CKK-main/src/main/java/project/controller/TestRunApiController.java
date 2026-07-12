package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.model.TestRun;
import project.model.TestRunTestCase;
import project.model.dto.*;
import project.service.TestRunService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestRunApiController {

    @Autowired
    private TestRunService testRunService;

    @GetMapping("/projects/{projectId}/test-runs")
    public List<TestRunDTO> getTestRunsByProject(@PathVariable Long projectId) {
        return testRunService.getTestRunsByProjectId(projectId);
    }

    // НОВЫЙ МЕТОД: Получение детальной информации о тест-ране
    @GetMapping("/test-runs/{id}/detailed")
    public ResponseEntity<Map<String, Object>> getTestRunDetailed(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestRunDTO testRun = testRunService.getTestRunDetailed(id);
            if (testRun != null) {
                response.put("success", true);
                response.put("testRun", testRun);
            } else {
                response.put("success", false);
                response.put("message", "Тест-ран не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/projects/{projectId}/test-runs")
    public ResponseEntity<Map<String, Object>> createTestRun(
            @PathVariable Long projectId,
            @RequestBody CreateTestRunRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            TestRunDTO testRun = testRunService.createTestRun(
                    projectId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getExecutorName(),
                    request.getCreatorName(),
                    request.getTestCaseIds(),
                    request.getTestSuiteIds());

            if (testRun != null) {
                response.put("success", true);
                response.put("testRun", testRun);
                response.put("message", "Тест-ран создан успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при создании тест-рана");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ОБНОВЛЕННЫЙ МЕТОД: Обновление тест-рана с безопасной обработкой enum
    @PutMapping("/test-runs/{id}")
    public ResponseEntity<Map<String, Object>> updateTestRun(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        Map<String, Object> response = new HashMap<>();
        try {
            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            String executorName = (String) payload.get("executorName");
            String creatorName = (String) payload.get("creatorName");
            String statusStr = (String) payload.get("status");

            // Безопасное получение статуса
            TestRun.Status status = null;
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                status = TestRun.Status.fromString(statusStr);
            }

            List<Integer> testCaseIdsToAdd = (List<Integer>) payload.get("testCaseIdsToAdd");
            List<Integer> testCaseIdsToRemove = (List<Integer>) payload.get("testCaseIdsToRemove");

            TestRunDTO updatedTestRun = testRunService.updateTestRun(
                    id, title, description, executorName, creatorName, status,
                    testCaseIdsToAdd, testCaseIdsToRemove);

            if (updatedTestRun != null) {
                response.put("success", true);
                response.put("testRun", updatedTestRun);
                response.put("message", "Тест-ран обновлен успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Тест-ран не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/test-runs/{id}/status")
    public ResponseEntity<Map<String, Object>> updateTestRunStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        Map<String, Object> response = new HashMap<>();
        try {
            String statusStr = payload.get("status");
            TestRun.Status status = TestRun.Status.fromString(statusStr);

            TestRunDTO testRun = testRunService.updateTestRunStatus(id, status);

            if (testRun != null) {
                response.put("success", true);
                response.put("testRun", testRun);
            } else {
                response.put("success", false);
                response.put("message", "Тест-ран не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/test-runs/{testRunId}/test-cases/{testCaseId}")
    public ResponseEntity<Map<String, Object>> updateTestCaseStatus(
            @PathVariable Long testRunId,
            @PathVariable Long testCaseId,
            @RequestBody Map<String, String> payload) {

        Map<String, Object> response = new HashMap<>();
        try {
            String statusStr = payload.get("status");
            TestRunTestCase.TestCaseStatus status = TestRunTestCase.TestCaseStatus.fromString(statusStr);
            String comment = payload.get("comment");

            TestRunTestCaseDTO testRunTestCase = testRunService.updateTestCaseStatus(
                    testRunId, testCaseId, status, comment);

            if (testRunTestCase != null) {
                response.put("success", true);
                response.put("testRunTestCase", testRunTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден в тест-ране");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/test-runs/{id}")
    public ResponseEntity<Map<String, Object>> deleteTestRun(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = testRunService.deleteTestRun(id);
            response.put("success", deleted);
            response.put("message", deleted ? "Тест-ран удален успешно" : "Тест-ран не найден");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/projects/{projectId}/test-runs/search")
    public List<TestRunDTO> searchTestRuns(@PathVariable Long projectId, @RequestParam String term) {
        return testRunService.searchTestRuns(projectId, term);
    }
}