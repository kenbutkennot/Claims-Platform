package com.example.claims.infrastructure;

import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findByRoleOrderByName(Role role);
    List<User> findByRoleInOrderByName(List<Role> roles);
}
