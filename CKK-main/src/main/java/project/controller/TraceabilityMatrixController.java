package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import project.model.TraceabilityMatrix;
import project.service.ProjectService;
import project.service.TraceabilityMatrixService;

@Controller
public class TraceabilityMatrixController {

    @Autowired
    private TraceabilityMatrixService matrixService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/project/{projectId}/traceability-matrix")
    public String showTraceabilityMatrix(@PathVariable Long projectId, Model model) {
        TraceabilityMatrix matrix = matrixService.getMatrixByProjectId(projectId)
                .orElseThrow(() -> new RuntimeException("Matrix not found for project ID: " + projectId));

        model.addAttribute("project", projectService.getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId)));
        model.addAttribute("matrixHtml", matrix.getMatrixHtml());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String userRole = "UNKNOWN"; // Значение по умолчанию, если пользователь не аутентифицирован или роль не определена

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // Получаем роли пользователя
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ANALYST".equals(role)) { // или другая ваша логика
                    userRole = "ANALYST";
                    break; // Нашли, выходим из цикла
                } else if ("ROLE_TESTER".equals(role)) {
                    userRole = "TESTER";
                    break;
                }
            }
        }
        model.addAttribute("userRole", userRole); // Пе

        return "traceability-matrix";
    }
}