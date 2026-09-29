package com.futureboundtech.repository;

import com.futureboundtech.entity.PlacementSave;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementSaveRepository extends JpaRepository<PlacementSave, Long> {

    boolean existsByStudentIdAndPlacementId(Long studentId, Long placementId);

    long countByPlacementId(Long placementId);

    void deleteByPlacementId(Long placementId);

    void deleteByStudentIdAndPlacementId(Long studentId, Long placementId);
}
