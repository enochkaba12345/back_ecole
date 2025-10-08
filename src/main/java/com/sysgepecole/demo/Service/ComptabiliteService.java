package com.sysgepecole.demo.Service;

import java.util.Date;

import org.springframework.http.ResponseEntity;

import com.sysgepecole.demo.Models.Comptabilite;

public interface ComptabiliteService {

	Comptabilite createComptabilite(Comptabilite comptabilite);
	ResponseEntity<Comptabilite> updateComptabilite(Long id, Comptabilite comptabilite);
	ResponseEntity<?> annulerOperation(Long id);
	ResponseEntity<?> ComptableId(Long iduser , Date dateDebut, Date dateFin);
	String updateEtape(Long id, String etape);

}
