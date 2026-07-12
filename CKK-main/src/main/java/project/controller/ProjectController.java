package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.model.Project;
import project.model.dto.TestCaseDTO;
import project.service.ProjectService;
import project.service.TestCaseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TestCaseService testCaseService;

    // Добавьте этот метод в ProjectController
    @ResponseBody
    @GetMapping("/api/projects/{projectId}/test-cases")
    public List<TestCaseDTO> getTestCasesByProject(@PathVariable Long projectId) {
        return testCaseService.getTestCasesByProjectId(projectId);
    }

    // Страница списка проектов
    @GetMapping("/")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/projects")
    public String showProjectsPage() {
        return "projects";
    }


    // Страница конкретного проекта
    @GetMapping("/projects/{id}")
    public String showProjectPage(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id).orElse(null);
        if (project == null) {
            return "redirect:/";
        }
        model.addAttribute("project", project);
        return "project-detail";
    }

    // ========== API ENDPOINTS ==========

    // Получить все проекты
    @ResponseBody
    @GetMapping("/api/projects")
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    // Создать проект
    @ResponseBody
    @PostMapping("/api/projects")
    public Map<String, Object> createProject(@RequestBody Project project) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (project.getName() != null && !project.getName().trim().isEmpty()) {
                Project createdProject = projectService.createProject(project.getName().trim());
                response.put("success", true);
                response.put("message", "Проект создан успешно!");
                response.put("project", createdProject);
            } else {
                response.put("success", false);
                response.put("message", "Название проекта не может быть пустым!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при создании проекта: " + e.getMessage());
        }
        return response;
    }

    // Получить проект по ID
    @ResponseBody
    @GetMapping("/api/projects/{id}")
    public Map<String, Object> getProject(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Project project = projectService.getProjectById(id).orElse(null);
            if (project != null) {
                response.put("success", true);
                response.put("project", project);
            } else {
                response.put("success", false);
                response.put("message", "Проект не найден!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при получении проекта: " + e.getMessage());
        }
        return response;
    }

    // Обновить проект
    @ResponseBody
    @PutMapping("/api/projects/{id}")
    public Map<String, Object> updateProject(@PathVariable Long id, @RequestBody Project project) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (project.getName() != null && !project.getName().trim().isEmpty()) {
                Project updatedProject = projectService.updateProject(id, project.getName().trim());
                if (updatedProject != null) {
                    response.put("success", true);
                    response.put("message", "Проект обновлен успешно!");
                    response.put("project", updatedProject);
                } else {
                    response.put("success", false);
                    response.put("message", "Проект не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название проекта не может быть пустым!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при обновлении проекта: " + e.getMessage());
        }
        return response;
    }

    // Удалить проект
    @ResponseBody
    @DeleteMapping("/api/projects/{id}")
    public Map<String, Object> deleteProject(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = projectService.deleteProject(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Проект удален успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Проект не найден!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при удалении проекта: " + e.getMessage());
        }
        return response;
    }
}