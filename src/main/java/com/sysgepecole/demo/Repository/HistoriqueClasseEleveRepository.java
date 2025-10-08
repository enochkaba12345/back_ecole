package com.sysgepecole.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.sysgepecole.demo.Models.HistoriqueClasseEleve;


@Repository
public interface HistoriqueClasseEleveRepository extends JpaRepository<HistoriqueClasseEleve, Long>{
	
	Optional<HistoriqueClasseEleve> findByIdeleveAndIdclasseAndIdannee(Long ideleve,Long idclasse,Long idannee);

	
	@Query("SELECT h.idclasse,h.idannee "
			+ "FROM HistoriqueClasseEleve h "
			+ "WHERE h.ideleve = :ideleve "
			+ "ORDER BY h.idannee ASC")
	List<Long> findByIdeleve(@Param("ideleve") Long ideleve);

	


}
