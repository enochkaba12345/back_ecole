package com.sysgepecole.demo.Repository;




import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sysgepecole.demo.Models.Categorie_frais;
import com.sysgepecole.demo.Models.Frais;
import com.sysgepecole.demo.Models.Paiement;


public interface FraisRepository extends  JpaRepository<Frais, Long>{

	
Optional<Frais> findById(Long idfrais);
	
	
    List<Frais> findByIntermedaireClasse_IdintermedaireclasseAndIntermedaireAnnee_Idintermedaireannee(Long idintermedaireclasse, Long idintermedaireannee);
	
	
    List<Frais> findByIntermedaireClasse_Idintermedaireclasse(Long idinterclasse);
    
    @Query("SELECT f FROM Frais f " +
    	       "WHERE f.intermedaireClasse.classe.idclasse = :idclasse " +
    	       "AND f.intermedaireAnnee.annee.idannee = :idannee")
    	List<Frais> findByClasseAndAnnee(@Param("idclasse") Long idclasse,
    	                                     @Param("idannee") Long idannee);
    
   


    @Query("SELECT f FROM Frais f WHERE f.intermedaireClasse.id = :idclasse AND f.intermedaireAnnee.id = :idannee AND f.tranche.id = :idtranche")
    List<Frais> findFrais(@Param("idclasse") Long idclasse, @Param("idannee") Long idannee, @Param("idtranche") Long idtranche);
    
    @Query("SELECT f FROM Frais f WHERE f.intermedaireClasse.id = :idClasse AND f.intermedaireAnnee.id = :idAnnee AND f.tranche.idtranche = :idTranche")
    List<Frais> findByClasseAnneeTranche(@Param("idClasse") Long idClasse, @Param("idAnnee") Long idAnnee, @Param("idTranche") Long idTranche);


	List<Frais> findByIntermedaireClasse_IdintermedaireclasseAndIntermedaireAnnee_Idintermedaireannee(
			Paiement idClassesActuelle, Paiement idAnneesActuelle);

	@Query("SELECT COALESCE(SUM(f.montant), 0) FROM Frais f WHERE f.intermedaireClasse.id = :idintermedaireclasse AND f.intermedaireAnnee.id = :idintermedaireannee")
    double findMontantTotal(@Param("idintermedaireclasse") Long idintermedaireclasse,
                            @Param("idintermedaireannee") Long idintermedaireannee);




    
    




    






	
	
	
	
}
