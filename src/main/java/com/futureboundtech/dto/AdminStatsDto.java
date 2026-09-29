package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** Aggregated dashboard analytics sourced live from the database. */
@Data
@Accessors(chain = true)
public class AdminStatsDto {

    private long totalStudents;
    private long activeStudents;
    private long totalTrainers;
    private long totalCourses;
    private long publishedCourses;
    private long totalEnrollments;
    private long activeEnrollments;
    private long totalBatches;
    private long upcomingClasses;
    private long certificatesIssued;
    private long unreadMessages;
    private long pendingPayments;
    private long failedPayments;

    private long successfulPayments;
    private String totalRevenue;

    private List<UserBriefDto> recentRegistrations = new ArrayList<>();
    private List<NameValueDto> enrollmentsByCourse = new ArrayList<>();
    private List<NameValueDto> revenueByStatus = new ArrayList<>();
    private List<NameValueDto> coursesByCategory = new ArrayList<>();
}
