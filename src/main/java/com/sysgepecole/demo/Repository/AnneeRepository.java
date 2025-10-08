package com.sysgepecole.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sysgepecole.demo.Models.Annee;


public interface AnneeRepository extends  JpaRepository<Annee, Long>{

	
	Optional<Annee> findByAnnee(String annee);
	
	Optional<Annee> findByIdannee(Long idannee);
	
	@Query("SELECT a FROM Annee a WHERE a.debut = :debut + 1")
	Annee findByOrder(@Param("debut") Long debut);

	Annee findByLibelle(String libelle);
	
	Optional<Annee> findTopByOrderByIdanneeDesc();


}
