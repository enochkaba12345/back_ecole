package com.sysgepecole.demo.Service;

import java.util.Optional;

import com.sysgepecole.demo.Models.Classe;
import com.sysgepecole.demo.Models.Eleve;
import com.sysgepecole.demo.Models.HistoriqueClasseEleve;

public interface HistoriqueClasseEleveService {
	
	    Optional<HistoriqueClasseEleve> findHistoriqueClasseEleve(Long ideleve, Long idclasse, Long idannee);
	    Eleve passerClasse(Eleve historique);
	    void HistoriqueClasseParClasse(Long idintermedaireclasse, Long idclasse, Long idannee);
	    Classe getClasseById(Long idClasse);
		Eleve passerClasse(Eleve eleve, Classe nouvelleClasse, Long idannee);
	

}
