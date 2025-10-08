package com.sysgepecole.demo.Repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.sysgepecole.demo.Models.Paiement;



@Repository
public interface PaiementRepository extends  JpaRepository<Paiement, Long>{
	
	List<Paiement> findByIdeleveAndIdintermedaireclasseAndIdintermedaireannee(Long ideleve, Long idintermedaireclasse,Long idintermedaireannee);

	List<Paiement> findByIdeleve(Long ideleve);
    @Query("SELECT COALESCE(SUM(p.montants), 0) FROM Paiement p WHERE p.ideleve = :ideleve AND p.idintermedaireclasse = :idClasse")
    double getTotalMontantsByIdeleve(@Param("ideleve") Long ideleve, @Param("idClasse") Long idintermedaireclasse);


    // aperçu simple : montants payés par interclasse
    @Query(value = """
      WITH paiements AS (
        SELECT ideleve, idintermedaireclasse, SUM(montants) AS montant_paye
        FROM tab_paiement
        GROUP BY ideleve, idintermedaireclasse
      ),
      frais AS (
        SELECT idintermedaireclasse, SUM(montant) AS montant_frais
        FROM tab_frais
        GROUP BY idintermedaireclasse
      )
      SELECT p.ideleve, p.idintermedaireclasse, COALESCE(pa.montant_paye,0) AS montant_paye, COALESCE(fr.montant_frais,0) AS montant_frais
      FROM (SELECT ideleve, idintermedaireclasse FROM tab_paiement WHERE ideleve = :ideleve
            UNION
            SELECT :ideleve AS ideleve, f.idintermedaireclasse FROM tab_frais f) p
      LEFT JOIN paiements pa ON p.ideleve = pa.ideleve AND p.idintermedaireclasse = pa.idintermedaireclasse
      LEFT JOIN frais fr ON p.idintermedaireclasse = fr.idintermedaireclasse
      """, nativeQuery = true)
    List<Object[]> getApercuPaiementsByEleveNative(@Param("ideleve") Long ideleve);

    // method to get remaining for current specific class quickly - native query
    @Query(value = "SELECT COALESCE((SELECT SUM(montant) FROM tab_frais WHERE idintermedaireclasse = :idinter),0) - COALESCE((SELECT SUM(montants) FROM tab_paiement WHERE ideleve = :ideleve AND idintermedaireclasse = :idinter),0)", nativeQuery = true)
    Double getMontantRestantForInterClasse(@Param("ideleve") Long ideleve, @Param("idinter") Long idintermedaireclasse);


	List<Paiement> findByIdeleveAndIdintermedaireclasseAndIdintermedaireannee(Long ideleve, Paiement idClassesActuelle,
			Paiement idAnneesActuelle);


	
}

	
    /*List<Paiement> findByIdeleveAndIdintermedaireclasse(long ideleve, Long idintermedaireclasse);
    

    @Query("SELECT COALESCE(SUM(p.montants), 0) FROM Paiement p WHERE p.ideleve = :ideleve AND p.idintermedaireclasse = :idintermedaireclasse")
    double getTotalMontantsByIdeleve(@Param("ideleve") Long ideleve, @Param("idintermedaireclasse") Long idintermedaireclasse);

    
    @Query(value = "SELECT COALESCE(SUM(f.montant), 0) - COALESCE(SUM(p.montants), 0) " +
            "FROM tab_Historique_classe_eleve h " +
            "JOIN tab_Intermedaireclasse ic ON h.idclasse = ic.idclasse  " +
            "JOIN tab_Intermedaireannee ia ON h.idannee = ia.idannee  " +
            "JOIN tab_Frais f ON f.idintermedaireclasse = ic.idintermedaireclasse " +
            "LEFT JOIN tab_Paiement p ON p.idintermedaireclasse = h.idclasse AND p.ideleve = h.ideleve " +
            "WHERE h.ideleve = :ideleve AND ic.idclasse = :idClasse", nativeQuery = true)
    double getDettePrecedente(@Param("ideleve") long ideleve, @Param("idClasse") Long idClasse);

*/



