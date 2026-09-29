package com.futureboundtech.repository;

import com.futureboundtech.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    @Query("SELECT DISTINCT t FROM Trainer t JOIN FETCH t.user")
    List<Trainer> findAllWithUser();

    Optional<Trainer> findByUser_Id(Long userId);
}
