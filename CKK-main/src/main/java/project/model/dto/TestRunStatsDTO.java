// TestRunStatsDTO.java
package project.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestRunStatsDTO {
    private int totalCount;
    private int passedCount;
    private int failedCount;
    private int skippedCount;
    private int notRunCount;
    private double passedPercentage;
    private double failedPercentage;
    private double skippedPercentage;
    private double notRunPercentage;

    public TestRunStatsDTO() {}

    public TestRunStatsDTO(int totalCount, int passedCount, int failedCount,
                           int skippedCount, int notRunCount) {
        this.totalCount = totalCount;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.skippedCount = skippedCount;
        this.notRunCount = notRunCount;

        // Расчет процентов
        if (totalCount > 0) {
            this.passedPercentage = (double) passedCount / totalCount * 100;
            this.failedPercentage = (double) failedCount / totalCount * 100;
            this.skippedPercentage = (double) skippedCount / totalCount * 100;
            this.notRunPercentage = (double) notRunCount / totalCount * 100;
        }
    }
}