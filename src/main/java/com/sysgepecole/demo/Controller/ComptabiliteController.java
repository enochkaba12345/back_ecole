package com.sysgepecole.demo.Controller;

import java.util.Date;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sysgepecole.demo.Models.Comptabilite;
import com.sysgepecole.demo.Service.ComptabiliteService;





@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/comptabilite")
public class ComptabiliteController {
	
	
	@Autowired
	ComptabiliteService comptabiliteservice;
	
	
	
	 @PostMapping("/createOperation")
	    public ResponseEntity<Comptabilite> createOperation(@RequestBody Comptabilite comptabilite) {
	        Comptabilite savedOperation= comptabiliteservice.createComptabilite(comptabilite);
	        return new ResponseEntity<>(savedOperation, HttpStatus.CREATED);
	    }
	 
	 @PutMapping("/UpdateOperation/{id}")
	    public ResponseEntity<Comptabilite> annuleOperation(@PathVariable Long id,@RequestBody Comptabilite comptabiliteDetails) {
	        return comptabiliteservice.updateComptabilite(id,comptabiliteDetails);
	    }
	 
	 
	 @PutMapping("/annuleOperation/{id}")
	    public ResponseEntity<?> annuleOperation(@PathVariable Long id) {
	        return comptabiliteservice.annulerOperation(id);
	    }
	 
	 @GetMapping("/ComptableIds/{iduser}/{dateDebut}/{dateFin}")
	    public ResponseEntity<?> ComptableIds(@PathVariable Long iduser,@PathVariable Date dateDebut,@PathVariable Date dateFin) {
	        return comptabiliteservice.ComptableId(iduser,dateDebut,dateFin);
	    }
	 
	 @PutMapping("/updateEtape/{id}/{etape}")
	   public ResponseEntity<String> updateEtape(@PathVariable Long id,@PathVariable String etape) {
			    try {
			        String response = comptabiliteservice.updateEtape(id, etape); 
			        return ResponseEntity.ok(response); 
			    } catch (RuntimeException ex) {
			        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
			    }
	   }

}
