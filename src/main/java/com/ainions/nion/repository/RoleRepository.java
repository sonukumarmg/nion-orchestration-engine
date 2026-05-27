package com.ainions.nion.repository;

import com.ainions.nion.domain.Role;
import com.ainions.nion.domain.enums.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}
