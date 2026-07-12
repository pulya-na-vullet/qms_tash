package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.model.Project;
import project.model.TestRun;
import project.model.TestRunTestCase;
import project.model.dto.TestRunDTO;
import project.service.ProjectService;
import project.service.TestRunService;
import project.service.TestSuiteService;

import java.util.List;
import java.util.Optional;

@Controller
public class TestRunController {

    @Autowired
    private TestRunService testRunService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TestSuiteService testSuiteService;

    // Страница списка тест-ранов проекта
    @GetMapping("/project/{projectId}/test-runs")
    public String showTestRunsPage(@PathVariable Long projectId, Model model) {
        Optional<Project> projectOptional = projectService.getProjectById(projectId);
        if (projectOptional.isEmpty()) {
            return "redirect:/project-qa";
        }

        List<TestRunDTO> testRuns = testRunService.getTestRunsByProjectId(projectId);

        model.addAttribute("project", projectOptional.get());
        model.addAttribute("testRuns", testRuns);

        // Добавляем утилитарные методы в модель
        model.addAttribute("thUtils", new ThymeleafUtils());

        return "test-runs";
    }

    // Страница детализации тест-рана
    @GetMapping("/test-run/{id}")
    public String showTestRunDetailPage(@PathVariable Long id, Model model) {
        Optional<TestRunDTO> testRunOptional = testRunService.getTestRunById(id);
        if (testRunOptional.isEmpty()) {
            return "redirect:/project-qa";
        }

        model.addAttribute("testRun", testRunOptional.get());
        model.addAttribute("thUtils", new ThymeleafUtils());

        return "test-run-detail";
    }

    // Внутренний класс утилит для Thymeleaf
    public static class ThymeleafUtils {

        public String getStatusBadgeClass(TestRun.Status status) {
            if (status == null) {
                return "secondary";
            }
            switch (status) {
                case NOT_STARTED: return "secondary";
                case IN_PROGRESS: return "warning";
                case COMPLETED: return "success";
                default: return "secondary";
            }
        }

        public String getTestCaseStatusBadgeClass(TestRunTestCase.TestCaseStatus status) {
            if (status == null) {
                return "secondary";
            }
            switch (status) {
                case NOT_RUN: return "secondary";
                case PASSED: return "success";
                case FAILED: return "danger";
                case SKIPPED: return "warning";
                default: return "secondary";
            }
        }

        public String getProgressBarWidth(int current, int total) {
            if (total == 0) return "0";
            return String.valueOf((current * 100) / total);
        }
    }
}