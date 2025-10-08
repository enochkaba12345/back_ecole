package com.sysgepecole.demo.Service;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

import org.springframework.http.ResponseEntity;

import com.sysgepecole.demo.Dto.PaiementDto;
import com.sysgepecole.demo.Models.Paiement;

import net.sf.jasperreports.engine.JRException;

public interface PaiementService {


	 Paiement createPaiement(Paiement paiement);
	 ResponseEntity<?> findByIdeleve(long ideleve, long idintermedaireclasse);
	 ResponseEntity<?> CollecteFraisPayerEleve(long ideleve,long idtranche,long idcategorie);
	 ResponseEntity<?> CollectionPaiement(Long ideleve);
	 ResponseEntity<?> getPaiementsByEleve(Long ideleve);
	 ResponseEntity<?> CollectionPaiementses(long idecole);
	 ResponseEntity<?> RapportjournalierCaisse(Long iduser);
	 ResponseEntity<?> RapportjournalierCaisseId(Long iduser);
	 ResponseEntity<?> InventaireCaissesId(Long iduser , Date dateDebut, Date dateFin);
	 ResponseEntity<?> RapportjournalierDesCaisse(String role);
	 ResponseEntity<?> ImpressionRapportjournalierCaisse(Long iduser) throws FileNotFoundException, JRException;
	 ResponseEntity<?> ImpressionRapportjournalierDesCaisseId(Long iduser) throws FileNotFoundException, JRException;
	 ResponseEntity<?> PaiementActuelledashbord();
	 ResponseEntity<?> annulerPaiement(Long idpaiement);
	 ResponseEntity<?> SectionPaiementActuelledashbord();
	 ResponseEntity<?> CollectionPaiementdashbord(long idecole,long idclasse,long idannee);
	 ResponseEntity<?> CollectionPaiementMode(Long idpaiement);
	 ResponseEntity<?> ImpressionRecuEleveAcompte(long ideleve) throws FileNotFoundException, JRException;
	 ResponseEntity<?> ImpressionRecuEleveSolde(long ideleve) throws FileNotFoundException, JRException;
	 ResponseEntity<?> ImpressionRecuModeEleveAcompte(long idpaiement) throws FileNotFoundException, JRException;
	 ResponseEntity<?> ImpressionRecuModeEleveSolde(long idpaiement) throws FileNotFoundException, JRException;
	 ResponseEntity<?> ImpressionRecuModeEleve(long idpaiement) throws FileNotFoundException, JRException;
	 ResponseEntity<?> EcoleParClasse(Long idecole,Long idclasse,Long idannee) ;
	 ResponseEntity<?> CollecteAnnulation(Long idecole,Long idclasse,Long idannee) ;
	 ResponseEntity<?> FichePaiementeleve(Long ideleve,Long idclasse,Long idannee) throws FileNotFoundException, JRException;
	 List<PaiementDto> searchPaiements(String nom, Long idecole, boolean isAdmin);
	 ResponseEntity<?> PaiementDeleve(Long ideleve);
	 ResponseEntity<?> PaiementGroupeleve(Long ideleve);
	 ResponseEntity<?> PaiementGroupDetail(Long ideleve,Long idintermedaireclasse,Long idintermedaireannee);
	 ResponseEntity<Paiement> updatePaiement(Long idpaiement, Paiement paiement);
	 ResponseEntity<?> CollectionPaiementAcompte(Long ideleve);
	 ResponseEntity<?> CollectionPaiementSolde(Long ideleve);
	 ResponseEntity<?> FicheRecouvrementClasse(long idecole, long idclasse, long idannee) throws FileNotFoundException, JRException;
	 void enregistrerPaiement(String username, String nom, String classe, String ecole, String annee, double montant, String frais);
	 ResponseEntity<?> ImpressionInventaireCaissesId(Long iduser, Date dateDebut, Date dateFin)
			throws FileNotFoundException, JRException;
	 ResponseEntity<?> FicheAnnuler(Long idecole, Long idclasse, Long idannee) throws FileNotFoundException, JRException;



}
