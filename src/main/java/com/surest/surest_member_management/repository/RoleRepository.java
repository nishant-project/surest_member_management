
package com.surest.surest_member_management.repository;

import com.surest.surest_member_management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
}
