package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.*;
import project.model.dto.*;
import project.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestRunService {

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private TestRunTestCaseRepository testRunTestCaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private TestCaseService testCaseService;

    public List<TestRunDTO> getTestRunsByProjectId(Long projectId) {
        List<TestRun> testRuns = testRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return testRuns.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // НОВЫЙ МЕТОД: Получение детальной информации о тест-ране
    public TestRunDTO getTestRunDetailed(Long id) {
        Optional<TestRun> testRunOptional = testRunRepository.findById(id);
        return testRunOptional.map(this::convertToDetailedDTO).orElse(null);
    }

    public TestRunDTO createTestRun(Long projectId, String title, String description,
                                    String executorName, String creatorName,
                                    List<Long> testCaseIds, List<Long> testSuiteIds) {
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            return null;
        }

        TestRun testRun = new TestRun(title, description, executorName, creatorName, projectOptional.get());
        testRun.setStatus(TestRun.Status.NOT_STARTED);

        TestRun savedTestRun = testRunRepository.save(testRun);

        // Добавляем отдельные тест-кейсы
        if (testCaseIds != null && !testCaseIds.isEmpty()) {
            for (Long testCaseId : testCaseIds) {
                addTestCaseToTestRun(savedTestRun, testCaseId);
            }
        }

        // Добавляем тест-кейсы из тест-сьютов
        if (testSuiteIds != null && !testSuiteIds.isEmpty()) {
            for (Long testSuiteId : testSuiteIds) {
                List<TestCase> suiteTestCases = testCaseRepository.findByTestSuiteIdOrderByCreatedAtAsc(testSuiteId);
                for (TestCase testCase : suiteTestCases) {
                    addTestCaseToTestRun(savedTestRun, testCase.getId());
                }
            }
        }

        return convertToDTO(savedTestRun);
    }

    // ОБНОВЛЕННЫЙ МЕТОД: Обновление тест-рана с изменением состава
    public TestRunDTO updateTestRun(Long id, String title, String description,
                                    String executorName, String creatorName, TestRun.Status status,
                                    List<Integer> testCaseIdsToAdd, List<Integer> testCaseIdsToRemove) {
        Optional<TestRun> testRunOptional = testRunRepository.findById(id);
        if (testRunOptional.isPresent()) {
            TestRun testRun = testRunOptional.get();

            // Обновляем основные поля
            if (title != null) testRun.setTitle(title);
            if (description != null) testRun.setDescription(description);
            if (executorName != null) testRun.setExecutorName(executorName);
            if (creatorName != null) testRun.setCreatorName(creatorName);
            if (status != null) testRun.setStatus(status);

            TestRun updatedTestRun = testRunRepository.save(testRun);

            // Удаляем тест-кейсы
            if (testCaseIdsToRemove != null && !testCaseIdsToRemove.isEmpty()) {
                for (Integer testCaseId : testCaseIdsToRemove) {
                    removeTestCaseFromTestRun(updatedTestRun, testCaseId.longValue());
                }
            }

            // Добавляем новые тест-кейсы
            if (testCaseIdsToAdd != null && !testCaseIdsToAdd.isEmpty()) {
                for (Integer testCaseId : testCaseIdsToAdd) {
                    addTestCaseToTestRun(updatedTestRun, testCaseId.longValue());
                }
            }

            return convertToDetailedDTO(updatedTestRun);
        }
        return null;
    }

    // Вспомогательный метод для добавления тест-кейса в тест-ран
    private void addTestCaseToTestRun(TestRun testRun, Long testCaseId) {
        Optional<TestCase> testCaseOptional = testCaseRepository.findById(testCaseId);
        if (testCaseOptional.isPresent()) {
            // Проверяем, не добавлен ли уже тест-кейс
            boolean alreadyExists = testRunTestCaseRepository.findAll().stream()
                    .anyMatch(trtc -> trtc.getTestRun().getId().equals(testRun.getId()) &&
                            trtc.getTestCase().getId().equals(testCaseId));

            if (!alreadyExists) {
                TestRunTestCase testRunTestCase = new TestRunTestCase(testRun, testCaseOptional.get());
                testRunTestCase.setStatus(TestRunTestCase.TestCaseStatus.NOT_RUN);
                testRunTestCaseRepository.save(testRunTestCase);
            }
        }
    }

    // Вспомогательный метод для удаления тест-кейса из тест-рана
    private void removeTestCaseFromTestRun(TestRun testRun, Long testCaseId) {
        testRunTestCaseRepository.findAll().stream()
                .filter(trtc -> trtc.getTestRun().getId().equals(testRun.getId()) &&
                        trtc.getTestCase().getId().equals(testCaseId))
                .findFirst()
                .ifPresent(trtc -> testRunTestCaseRepository.delete(trtc));
    }

    public Optional<TestRunDTO> getTestRunById(Long id) {
        return testRunRepository.findById(id).map(this::convertToDTO);
    }

    public TestRunDTO updateTestRunStatus(Long id, TestRun.Status status) {
        Optional<TestRun> testRunOptional = testRunRepository.findById(id);
        if (testRunOptional.isPresent()) {
            TestRun testRun = testRunOptional.get();
            testRun.setStatus(status);
            TestRun updatedTestRun = testRunRepository.save(testRun);
            return convertToDTO(updatedTestRun);
        }
        return null;
    }

    public boolean deleteTestRun(Long id) {
        if (testRunRepository.existsById(id)) {
            testRunTestCaseRepository.deleteByTestRunId(id);
            testRunRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<TestRunDTO> searchTestRuns(Long projectId, String searchTerm) {
        List<TestRun> testRuns = testRunRepository.searchByProjectId(projectId, searchTerm);
        return testRuns.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public TestRunTestCaseDTO updateTestCaseStatus(Long testRunId, Long testCaseId,
                                                   TestRunTestCase.TestCaseStatus status, String comment) {
        Optional<TestRunTestCase> testRunTestCaseOptional = testRunTestCaseRepository.findAll().stream()
                .filter(trtc -> trtc.getTestRun().getId().equals(testRunId) &&
                        trtc.getTestCase().getId().equals(testCaseId))
                .findFirst();

        if (testRunTestCaseOptional.isPresent()) {
            TestRunTestCase testRunTestCase = testRunTestCaseOptional.get();
            testRunTestCase.setStatus(status);
            testRunTestCase.setComment(comment);
            TestRunTestCase updated = testRunTestCaseRepository.save(testRunTestCase);
            return convertToTestCaseDTO(updated);
        }
        return null;
    }

    private TestRunDTO convertToDTO(TestRun testRun) {
        TestRunDTO dto = new TestRunDTO(
                testRun.getId(),
                testRun.getTitle(),
                testRun.getDescription(),
                testRun.getExecutorName(),
                testRun.getCreatorName(),
                testRun.getStatus(),
                testRun.getCreatedAt(),
                testRun.getUpdatedAt(),
                testRun.getProject().getId()
        );

        // Загружаем связанные тест-кейсы
        List<TestRunTestCase> testRunTestCases = testRunTestCaseRepository.findByTestRunIdOrderByCreatedAtAsc(testRun.getId());
        List<TestRunTestCaseDTO> testCaseDTOs = testRunTestCases.stream()
                .map(this::convertToTestCaseDTO)
                .collect(Collectors.toList());
        dto.setTestCases(testCaseDTOs);

        // Рассчитываем статистику
        dto.setStats(calculateStats(testRunTestCases));

        return dto;
    }

    // НОВЫЙ МЕТОД: Конвертация в детальный DTO
    private TestRunDTO convertToDetailedDTO(TestRun testRun) {
        TestRunDTO dto = convertToDTO(testRun);

        // Дополнительная информация для детального отображения
        List<TestRunTestCase> testRunTestCases = testRunTestCaseRepository.findByTestRunIdOrderByCreatedAtAsc(testRun.getId());
        dto.setTestCases(testRunTestCases.stream()
                .map(this::convertToDetailedTestCaseDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    // НОВЫЙ МЕТОД: Детальная конвертация TestRunTestCase
    private TestRunTestCaseDTO convertToDetailedTestCaseDTO(TestRunTestCase testRunTestCase) {
        TestCaseDTO testCaseDTO = testCaseService.getTestCaseById(testRunTestCase.getTestCase().getId()).orElse(null);

        TestRunTestCase.TestCaseStatus status = testRunTestCase.getStatus() != null ?
                testRunTestCase.getStatus() : TestRunTestCase.TestCaseStatus.NOT_RUN;

        TestRunTestCaseDTO dto = new TestRunTestCaseDTO(
                testRunTestCase.getId(),
                testRunTestCase.getTestRun().getId(),
                testCaseDTO,
                status,
                testRunTestCase.getComment(),
                testRunTestCase.getCreatedAt(),
                testRunTestCase.getUpdatedAt()
        );

        return dto;
    }

    // Метод расчета статистики
    private TestRunStatsDTO calculateStats(List<TestRunTestCase> testRunTestCases) {
        int totalCount = testRunTestCases.size();
        int passedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int notRunCount = 0;

        for (TestRunTestCase trtc : testRunTestCases) {
            if (trtc.getStatus() == null) {
                notRunCount++;
                continue;
            }
            switch (trtc.getStatus()) {
                case PASSED:
                    passedCount++;
                    break;
                case FAILED:
                    failedCount++;
                    break;
                case SKIPPED:
                    skippedCount++;
                    break;
                case NOT_RUN:
                    notRunCount++;
                    break;
            }
        }

        return new TestRunStatsDTO(totalCount, passedCount, failedCount, skippedCount, notRunCount);
    }

    private TestRunTestCaseDTO convertToTestCaseDTO(TestRunTestCase testRunTestCase) {
        TestCaseDTO testCaseDTO = testCaseService.getTestCaseById(testRunTestCase.getTestCase().getId()).orElse(null);

        TestRunTestCase.TestCaseStatus status = testRunTestCase.getStatus() != null ?
                testRunTestCase.getStatus() : TestRunTestCase.TestCaseStatus.NOT_RUN;

        return new TestRunTestCaseDTO(
                testRunTestCase.getId(),
                testRunTestCase.getTestRun().getId(),
                testCaseDTO,
                status,
                testRunTestCase.getComment(),
                testRunTestCase.getCreatedAt(),
                testRunTestCase.getUpdatedAt()
        );
    }
}