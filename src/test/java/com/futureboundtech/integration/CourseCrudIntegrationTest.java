package com.futureboundtech.integration;

import com.futureboundtech.dto.CourseDto;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import com.futureboundtech.enums.TrainingMode;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Course lifecycle against the real {@link CourseService}: create (with slug
 * generation), read, publish so it becomes publicly visible, and safe delete.
 */
class CourseCrudIntegrationTest extends AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired CourseService courseService;

    private CourseDto newCourse() {
        return new CourseDto()
                .setTitle("Data Engineering Bootcamp")
                .setShortDescription("ETL, pipelines and warehouses")
                .setDetailedDescription("A rigorous, project-first data engineering track.")
                .setCategory(CourseCategory.FULL_STACK)
                .setLevel(CourseLevel.INTERMEDIATE)
                .setDurationMonths(6)
                .setFee(new BigDecimal("8999"))
                .setTrainingMode(TrainingMode.ONLINE);
    }

    @Test
    @DisplayName("create generates a slug, defaults to DRAFT and is not yet public")
    void createDefaultsToDraft() {
        CourseDto created = courseService.create(newCourse(), null);
        assertNotNull(created.getId());
        assertNotNull(created.getSlug());
        assertFalse(created.getSlug().isBlank());
        assertEquals(CourseStatus.DRAFT, created.getStatus());
        assertFalse(created.isPublicListed());
    }

    @Test
    @DisplayName("publish makes the course resolvable through the public catalog")
    void publishThenPublicLookup() {
        CourseDto created = courseService.create(newCourse(), null);
        assertEquals(created.getTitle(), courseService.findAdminById(created.getId()).getTitle());

        courseService.publish(created.getId());
        CourseDto publicView = courseService.findPublicBySlug(created.getSlug());
        assertEquals(CourseStatus.PUBLISHED, publicView.getStatus());
        assertTrue(publicView.isPublicListed());
    }

    @Test
    @DisplayName("safe delete hides the course from the public catalog")
    void deleteHidesCourse() {
        CourseDto created = courseService.create(newCourse(), null);
        courseService.publish(created.getId());
        courseService.deleteSafely(created.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> courseService.findPublicBySlug(created.getSlug()));
    }

    @Test
    @DisplayName("unpublish removes a course from the public catalog")
    void unpublishHidesCourse() {
        CourseDto created = courseService.create(newCourse(), null);
        courseService.publish(created.getId());
        assertNotNull(courseService.findPublicBySlug(created.getSlug()));

        courseService.unpublish(created.getId());
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.findPublicBySlug(created.getSlug()));
    }
}
