package project.util;

import project.model.TestRun;
import project.model.TestRunTestCase;

public class ThymeleafUtils {

    public static String getStatusBadgeClass(TestRun.Status status) {
        if (status == null) return "secondary";
        switch (status) {
            case NOT_STARTED: return "secondary";
            case IN_PROGRESS: return "warning";
            case COMPLETED: return "success";
            default: return "secondary";
        }
    }

    public static String getTestCaseStatusBadgeClass(TestRunTestCase.TestCaseStatus status) {
        if (status == null) return "secondary";
        switch (status) {
            case NOT_RUN: return "secondary";
            case PASSED: return "success";
            case FAILED: return "danger";
            case SKIPPED: return "warning";
            default: return "secondary";
        }
    }

    public static String getProgressBarWidth(int current, int total) {
        if (total == 0) return "0";
        return String.valueOf((current * 100) / total);
    }
}