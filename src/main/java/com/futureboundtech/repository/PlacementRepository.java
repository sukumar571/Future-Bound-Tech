package com.futureboundtech.repository;

import com.futureboundtech.entity.Placement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlacementRepository extends JpaRepository<Placement, Long> {

    List<Placement> findAllByOrderByCreatedAtDesc();

    List<Placement> findByPublishedTrueOrderByCreatedAtDesc();

    @Query("""
            select p from Placement p
            where p.published = true
              and lower(p.skills) like lower(concat('%', :skill, '%'))
            order by p.createdAt desc
            """)
    List<Placement> findPublishedBySkill(@Param("skill") String skill);

    @Query("""
            select distinct p from Placement p
            left join PlacementSave s on s.placement = p and s.student.id = :studentId
            where p.published = true
            order by (case when s.id is null then 1 else 0 end), p.createdAt desc
            """)
    List<Placement> findPublishedWithSavedFirst(@Param("studentId") Long studentId);

    @Query("""
            select p from Placement p
            join PlacementSave s on s.placement = p
            where s.student.id = :studentId
            order by p.createdAt desc
            """)
    List<Placement> findSavedByStudent(@Param("studentId") Long studentId);
}
