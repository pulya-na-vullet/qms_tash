// TestCaseReviewController.java
package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.model.dto.TestCaseReviewDTO;
import project.service.TestCaseReviewService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TestCaseReviewController {

    @Autowired
    private TestCaseReviewService testCaseReviewService;

    @ResponseBody
    @PostMapping("/api/test-suites/{testSuiteId}/ai-test-case-review")
    public Map<String, Object> runAIReviewForTestSuite(@PathVariable Long testSuiteId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<TestCaseReviewDTO> reviews = testCaseReviewService.reviewTestSuite(testSuiteId);
            response.put("success", true);
            response.put("message", "AI ревью завершено для " + reviews.size() + " тест-кейсов");
            response.put("reviews", reviews);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при выполнении AI ревью: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @PostMapping("/api/test-cases/{testCaseId}/ai-review")
    public Map<String, Object> runAIReviewForTestCase(@PathVariable Long testCaseId) {
        Map<String, Object> response = new HashMap<>();
        try {
            TestCaseReviewDTO review = testCaseReviewService.createOrUpdateReview(testCaseId);
            if (review != null) {
                response.put("success", true);
                response.put("message", "AI ревью завершено");
                response.put("review", review);
            } else {
                response.put("success", false);
                response.put("message", "Тест-кейс не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при выполнении AI ревью: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @GetMapping("/api/test-cases/{testCaseId}/ai-review")
    public Map<String, Object> getTestCaseReview(@PathVariable Long testCaseId) {
        Map<String, Object> response = new HashMap<>();
        try {
            var review = testCaseReviewService.getReviewByTestCaseId(testCaseId);
            if (review.isPresent()) {
                response.put("success", true);
                response.put("review", review.get());
            } else {
                response.put("success", false);
                response.put("message", "Ревью не найдено");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении ревью: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @GetMapping("/api/test-suites/{testSuiteId}/ai-reviews")
    public Map<String, Object> getTestSuiteReviews(@PathVariable Long testSuiteId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<TestCaseReviewDTO> reviews = testCaseReviewService.getReviewsByTestSuiteId(testSuiteId);
            response.put("success", true);
            response.put("reviews", reviews);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении ревью: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @DeleteMapping("/api/test-cases/{testCaseId}/ai-review")
    public Map<String, Object> deleteTestCaseReview(@PathVariable Long testCaseId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = testCaseReviewService.deleteReview(testCaseId);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Ревью удалено");
            } else {
                response.put("success", false);
                response.put("message", "Ревью не найдено");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении ревью: " + e.getMessage());
        }
        return response;
    }
}