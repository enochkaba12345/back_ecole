package com.sysgepecole.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.sysgepecole.demo.Models.Eleve;
import com.sysgepecole.demo.Repository.EleveRepository;
import com.sysgepecole.demo.Service.HistoriqueClasseEleveService;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/historique")
public class HistoriqueClasseEleveController {
	
	
	@Autowired
    private HistoriqueClasseEleveService historiqueClasseEleveService;
	
	@Autowired
    private EleveRepository eleveRepository;
	
	
	@PostMapping("/promouvoirEleveAuto/{ideleve}")
    public ResponseEntity<?> promouvoirEleveAuto(@PathVariable Long ideleve) {
        Eleve eleve = eleveRepository.findById(ideleve)
                .orElseThrow(() -> new RuntimeException("Élève non trouvé avec l'ID : " + ideleve));

        Eleve eleveMisAJour = historiqueClasseEleveService.passerClasse(eleve);
        return ResponseEntity.ok(eleveMisAJour);
    }
    
    @PostMapping("/promouvoirEleve")
    public ResponseEntity<String> promouvoirEleve(
            @RequestParam Long idintermedaireclasse,
            @RequestParam Long idclasse,
            @RequestParam Long idannee) {

    	historiqueClasseEleveService.HistoriqueClasseParClasse(idintermedaireclasse, idclasse, idannee);
        return ResponseEntity.ok("Les élèves ont été transférés avec succès !");
    }

    @PostMapping("/promouvoirClasse")
    public ResponseEntity<?> promouvoirClasse(
            @RequestParam Long idintermedaireclasse,
            @RequestParam Long idclasse,
            @RequestParam Long idannee) {

        try {
            historiqueClasseEleveService.HistoriqueClasseParClasse(idintermedaireclasse, idclasse, idannee);
            return ResponseEntity.ok("Promotion réussie pour la classe intermédiaire " + idintermedaireclasse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la promotion : " + e.getMessage());
        }
    }

}
