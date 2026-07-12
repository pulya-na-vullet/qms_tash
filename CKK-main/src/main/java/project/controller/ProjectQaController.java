package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import project.model.Project;
import project.model.dto.TestSuiteDTO;
import project.service.ProjectService;
import project.service.TestSuiteService;

import java.util.List;
import java.util.Optional;

@Controller
public class ProjectQaController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TestSuiteService testSuiteService;

    // Страница списка проектов QA
    @GetMapping("/project-qa")
    public String showProjectQaPage(Model model) {
        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);
        return "project-qa";
    }

    // Страница детализации проекта для QA
    @GetMapping("/project-qa/{id}")
    public String showProjectQaDetailPage(@PathVariable Long id, Model model) {
        Optional<Project> projectOptional = projectService.getProjectById(id);
        if (projectOptional.isEmpty()) {
            return "redirect:/project-qa";
        }

        Project project = projectOptional.get();
        List<TestSuiteDTO> testSuites = testSuiteService.getTestSuitesByProjectId(id);

        model.addAttribute("project", project);
        model.addAttribute("testSuites", testSuites);
        return "project-qa-detail";
    }
}