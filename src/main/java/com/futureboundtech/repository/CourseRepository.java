package com.futureboundtech.repository;

import com.futureboundtech.entity.Course;
import com.futureboundtech.enums.CourseCategory;
import com.futureboundtech.enums.CourseLevel;
import com.futureboundtech.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndDeletedFalse(Long id);

    Optional<Course> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalseAndIdNot(String slug, Long id);

    List<Course> findByDeletedFalseOrderByTitleAsc();

    List<Course> findByTrainer_IdAndDeletedFalseOrderByTitleAsc(Long trainerId);

    @Query("""
            SELECT DISTINCT c FROM Course c
            LEFT JOIN FETCH c.trainer t
            LEFT JOIN FETCH t.user
            WHERE c.deleted = false
              AND c.status = :status
              AND c.publicListed = true
              AND (:query IS NULL OR :query = ''
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:category IS NULL OR c.category = :category)
              AND (:level IS NULL OR c.level = :level)
            ORDER BY c.title ASC
            """)
    List<Course> searchPublicCatalog(@Param("query") String query,
                                     @Param("category") CourseCategory category,
                                     @Param("level") CourseLevel level,
                                     @Param("status") CourseStatus status);

    @Query("""
            SELECT DISTINCT c FROM Course c
            LEFT JOIN FETCH c.trainer t
            LEFT JOIN FETCH t.user
            WHERE c.deleted = false
              AND (:query IS NULL OR :query = ''
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:category IS NULL OR c.category = :category)
              AND (:level IS NULL OR c.level = :level)
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.title ASC
            """)
    List<Course> searchAdmin(@Param("query") String query,
                             @Param("category") CourseCategory category,
                             @Param("level") CourseLevel level,
                             @Param("status") CourseStatus status);

    long countByDeletedFalse();

    long countByDeletedFalseAndStatus(CourseStatus status);

    long countByDeletedFalseAndPublicListedTrue();
}
