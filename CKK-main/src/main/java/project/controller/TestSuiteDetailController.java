package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import project.model.dto.TestCaseDTO;
import project.model.dto.TestSuiteDTO;
import project.service.TestCaseService;
import project.service.TestSuiteService;

import java.util.List;

@Controller
public class TestSuiteDetailController {

    @Autowired
    private TestSuiteService testSuiteService;

    @Autowired
    private TestCaseService testCaseService;

    // Страница детализации тест-сьюта
    @GetMapping("/test-suite/{id}")
    public String showTestSuiteDetailPage(@PathVariable Long id, Model model) {
        TestSuiteDTO testSuiteDTO = testSuiteService.getTestSuiteById(id).orElse(null);
        if (testSuiteDTO == null) {
            return "redirect:/project-qa";
        }

        List<TestCaseDTO> testCases = testCaseService.getTestCasesByTestSuiteId(id);

        model.addAttribute("testSuite", testSuiteDTO);
        model.addAttribute("testCases", testCases);
        return "test-suite-detail";
    }
}