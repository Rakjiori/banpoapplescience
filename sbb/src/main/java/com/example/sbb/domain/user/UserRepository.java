package com.example.sbb.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<SiteUser, Long>, JpaSpecificationExecutor<SiteUser> {
    Optional<SiteUser> findByUsername(String username);
    Optional<SiteUser> findByRole(String role);
    List<SiteUser> findByRoleOrderByIdAsc(String role);
}
