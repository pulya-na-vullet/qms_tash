package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.model.TestSuite;
import project.model.Project;
import project.model.dto.TestSuiteDTO;
import project.repository.TestSuiteRepository;
import project.repository.ProjectRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestSuiteService {

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<TestSuiteDTO> getTestSuitesByProjectId(Long projectId) {
        List<TestSuite> testSuites = testSuiteRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        return testSuites.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TestSuiteDTO createTestSuite(Long projectId, String name) {
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isPresent()) {
            // Проверяем, существует ли уже тест-сьют с таким именем
            Optional<TestSuite> existingTestSuite = testSuiteRepository.findByProjectIdAndName(projectId, name.trim());
            if (existingTestSuite.isPresent()) {
                return convertToDTO(existingTestSuite.get());
            }

            TestSuite testSuite = new TestSuite(name.trim(), projectOptional.get());
            TestSuite savedTestSuite = testSuiteRepository.save(testSuite);
            return convertToDTO(savedTestSuite);
        }
        return null;
    }

    public Optional<TestSuiteDTO> getTestSuiteById(Long id) {
        return testSuiteRepository.findById(id).map(this::convertToDTO);
    }

    public TestSuiteDTO updateTestSuite(Long id, String name) {
        Optional<TestSuite> testSuiteOptional = testSuiteRepository.findById(id);
        if (testSuiteOptional.isPresent()) {
            TestSuite testSuite = testSuiteOptional.get();
            testSuite.setName(name.trim());
            TestSuite updatedTestSuite = testSuiteRepository.save(testSuite);
            return convertToDTO(updatedTestSuite);
        }
        return null;
    }

    public boolean deleteTestSuite(Long id) {
        if (testSuiteRepository.existsById(id)) {
            testSuiteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<TestSuiteDTO> searchTestSuites(Long projectId, String searchTerm) {
        List<TestSuite> testSuites = testSuiteRepository.findByProjectIdAndNameContainingIgnoreCaseOrderByCreatedAtAsc(
                projectId, searchTerm);
        return testSuites.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Метод для преобразования TestSuite в TestSuiteDTO
    private TestSuiteDTO convertToDTO(TestSuite testSuite) {
        return new TestSuiteDTO(
                testSuite.getId(),
                testSuite.getName(),
                testSuite.getCreatedAt(),
                testSuite.getUpdatedAt(),
                testSuite.getProject().getId()
        );
    }
}