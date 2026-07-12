package project.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project.model.Project;
import project.service.ProjectService;
import project.service.TraceabilityMatrixService;

import java.util.List;

@Component
public class TraceabilityMatrixScheduler {

    @Autowired
    private TraceabilityMatrixService matrixService;

    @Autowired
    private ProjectService projectService;

    @Scheduled(fixedRate = 1000) // 15 минут = 900000 мс
    public void generateAllMatrices() {
        List<Project> projects = projectService.getAllProjects(); // реализуйте метод
        for (Project project : projects) {
            matrixService.generateAndSaveMatrix(project.getId());
        }
    }
}
