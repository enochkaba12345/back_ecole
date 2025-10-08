package com.sysgepecole.demo.Service;

import com.sysgepecole.demo.Models.Annee;

public class AnneeUtils {



	public static Long extraireDebut(Annee annee) {
	    String anneeNom = annee.getAnnee(); // ex: "2024-2025"
	    String[] parties = anneeNom.trim().split("-");
	    if (parties.length != 2) {
	        throw new IllegalArgumentException("Format d'année invalide : " + anneeNom);
	    }
	    return Long.parseLong(parties[0]); // "2024" -> 2024L
	}

	public static Long extraireFin(Annee annee) {
	    String anneeNom = annee.getAnnee(); // ex: "2024-2025"
	    String[] parties = anneeNom.trim().split("-");
	    if (parties.length != 2) {
	        throw new IllegalArgumentException("Format d'année invalide : " + anneeNom);
	    }
	    return Long.parseLong(parties[1]); // "2025" -> 2025L
	}
	
	public static Long extraireOrdre(Annee annee) {
        if (annee == null || annee.getIdannee() == null) {
            throw new IllegalArgumentException("L'année ou son identifiant est nul.");
        }
        return annee.getIdannee();
    }


  

}
