package com.sysgepecole.demo.Repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sysgepecole.demo.Models.Intermedaire_classe;

public interface Intermedaire_classeRepository extends  JpaRepository<Intermedaire_classe, Long>{
	
	Optional<Intermedaire_classe> findById(Long idintermedaireclasse);
	
	
	
	  @Query("SELECT i FROM Intermedaire_classe i WHERE i.classe.idclasse = :idclasse"
	  ) Optional<Intermedaire_classe> findByClasseId(@Param("idclasse") Long idclasse);
	 


	Optional<Intermedaire_classe> findByClasse_IdclasseAndEcole_Idecole(Long idclasse, Long idecole);
	
	 

}
