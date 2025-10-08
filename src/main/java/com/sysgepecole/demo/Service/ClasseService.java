package com.sysgepecole.demo.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.sysgepecole.demo.Dto.ClasseModelDto;
import com.sysgepecole.demo.Models.Classe;



public interface ClasseService {
	
	
	 ResponseEntity<Classe> updateClasses(Long idclasse, Classe classe);
	 ResponseEntity<Map<String, Boolean>> delete(Long idclasse);
	 List<Classe> getAllClasse();
	 ResponseEntity<?> CollecteClasse(long idniveau,Long idecole);
	 List<ClasseModelDto> FindClasseById(long idclasse);
	 Classe getClasseMontante(Classe classeActuelle);
	 ResponseEntity<?> getClasseActuelleEtMontante(long idclasse);
	 ResponseEntity<?> CollecteCategorieClasse(long idtranche,long idcategorie,long idecole);
	 ResponseEntity<?> CollecteCategorieClasseAdmin(long idtranche,long idcategorie);
	 ResponseEntity<?> CollecteEcole(long idecole);
	 ResponseEntity<?> CollecteEcoleAnnee(long idecole,long idannee);
	 Optional<Classe> findClasseByClasse(String classe);
	 Classe createClasses(Classe classe);
}
