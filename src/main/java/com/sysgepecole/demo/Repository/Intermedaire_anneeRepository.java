package com.sysgepecole.demo.Repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sysgepecole.demo.Models.Intermedaire_annee;

public interface Intermedaire_anneeRepository extends  JpaRepository<Intermedaire_annee, Long>{

	
	Optional<Intermedaire_annee> findById(Long idintermedaireannee);
	
	@Query("SELECT i FROM Intermedaire_annee i WHERE i.annee.idannee = :idannee")
	Optional<Intermedaire_annee> findByAnneeId(@Param("idannee") Long idannee);

	Optional<Intermedaire_annee> findByAnnee_IdanneeAndEcole_Idecole(Long idannee, long idecole);
   


}
