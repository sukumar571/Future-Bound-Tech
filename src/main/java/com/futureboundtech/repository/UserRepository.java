package com.futureboundtech.repository;

import com.futureboundtech.entity.User;
import com.futureboundtech.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<com.futureboundtech.entity.User> findByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    long countByRole(Role role);
}
