package com.sysgepecole.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.sysgepecole.demo.Models.Role;
import com.sysgepecole.demo.Models.Users;


public interface RoleRepository extends  JpaRepository<Role, Long>{


	 List<Role> findById(long idrole);
	 List<Role> findByRole(Users idrole);
	 Optional<Role> findByRole(String role);


}
