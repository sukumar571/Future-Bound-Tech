package com.futureboundtech;

import com.futureboundtech.entity.Placement;
import com.futureboundtech.repository.PlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDate;

/**
 * Seeds clearly-labelled DEMO placement posts so the Phase 17 placement pages are
 * populated for the demo. Every company/role carries a "(DEMO)" marker and links
 * point to example.com placeholders — this is NOT real hiring information and
 * must be replaced with verified openings. Runs only when the table is empty.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeders.enabled", havingValue = "true", matchIfMissing = true)
public class PlacementDataSeeder {

    private final PlacementRepository placementRepository;

    @Bean
    @Order(4)
    public CommandLineRunner initPlacements() {
        return args -> {
            if (placementRepository.count() > 0) {
                return;
            }
            placementRepository.save(Placement.builder()
                    .companyName("Nimbus Technologies (DEMO)")
                    .jobTitle("Junior Java Developer (DEMO)")
                    .description("Full-stack support role on Spring Boot services and REST APIs. "
                            + "Demo listing for layout — not a real vacancy.")
                    .eligibility("BE/BTech (2025/2026 batch), 60% aggregate or 6.0 CGPA, no active backlogs.")
                    .skills("Java, Spring Boot, SQL, REST APIs")
                    .location("Bengaluru / Hybrid")
                    .jobType("Full-time")
                    .deadline(LocalDate.now().plusDays(21))
                    .applicationUrl("https://example.com/apply/demo-junior-java-developer")
                    .prepResources("Revise OOP, Collections and Spring basics. Practise the Java + Technical Interview "
                            + "MCQs and take the Foundation Mock Test before applying.")
                    .published(true)
                    .build());

            placementRepository.save(Placement.builder()
                    .companyName("DataForge Analytics (DEMO)")
                    .jobTitle("Data Analyst Intern (DEMO)")
                    .description("Support reporting and dashboarding with SQL and Python. "
                            + "Demo listing for layout — not a real vacancy.")
                    .eligibility("Final-year students, strong SQL, basic Python. Available 6 days/week.")
                    .skills("SQL, Python, Excel, Statistics")
                    .location("Remote")
                    .jobType("Internship")
                    .deadline(LocalDate.now().plusDays(9))
                    .applicationUrl("https://example.com/apply/demo-data-analyst-intern")
                    .prepResources("Focus on SQL joins/aggregations and the Aptitude section. "
                            + "Brush up Python data-coding problems in the library.")
                    .published(true)
                    .build());

            placementRepository.save(Placement.builder()
                    .companyName("BrightWeb Studio (DEMO)")
                    .jobTitle("Frontend Developer (DEMO)")
                    .description("Build responsive UIs with HTML, CSS, Bootstrap and JavaScript. "
                            + "Demo listing for layout — not a real vacancy.")
                    .eligibility("Any graduate with a strong project portfolio; knowledge of responsive design.")
                    .skills("HTML, CSS, JavaScript, Bootstrap")
                    .location("Pune / Onsite")
                    .jobType("Full-time")
                    .deadline(LocalDate.now().minusDays(3))
                    .applicationUrl("https://example.com/apply/demo-frontend-developer")
                    .prepResources("Practise the HTML/CSS/JavaScript MCQs. Prepare 2 live project demos.")
                    .published(true)
                    .build());

            // A draft post to demonstrate the publish/unpublish workflow.
            placementRepository.save(Placement.builder()
                    .companyName("CloudNine Infra (DEMO)")
                    .jobTitle("Cloud Support Associate (DEMO)")
                    .description("Draft listing pending verification — do not share until published.")
                    .eligibility("Any graduate; AWS familiarity preferred.")
                    .skills("AWS, Linux, Networking")
                    .location("Hyderabad / Hybrid")
                    .jobType("Contract")
                    .deadline(LocalDate.now().plusDays(30))
                    .applicationUrl("https://example.com/apply/demo-cloud-support-associate")
                    .prepResources("Review the AWS MCQs and basic Linux commands.")
                    .published(false)
                    .build());
        };
    }
}
