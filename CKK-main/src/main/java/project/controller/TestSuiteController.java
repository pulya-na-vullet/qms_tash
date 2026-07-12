package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.model.dto.TestSuiteDTO;
import project.service.TestSuiteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TestSuiteController {

    @Autowired
    private TestSuiteService testSuiteService;

    // ========== API ENDPOINTS ==========

    // Получить все тест-сьюты проекта
    @ResponseBody
    @GetMapping("/api/projects/{projectId}/test-suites")
    public List<TestSuiteDTO> getTestSuitesByProject(@PathVariable Long projectId) {
        return testSuiteService.getTestSuitesByProjectId(projectId);
    }

    // Создать тест-сьют
    @ResponseBody
    @PostMapping("/api/projects/{projectId}/test-suites")
    public Map<String, Object> createTestSuite(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                TestSuiteDTO createdTestSuite = testSuiteService.createTestSuite(projectId, name.trim());
                if (createdTestSuite != null) {
                    response.put("success", true);
                    response.put("message", "Тест-сьют создан успешно!");
                    response.put("testSuite", createdTestSuite);
                } else {
                    response.put("success", false);
                    response.put("message", "Проект не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название тест-сьюта не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при создании тест-сьюта: " + e.getMessage());
        }
        return response;
    }

    // Получить тест-сьют по ID
    @ResponseBody
    @GetMapping("/api/test-suites/{id}")
    public Map<String, Object> getTestSuite(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestSuiteDTO testSuite = testSuiteService.getTestSuiteById(id).orElse(null);
            if (testSuite != null) {
                response.put("success", true);
                response.put("testSuite", testSuite);
            } else {
                response.put("success", false);
                response.put("message", "Тест-сьют не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении тест-сьюта: " + e.getMessage());
        }
        return response;
    }

    // Обновить тест-сьют
    @ResponseBody
    @PutMapping("/api/test-suites/{id}")
    public Map<String, Object> updateTestSuite(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                TestSuiteDTO updatedTestSuite = testSuiteService.updateTestSuite(id, name.trim());
                if (updatedTestSuite != null) {
                    response.put("success", true);
                    response.put("message", "Тест-сьют обновлен успешно!");
                    response.put("testSuite", updatedTestSuite);
                } else {
                    response.put("success", false);
                    response.put("message", "Тест-сьют не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название тест-сьюта не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении тест-сьюта: " + e.getMessage());
        }
        return response;
    }

    // Удалить тест-сьют
    @ResponseBody
    @DeleteMapping("/api/test-suites/{id}")
    public Map<String, Object> deleteTestSuite(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = testSuiteService.deleteTestSuite(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Тест-сьют удален успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Тест-сьют не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении тест-сьюта: " + e.getMessage());
        }
        return response;
    }

    // Поиск тест-сьютов
    @ResponseBody
    @GetMapping("/api/projects/{projectId}/test-suites/search")
    public List<TestSuiteDTO> searchTestSuites(@PathVariable Long projectId, @RequestParam String term) {
        return testSuiteService.searchTestSuites(projectId, term);
    }
}