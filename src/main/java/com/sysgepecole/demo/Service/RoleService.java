package com.sysgepecole.demo.Service;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.sysgepecole.demo.Models.Role;

public interface RoleService {

	 ResponseEntity<?> getAllRole();
	 ResponseEntity<?> getRole(String role);
	 ResponseEntity<Role> updateRoles(Long idrole, Role role);
	 Role createRole(Role role);
	 Optional<Role> findRoleByRole(String role);
}
