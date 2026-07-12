// TestCaseReviewService.java
package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.model.TestCase;
import project.model.TestCaseReview;
import project.model.dto.TestCaseDTO;
import project.model.dto.TestCaseReviewDTO;
import project.repository.TestCaseReviewRepository;
import project.repository.TestCaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestCaseReviewService {

    @Autowired
    private TestCaseReviewRepository testCaseReviewRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private AIReviewService aiReviewService;

    @Autowired
    private TestCaseService testCaseService;

    public TestCaseReviewDTO createOrUpdateReview(Long testCaseId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isEmpty()) {
            return null;
        }

        TestCase testCase = testCaseOpt.get();
        TestCaseDTO testCaseDTO = testCaseService.getTestCaseById(testCaseId).orElse(null);
        if (testCaseDTO == null) {
            return null;
        }

        String reviewResult = aiReviewService.reviewTestCase(testCaseDTO);
        Integer overallScore = extractOverallScore(reviewResult);

        Optional<TestCaseReview> existingReview = testCaseReviewRepository.findByTestCaseId(testCaseId);
        TestCaseReview review;

        if (existingReview.isPresent()) {
            review = existingReview.get();
            review.setReviewResult(reviewResult);
            review.setOverallScore(overallScore);
        } else {
            review = new TestCaseReview();
            review.setTestCase(testCase);
            review.setReviewResult(reviewResult);
            review.setOverallScore(overallScore);
        }

        TestCaseReview savedReview = testCaseReviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    public List<TestCaseReviewDTO> reviewTestSuite(Long testSuiteId) {
        List<TestCase> testCases = testCaseRepository.findByTestSuiteIdOrderByCreatedAtAsc(testSuiteId);
        return testCases.stream()
                .map(testCase -> createOrUpdateReview(testCase.getId()))
                .collect(Collectors.toList());
    }

    public Optional<TestCaseReviewDTO> getReviewByTestCaseId(Long testCaseId) {
        return testCaseReviewRepository.findByTestCaseId(testCaseId)
                .map(this::convertToDTO);
    }

    public List<TestCaseReviewDTO> getReviewsByTestSuiteId(Long testSuiteId) {
        return testCaseReviewRepository.findByTestSuiteId(testSuiteId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean deleteReview(Long testCaseId) {
        if (testCaseReviewRepository.existsByTestCaseId(testCaseId)) {
            testCaseReviewRepository.deleteByTestCaseId(testCaseId);
            return true;
        }
        return false;
    }

    private Integer extractOverallScore(String reviewResult) {
        try {
            if (reviewResult.contains("ОБЩАЯ ОЦЕНКА:")) {
                String scorePart = reviewResult.split("ОБЩАЯ ОЦЕНКА:")[1].split("/")[0].trim();
                return Integer.parseInt(scorePart.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            // Если не удалось извлечь оценку, вернем null
        }
        return null;
    }

    private TestCaseReviewDTO convertToDTO(TestCaseReview review) {
        return new TestCaseReviewDTO(
                review.getId(),
                review.getTestCase().getId(),
                review.getReviewResult(),
                review.getOverallScore(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}