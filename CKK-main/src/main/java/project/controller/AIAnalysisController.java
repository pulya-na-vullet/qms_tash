package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.service.AIAnalysisService;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AIAnalysisController {

    @Autowired
    private AIAnalysisService aiAnalysisService;

    @ResponseBody
    @PostMapping("/api/test-suites/{testSuiteId}/ai-analysis")
    public Map<String, Object> analyzeTestSuite(@PathVariable Long testSuiteId) {
        return aiAnalysisService.analyzeTestSuite(testSuiteId);
    }
}