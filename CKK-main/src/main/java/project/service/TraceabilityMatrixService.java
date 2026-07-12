// src/main/java/project/service/TraceabilityMatrixService.java
package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.TraceabilityMatrix;
import project.model.dto.TestCaseDTO;
import project.model.dto.UserStoryDTO;
import project.repository.TraceabilityMatrixRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TraceabilityMatrixService {

    @Autowired
    private TraceabilityMatrixRepository matrixRepository;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private UserStoryService userStoryService;

    @Autowired
    private TestCasesUserStoriesService tcusService;

    public Optional<TraceabilityMatrix> getMatrixByProjectId(Long projectId) {
        return matrixRepository.findByProjectId(projectId);
    }

    public void generateAndSaveMatrix(Long projectId) {
        List<TestCaseDTO> testCases = testCaseService.getTestCasesByProjectId(projectId);
        List<UserStoryDTO> userStories = userStoryService.getUserStoriesByProjectId(projectId);

        String html = buildMatrixHtml(userStories, testCases, projectId);

        TraceabilityMatrix matrix = matrixRepository.findByProjectId(projectId)
                .orElse(new TraceabilityMatrix());
        matrix.setProjectId(projectId);
        matrix.setMatrixHtml(html);
        matrix.setCreatedAt(LocalDateTime.now());
        matrixRepository.save(matrix);
    }

    private String buildMatrixHtml(List<UserStoryDTO> userStories, List<TestCaseDTO> testCases, Long projectId) {
        StringBuilder sb = new StringBuilder();

        sb.append("<table class=\"traceability-table\">");

        // Заголовок
        sb.append("<thead><tr>");
        sb.append("<th class=\"user-story-header\">User Story \\ Test Case</th>");

        for (TestCaseDTO tc : testCases) {
            sb.append("<th class=\"test-case-header\">");
            sb.append("<a href=\"#\" class=\"rotated-link\" data-test-case-id=\"")
                    .append(tc.getId())
                    .append("\" title=\"")
                    .append(escapeHtml(tc.getName()))
                    .append("\">")
                    .append("TC-").append(tc.getId()) // <-- Только номер
                    .append("</a>");
            sb.append("</th>");
        }
        sb.append("</tr></thead>");

        // Тело
        sb.append("<tbody>");
        for (UserStoryDTO us : userStories) {
            sb.append("<tr class=\"user-story-row\">");
            sb.append("<td class=\"user-story-cell\" title=\"")
                    .append(escapeHtml(us.getName()))
                    .append("\">")
                    .append(escapeHtml(us.getName()))
                    .append("</td>");

            for (TestCaseDTO tc : testCases) {
                boolean linked = tcusService.isTestCaseLinkedToUserStory(tc.getId(), us.getId());
                if (linked) {
                    sb.append("<td class=\"linked-cell\"><span class=\"linked-indicator\">✓</span></td>");
                } else {
                    sb.append("<td class=\"unlinked-cell\"></td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</tbody>");

        sb.append("</table>");
        return sb.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}