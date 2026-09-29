package com.futureboundtech.repository;

import com.futureboundtech.entity.InstituteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstituteSettingsRepository extends JpaRepository<InstituteSettings, Long> {

}
