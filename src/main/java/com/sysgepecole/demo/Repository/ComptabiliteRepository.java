package com.sysgepecole.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sysgepecole.demo.Models.Comptabilite;

@Repository
public interface ComptabiliteRepository extends  JpaRepository<Comptabilite, Long>{

	
	@Query("SELECT COUNT(c) FROM Comptabilite c WHERE c.iduser = :iduser AND MONTH(c.dateOperation) = :mois AND YEAR(c.dateOperation) = :annee")
	int countByUserAndMonth(@Param("iduser") Long iduser, @Param("mois") int mois, @Param("annee") int annee);
	

	Optional<Comptabilite> findByIdannee(Long idannee);


}
