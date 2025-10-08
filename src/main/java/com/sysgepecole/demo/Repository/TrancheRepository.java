package com.sysgepecole.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.sysgepecole.demo.Models.Tranche;


public interface TrancheRepository extends  JpaRepository<Tranche, Long>{
	
	Optional<Tranche> findByTranche(String tranche);

	Optional<Tranche> findById(Long idtranche);
	
	

}
