package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Aggregated practice performance for a single student. */
@Data
@Accessors(chain = true)
public class PracticePerformanceDto {
    private long totalAttempts;
    private long correctAttempts;
    private int accuracyPercent;

    private long mockTestsTaken;
    private long mockTestsPassed;
    private Integer bestTestPercent;

    /** Per-category accuracy rows (name = category label, value = attempts). */
    private List<CategoryStat> byCategory = new ArrayList<>();

    private List<RecentAttempt> recentAttempts = new ArrayList<>();
    private List<RecentTest> recentTests = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class CategoryStat {
        private String category;
        private long attempts;
        private long correct;
        private int accuracyPercent;
    }

    @Data
    @Accessors(chain = true)
    public static class RecentAttempt {
        private String questionText;
        private String categoryLabel;
        private boolean correct;
        private LocalDateTime attemptedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class RecentTest {
        private String title;
        private Integer percentage;
        private boolean passed;
        private LocalDateTime submittedAt;
    }
}
