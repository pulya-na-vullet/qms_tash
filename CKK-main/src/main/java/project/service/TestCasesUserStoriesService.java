package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.repository.TestCasesUserStoriesRepository;

@Service
public class TestCasesUserStoriesService {

    @Autowired
    private TestCasesUserStoriesRepository tcusRepository;

    public boolean isTestCaseLinkedToUserStory(Long testCaseId, Long userStoryId) {
        return tcusRepository.existsByTestCaseIdAndUserStoryId(testCaseId, userStoryId);
    }
}
