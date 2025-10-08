package com.sysgepecole.demo.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.sysgepecole.demo.Models.Role;
import com.sysgepecole.demo.Service.RoleService;
import com.sysgepecole.demo.ServiceImpl.RoleServiceImpl;



@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/role")
public class RoleController {
	
	
	@Autowired
	RoleService roleservice;
	
	@Autowired
	RoleServiceImpl rolesservice;
	
	@GetMapping("/findRoleByRole/{ecole}")
	public Optional<Role> findRoleByRole(@PathVariable String role){
		return roleservice.findRoleByRole(role);
	}

	
	@PostMapping("/createRole")
	public Role createRole(@RequestBody Role role) {
		return roleservice.createRole(role);
	}
	
	
	@PutMapping("/updateRole/{idrole}")
	public ResponseEntity<Role> updateRole(@PathVariable Long idrole, @RequestBody Role roleDetails){
		return roleservice.updateRoles(idrole, roleDetails);
	}

	
	 @GetMapping("/getAllRole")
	    public ResponseEntity<?> getAllRole()  {
	        return roleservice.getAllRole();
	    }

	 @GetMapping
	    public List<String> getUserRoles() {
	        return rolesservice.getUserRoles();
	    }
	 
	 @GetMapping("/getRole/{role}")
	    public ResponseEntity<?> getRole(@PathVariable String role)  {
	        return roleservice.getRole(role);
	    }
}
