package com.sysgepecole.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sysgepecole.demo.Models.Classe;


public interface ClasseRepository extends  JpaRepository<Classe, Long>{

	
	Optional<Classe> findByClasse(String classe);
	
	@Query("SELECT c FROM Classe c WHERE c.ordre = :ordre + 1")
	Classe findByOrder(@Param("ordre") Long ordre);

	@Query("SELECT c FROM Classe c WHERE c.ordre = :ordre AND c.cycle = :cycle ORDER BY c.idclasse ASC")
	List<Classe> findClasseMontanteList(@Param("ordre") long ordre, @Param("cycle") String cycle);

	Optional<Classe> findByIdclasse(Long idclasse);







	
}
