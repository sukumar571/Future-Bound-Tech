package com.futureboundtech.repository;

import com.futureboundtech.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudent_Id(Long studentId);

    List<Attendance> findByLiveClass_Id(Long liveClassId);

    Optional<Attendance> findByLiveClass_IdAndStudent_Id(Long liveClassId, Long studentId);
}
