package com.sysgepecole.demo.Service;

import com.sysgepecole.demo.Models.Classe;

public class ClasseUtils {

		public static Long extraireOrdre(Classe classe) {
			  try {
				  String classeNom = classe.getClasse();
		            String[] parties = classeNom.trim().split(" ");
		            return Long.parseLong(parties[0]);
		        } catch (Exception e) {
		            throw new IllegalArgumentException("Impossible d'extraire l'ordre depuis : " + classe, e);
		        }
		}

		public static String extraireCycle(Classe classe) {
			String classeNom = classe.getClasse();
			 String[] parties = classeNom.trim().split(" ");
		        if (parties.length < 2) {
		            throw new IllegalArgumentException("Libellé classe invalide : " + classe);
		        }

		        return parties[2].toUpperCase(); // MATERNELLE
		}
	    
	 

}
