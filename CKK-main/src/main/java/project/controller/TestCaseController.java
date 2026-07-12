package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.model.TestCase;
import project.model.dto.TestCaseDTO;
import project.service.TagService;
import project.service.TestCaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TagService tagService;

    // ========== API ENDPOINTS ==========

    // Получить все тест-кейсы тест-сьюта
    @ResponseBody
    @GetMapping("/api/test-suites/{testSuiteId}/test-cases")
    public List<TestCaseDTO> getTestCasesByTestSuite(@PathVariable Long testSuiteId) {
        return testCaseService.getTestCasesByTestSuiteId(testSuiteId);
    }

    // Создать тест-кейс
    @ResponseBody
    @PostMapping("/api/test-suites/{testSuiteId}/test-cases")
    public Map<String, Object> createTestCase(@PathVariable Long testSuiteId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            String preconditions = (String) payload.get("preconditions");

            if (name != null && !name.trim().isEmpty()) {
                TestCaseDTO createdTestCase = testCaseService.createTestCase(testSuiteId, name.trim(), description, preconditions);
                if (createdTestCase != null) {
                    response.put("success", true);
                    response.put("message", "Тест-кейс создан успешно!");
                    response.put("testCase", createdTestCase);
                } else {
                    response.put("success", false);
                    response.put("message", "Тест-сьют не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название тест-кейса не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при создании тест-кейса: " + e.getMessage());
        }
        return response;
    }

    // Получить тест-кейс по ID
    @ResponseBody
    @GetMapping("/api/test-cases/{id}")
    public Map<String, Object> getTestCase(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO testCase = testCaseService.getTestCaseById(id).orElse(null);
            if (testCase != null) {
                response.put("success", true);
                response.put("testCase", testCase);
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении тест-кейса: " + e.getMessage());
        }
        return response;
    }

    // Обновить тест-кейс
    @ResponseBody
    @PutMapping("/api/test-cases/{id}")
    public Map<String, Object> updateTestCase(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            String preconditions = (String) payload.get("preconditions");
            String priorityStr = (String) payload.get("priority");
            String statusStr = (String) payload.get("status");

            TestCase.Priority priority = priorityStr != null ? TestCase.Priority.valueOf(priorityStr) : null;
            TestCase.Status status = statusStr != null ? TestCase.Status.valueOf(statusStr) : null;

            TestCaseDTO updatedTestCase = testCaseService.updateTestCase(id, name, description, preconditions, priority, status);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Тест-кейс обновлен успешно!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении тест-кейса: " + e.getMessage());
        }
        return response;
    }

    // Удалить тест-кейс
    @ResponseBody
    @DeleteMapping("/api/test-cases/{id}")
    public Map<String, Object> deleteTestCase(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = testCaseService.deleteTestCase(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Тест-кейс удален успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении тест-кейса: " + e.getMessage());
        }
        return response;
    }

    // Клонировать тест-кейс
    @ResponseBody
    @PostMapping("/api/test-cases/{id}/clone")
    public Map<String, Object> cloneTestCase(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO clonedTestCase = testCaseService.cloneTestCase(id);
            if (clonedTestCase != null) {
                response.put("success", true);
                response.put("message", "Тест-кейс склонирован успешно!");
                response.put("testCase", clonedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при клонировании тест-кейса: " + e.getMessage());
        }
        return response;
    }

    // Поиск тест-кейсов
    @ResponseBody
    @GetMapping("/api/test-suites/{testSuiteId}/test-cases/search")
    public List<TestCaseDTO> searchTestCases(@PathVariable Long testSuiteId, @RequestParam String term) {
        return testCaseService.searchTestCases(testSuiteId, term);
    }

    // Добавить тег к тест-кейсу
    @ResponseBody
    @PostMapping("/api/test-cases/{testCaseId}/tags/{tagId}")
    public Map<String, Object> addTagToTestCase(@PathVariable Long testCaseId, @PathVariable Long tagId) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO updatedTestCase = testCaseService.addTagToTestCase(testCaseId, tagId);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Тег добавлен к тест-кейсу!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при добавлении тега!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при добавлении тега: " + e.getMessage());
        }
        return response;
    }

    // Удалить тег из тест-кейса
    @ResponseBody
    @DeleteMapping("/api/test-cases/{testCaseId}/tags/{tagId}")
    public Map<String, Object> removeTagFromTestCase(@PathVariable Long testCaseId, @PathVariable Long tagId) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO updatedTestCase = testCaseService.removeTagFromTestCase(testCaseId, tagId);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Тег удален из тест-кейса!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при удалении тега!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении тега: " + e.getMessage());
        }
        return response;
    }

    // Добавить шаг к тест-кейсу
    @ResponseBody
    @PostMapping("/api/test-cases/{testCaseId}/steps")
    public Map<String, Object> addStepToTestCase(@PathVariable Long testCaseId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer stepNumber = (Integer) payload.get("stepNumber");
            String action = (String) payload.get("action");
            String expectedResult = (String) payload.get("expectedResult");

            TestCaseDTO updatedTestCase = testCaseService.addStepToTestCase(testCaseId, stepNumber, action, expectedResult);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Шаг добавлен к тест-кейсу!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при добавлении шага!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при добавлении шага: " + e.getMessage());
        }
        return response;
    }

    // Обновить шаг
    @ResponseBody
    @PutMapping("/api/test-steps/{stepId}")
    public Map<String, Object> updateStep(@PathVariable Long stepId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer stepNumber = (Integer) payload.get("stepNumber");
            String action = (String) payload.get("action");
            String expectedResult = (String) payload.get("expectedResult");

            TestCaseDTO updatedTestCase = testCaseService.updateStep(stepId, stepNumber, action, expectedResult);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Шаг обновлен!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при обновлении шага!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении шага: " + e.getMessage());
        }
        return response;
    }

    // Удалить шаг
    @ResponseBody
    @DeleteMapping("/api/test-steps/{stepId}")
    public Map<String, Object> removeStep(@PathVariable Long stepId) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO updatedTestCase = testCaseService.removeStep(stepId);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "Шаг удален!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при удалении шага!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении шага: " + e.getMessage());
        }
        return response;
    }

    // Добавить user story к тест-кейсу
    @ResponseBody
    @PostMapping("/api/test-cases/{testCaseId}/user-stories/{userStoryId}")
    public Map<String, Object> addUserStoryToTestCase(
            @PathVariable Long testCaseId,
            @PathVariable Long userStoryId
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO updatedTestCase = testCaseService.addUserStoryToTestCase(testCaseId, userStoryId);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "User story добавлена к тест-кейсу!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при добавлении user story!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при добавлении user story: " + e.getMessage());
        }
        return response;
    }

    // Удалить user story из тест-кейса
    @ResponseBody
    @DeleteMapping("/api/test-cases/{testCaseId}/user-stories/{userStoryId}")
    public Map<String, Object> removeUserStoryFromTestCase(
            @PathVariable Long testCaseId,
            @PathVariable Long userStoryId
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseDTO updatedTestCase = testCaseService.removeUserStoryFromTestCase(testCaseId, userStoryId);
            if (updatedTestCase != null) {
                response.put("success", true);
                response.put("message", "User story удалена из тест-кейса!");
                response.put("testCase", updatedTestCase);
            } else {
                response.put("success", false);
                response.put("message", "Ошибка при удалении user story!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении user story: " + e.getMessage());
        }
        return response;
    }

    // *** УДАЛЕНЫ ДУБЛИРУЮЩИЕСЯ МЕТОДЫ ДЛЯ КОММЕНТАРИЕВ ***
    // Они теперь находятся в CommentController
}