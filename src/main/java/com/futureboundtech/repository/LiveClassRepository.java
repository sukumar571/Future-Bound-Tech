package com.futureboundtech.repository;

import com.futureboundtech.entity.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {

    @Query("""
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.batch b
            JOIN FETCH b.course
            WHERE b.course.id IN :courseIds
              AND lc.startTime >= :from
              AND lc.status <> com.futureboundtech.enums.ClassStatus.CANCELLED
            ORDER BY lc.startTime ASC
            """)
    List<LiveClass> findUpcomingForCourses(@Param("courseIds") List<Long> courseIds,
                                           @Param("from") LocalDateTime from);

    @Query("""
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.batch b
            JOIN FETCH b.course
            WHERE b.course.id IN :courseIds
            ORDER BY lc.startTime DESC
            """)
    List<LiveClass> findAllForCourses(@Param("courseIds") List<Long> courseIds);

    @Query("""
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.batch b
            JOIN FETCH b.course
            WHERE b.id IN :batchIds
            ORDER BY lc.startTime DESC
            """)
    List<LiveClass> findAllForBatches(@Param("batchIds") List<Long> batchIds);

    List<LiveClass> findAllByOrderByStartTimeDesc();

    List<LiveClass> findByStartTimeGreaterThanEqualAndStatusNotOrderByStartTimeAsc(
            LocalDateTime from, com.futureboundtech.enums.ClassStatus excluded);

    /** Scheduled classes opening inside a reminder window; used by the reminder sweep. */
    @Query("""
            SELECT lc FROM LiveClass lc
            JOIN FETCH lc.batch b
            WHERE lc.startTime >= :from AND lc.startTime <= :to
              AND lc.status <> com.futureboundtech.enums.ClassStatus.CANCELLED
            ORDER BY lc.startTime ASC
            """)
    List<LiveClass> findStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
