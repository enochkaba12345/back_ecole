package com.sysgepecole.demo.ServiceImpl;

import java.io.FileNotFoundException;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.sysgepecole.demo.Dto.EleveModelDto;
import com.sysgepecole.demo.Dto.PaiementDto;
import com.sysgepecole.demo.Models.Categorie_frais;
import com.sysgepecole.demo.Models.Comptabilite;
import com.sysgepecole.demo.Models.Frais;
import com.sysgepecole.demo.Models.HistoriqueClasseEleve;
import com.sysgepecole.demo.Models.NumberToWords;
import com.sysgepecole.demo.Models.Paiement;
import com.sysgepecole.demo.Models.Tranche;
import com.sysgepecole.demo.Models.Users;
import com.sysgepecole.demo.Models.reportBase64;
import com.sysgepecole.demo.Repository.CategorieRepository;
import com.sysgepecole.demo.Repository.ComptabiliteRepository;
import com.sysgepecole.demo.Repository.EleveRepository;
import com.sysgepecole.demo.Repository.FraisRepository;
import com.sysgepecole.demo.Repository.HistoriqueClasseEleveRepository;
import com.sysgepecole.demo.Repository.PaiementRepository;
import com.sysgepecole.demo.Repository.TrancheRepository;
import com.sysgepecole.demo.Repository.UsersRepository;
import com.sysgepecole.demo.Service.PaiementService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class PaiementServiceImpl implements PaiementService {

	private static final Logger logger = LoggerFactory.getLogger(PaiementServiceImpl.class);

	@Autowired
	public PaiementRepository paiementRepository;

	@Autowired
	private EleveRepository eleverepository;

	@Autowired
	private HistoriqueClasseEleveRepository historiqueclasseeleverepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	public FraisRepository fraisRepository;

	@Autowired
	public TrancheRepository trancheRepository;

	@Autowired
	public CategorieRepository categorieRepository;

	@Autowired
	public ComptabiliteRepository comptabiliteRepository;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public Paiement createPaiement(Paiement paiement) {
		if (paiement.getIduser() == null) {
			throw new IllegalArgumentException("L'ID utilisateur est requis pour effectuer le paiement.");
		}

		// Paiements déjà effectués pour la classe actuelle
		List<Paiement> paiementsActuels = paiementRepository.findByIdeleveAndIdintermedaireclasseAndIdintermedaireannee(
				paiement.getIdeleve(), paiement.getIdintermedaireclasse(), paiement.getIdintermedaireannee());

		// Frais de la classe actuelle
		List<Frais> fraisList = fraisRepository.findByClasseAndAnnee(paiement.getIdintermedaireclasse(),
				paiement.getIdintermedaireannee());
		if (fraisList.isEmpty()) {
			throw new IllegalStateException("Structure des frais introuvable pour cette classe et année.");
		}

		double totalMontantClasseActuelle = fraisList.stream().mapToDouble(Frais::getMontant).sum();
		double montantPaiement = paiement.getMontants() != null ? paiement.getMontants() : 0.0;

		// 1️⃣ Gestion dettes des classes précédentes
		List<Long> idClassesPrecedentes = historiqueclasseeleverepository.findByIdeleve(paiement.getIdeleve());
		List<Long> idAnneesPrecedentes = historiqueclasseeleverepository.findByIdeleve(paiement.getIdeleve());

		for (int i = 0; i < idClassesPrecedentes.size(); i++) {
			Long idClasse = idClassesPrecedentes.get(i);
			Long idAnnee = idAnneesPrecedentes.get(i);

			List<Frais> fraisPrecedents = fraisRepository
					.findByIntermedaireClasse_IdintermedaireclasseAndIntermedaireAnnee_Idintermedaireannee(idClasse,
							idAnnee);
			List<Paiement> paiementsPrecedents = paiementRepository
					.findByIdeleveAndIdintermedaireclasseAndIdintermedaireannee(paiement.getIdeleve(), idClasse,
							idAnnee);

			for (Frais f : fraisPrecedents) {
				if (f == null || f.getCategorie() == null)
					continue;

				double dejaPaye = paiementsPrecedents.stream().filter(
						p -> p.getCategorie() != null && p.getCategorie().equals(f.getCategorie().getCategorie()))
						.mapToDouble(Paiement::getMontants).sum();
				

				double reste = f.getMontant() - dejaPaye;
				if (reste > 0 && montantPaiement > 0) {
					double montantARepartir = Math.min(reste, montantPaiement);
					Paiement pDette = new Paiement();
					pDette.setIdeleve(paiement.getIdeleve());
					pDette.setIduser(paiement.getIduser());
					pDette.setCategorie(f.getCategorie().getCategorie());
					pDette.setMontants(montantARepartir);
					pDette.setIdintermedaireclasse(idClasse);
					pDette.setIdintermedaireannee(idAnnee);
					pDette.setAnnule(false);
					pDette.setStatut(montantARepartir >= reste ? "SOLDER" : "ACOMPTE");

					Tranche tranche = trancheRepository.findById(Long.parseLong(paiement.getTranche()))
							.orElseThrow(() -> new RuntimeException("Tranche introuvable : " + paiement.getTranche()));

					String statut = (montantARepartir >= reste) ? "SOLDER" : "ACOMPTE";

					String libelleFrais = statut.equals("SOLDER") ? "VOUS AVEZ SOLDER TOUS LES FRAIS CLASSE PRECEDENTE"
							: tranche.getTranche() + " " + f.getCategorie().getCategorie() + " " + statut
									+ " CLASSE PRECEDENTE";

					pDette.setTranche(tranche.getTranche());
					pDette.setFrais(libelleFrais);
					paiementRepository.save(pDette);
					
					Optional<Comptabilite>existing = comptabiliteRepository.findByIdannee(paiement.getIdintermedaireannee());
					
					Comptabilite comptabilite;
					
					if (existing.isPresent()) {
						comptabilite = existing.get();
						double monatantActuel = comptabilite.getMontants();
						comptabilite.setMontants(monatantActuel + montantARepartir);
						 comptabilite.setTypeoperation("ENCAISSEMENT"); 
					
						
					} else {
						comptabilite = new Comptabilite();
						comptabilite.setIdannee(paiement.getIdintermedaireannee());
						comptabilite.setMontants(montantARepartir);
						comptabilite.setTypeoperation("ENCAISSEMENT"); 
						
					}
					
					comptabiliteRepository.save(comptabilite);
					
					montantPaiement -= montantARepartir;
				}
			}
		}

		// 2️⃣ Paiement de la classe actuelle
		if (montantPaiement > totalMontantClasseActuelle) {
			double excedent = montantPaiement - totalMontantClasseActuelle;
			montantPaiement = totalMontantClasseActuelle;
			// envoyer notification utilisateur: excédent rejeté
			System.out.println("Attention: excédent rejeté " + excedent + " FC");
		}

		for (Frais f : fraisList) {
			double dejaPaye = paiementsActuels.stream()
					.filter(p -> p.getCategorie() != null && p.getCategorie().equals(f.getCategorie().getCategorie()))
					.mapToDouble(Paiement::getMontants).sum();

			double reste = f.getMontant() - dejaPaye;
			if (reste > 0 && montantPaiement > 0) {
				double montantARepartir = Math.min(reste, montantPaiement);
				Paiement pActuelle = new Paiement();
				pActuelle.setIdeleve(paiement.getIdeleve());
				pActuelle.setIduser(paiement.getIduser());
				pActuelle.setCategorie(f.getCategorie().getCategorie());
				pActuelle.setMontants(montantARepartir);
				pActuelle.setAnnule(false);
				pActuelle.setIdintermedaireclasse(paiement.getIdintermedaireclasse());
				pActuelle.setIdintermedaireannee(paiement.getIdintermedaireannee());
				pActuelle.setStatut(montantARepartir >= reste ? "SOLDER" : "ACOMPTE");

				Tranche tranche = trancheRepository.findById(Long.parseLong(paiement.getTranche()))
						.orElseThrow(() -> new RuntimeException("Tranche introuvable : " + paiement.getTranche()));

				String statut = (montantARepartir >= reste) ? "SOLDER" : "ACOMPTE";

				String libelleFrais = statut.equals("SOLDER") ? "VOUS AVEZ SOLDER TOUS LES FRAIS"
						: tranche.getTranche() + " " + f.getCategorie().getCategorie() + " " + statut;

				pActuelle.setTranche(tranche.getTranche());
				pActuelle.setFrais(libelleFrais);
				paiementRepository.save(pActuelle);
				
				Optional<Comptabilite>existing = comptabiliteRepository.findByIdannee(paiement.getIdintermedaireannee());
				
				Comptabilite comptabilite;
				
				if (existing.isPresent()) {
					comptabilite = existing.get();
					double monatantActuel = comptabilite.getMontants();
					comptabilite.setMontants(monatantActuel + montantARepartir);
					comptabilite.setTypeoperation("ENCAISSEMENT"); 
					
				} else {
					comptabilite = new Comptabilite();
					comptabilite.setIdannee(paiement.getIdintermedaireannee());
					comptabilite.setMontants(montantARepartir);
					 comptabilite.setTypeoperation("ENCAISSEMENT"); 
					
				}
				
			
				comptabiliteRepository.save(comptabilite);

				montantPaiement -= montantARepartir;
			}
		}

		return paiement;
	}

	@Override
	public ResponseEntity<Paiement> updatePaiement(Long idpaiement, Paiement paiement) {
		try {
			Optional<Paiement> paiementData = paiementRepository.findById(idpaiement);

			if (!paiementData.isPresent()) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

			Paiement _paiement = paiementData.get();

			// 🔍 Récupérer tous les paiements de l'élève sauf celui qu'on modifie
			List<Paiement> paiementsExistants = paiementRepository.findByIdeleve(paiement.getIdeleve()).stream()
					.filter(p -> !Long.valueOf(p.getIdpaiement()).equals(idpaiement)).collect(Collectors.toList());

			// 💰 Total payé sur toutes les classes
			double totalPaye = paiementsExistants.stream()
					.mapToDouble(p -> p.getMontants() != null ? p.getMontants() : 0).sum();

			// 💰 Montant total dû pour la classe actuelle
			double montantClasseActuelle = fraisRepository.findMontantTotal(paiement.getIdintermedaireclasse(),
					paiement.getIdintermedaireannee());

			// 🔹 Vérification dettes sur les classes précédentes
			double dettesPrecedentes = paiementsExistants.stream()
					.filter(p -> !p.getIdintermedaireclasse().equals(paiement.getIdintermedaireclasse())
							|| !p.getIdintermedaireannee().equals(paiement.getIdintermedaireannee()))
					.mapToDouble(p -> {
						double totalFrais = fraisRepository.findMontantTotal(p.getIdintermedaireclasse(),
								p.getIdintermedaireannee());
						return Math.max(totalFrais - p.getMontants(), 0);
					}).sum();

			if (dettesPrecedentes > 0) {
				// ⚠️ L'élève doit d'abord payer les dettes précédentes
				paiement.setMontants(Math.min(paiement.getMontants(), dettesPrecedentes));
			} else {
				// Ajuster pour la classe actuelle
				double montantRestant = montantClasseActuelle - paiementsExistants.stream()
						.filter(p -> p.getIdintermedaireclasse().equals(paiement.getIdintermedaireclasse())
								&& p.getIdintermedaireannee().equals(paiement.getIdintermedaireannee()))
						.mapToDouble(p -> p.getMontants() != null ? p.getMontants() : 0).sum();

				if (paiement.getMontants() > montantRestant) {
					paiement.setMontants(montantRestant);
				}
			}

			// 🔄 Mise à jour des champs
			_paiement.setIdeleve(paiement.getIdeleve());
			_paiement.setIdintermedaireclasse(paiement.getIdintermedaireclasse());
			_paiement.setIdintermedaireannee(paiement.getIdintermedaireannee());
			_paiement.setFrais(paiement.getFrais());
			_paiement.setMontants(paiement.getMontants());
			_paiement.setIduser(paiement.getIduser());
			_paiement.setAnnule(false);

			return new ResponseEntity<>(paiementRepository.save(_paiement), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private List<PaiementDto> findByIdeleves(long ideleve, long idintermedaireclasse) {
		String query = "SELECT UPPER(b.nom) AS nom ,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, UPPER(b.sexe) AS sexe, "
				+ "a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, "
				+ "c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue "
				+ "FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idintermedaireannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON b.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "WHERE b.ideleve = :ideleve AND c.idintermedaireclasse = :idintermedaireclasse "
				+ "GROUP BY b.ideleve, a.idecole, e.idclasse, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee "
				+ "ORDER BY b.ideleve";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve)
				.addValue("idintermedaireclasse", idintermedaireclasse);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> findByIdeleve(long ideleve, long idintermedaireclasse) {
		List<PaiementDto> collections = findByIdeleves(ideleve, idintermedaireclasse);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ce nom.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> CollecteFraisPayerEleves(long ideleve, long idtranche, long idcategorie) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, "
				+ "c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, d2.montant, b.telephone,"
				+ "CASE WHEN n.annule = false THEN n.montants ELSE 0 END AS montants " + " FROM tab_Eleve b "
				+ " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idintermedaireannee "
				+ " JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ " JOIN tab_Frais d2 ON b.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ " JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Paiement n ON n.ideleve = b.ideleve   "
				+ " WHERE b.ideleve = :ideleve AND l.idtranche = :idtranche AND k.idcategorie = :idcategorie "
				+ " GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, l.idtranche, n.idpaiement,d2.idfrais "
				+ " ORDER BY b.ideleve ";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve)
				.addValue("idtranche", idtranche).addValue("idcategorie", idcategorie);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollecteFraisPayerEleve(long ideleve, long idtranche, long idcategorie) {
		List<PaiementDto> collections = CollecteFraisPayerEleves(ideleve, idtranche, idcategorie);

		if (collections.isEmpty()) {

			return ResponseEntity.ok(collections);
		} else {

			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> CollectionPaiements(Long ideleve) {
		String query = "SELECT UPPER(b.nom) AS nom, b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ " a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
				+ " c.idintermedaireclasse, d1.idintermedaireannee, f.idannee,  UPPER(n.categorie) AS categorie, "
				+ " UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.datepaie, UPPER(n.frais) as frais, "
				+ " m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ "CASE WHEN n.annule = false THEN n.montants ELSE 0 END AS montants, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(y.logos, ''), 'logos.png')) AS logos, n.statut, "
				+ " (SELECT SUM(d2_sub.montant) " + " FROM tab_Frais d2_sub "
				+ " WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse "
				+ " AND d2_sub.idintermedaireannee = d1.idintermedaireannee) AS montant_frais, "
				+ " (SELECT SUM(n_sub.montants) " + " FROM tab_Paiement n_sub "
				+ " WHERE n_sub.ideleve = b.ideleve) AS montant_paiement " + " FROM tab_Eleve b "
				+ " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idannee "
				+ " JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ " JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ " JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune " + " JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ "LEFT JOIN tab_Logos y ON y.idecole = a.idecole " + " WHERE b.ideleve = :ideleve "
				+ " GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse, h.idcommune,"
				+ " m.idprovince, c.idintermedaireclasse, d1.idintermedaireannee, "
				+ "f.idannee, k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais,y.logos "
				+ " ORDER BY n.idpaiement DESC LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollectionPaiement(Long ideleve) {
		List<PaiementDto> collections = CollectionPaiements(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> ImpressionRecuEleveAcomptes(long ideleve) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ "a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
				+ "c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.montants AS montant_paiement,n.datepaie, UPPER(n.frais) as frais, "
				+ "m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune,UPPER(z.username) AS username,z.iduser, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos, x.id "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_User z ON z.iduser = n.iduser " + "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "WHERE b.ideleve = :ideleve "
				+ "GROUP BY b.ideleve,b.matricule, a.idecole,x.id, e.idclasse, h.idcommune, m.idprovince,z.iduser,c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais "
				+ "ORDER BY n.idpaiement DESC LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}

	}

	public ResponseEntity<?> ImpressionRecuEleveAcompte(long ideleve) {
		try {

			List<PaiementDto> collections = ImpressionRecuEleveAcomptes(ideleve);
			System.out.println(collections);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("NumberToWords", new NumberToWords());

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(
					ResourceUtils.getFile("classpath:etats/Recu.jrxml").getAbsolutePath()), parameters, ds);
			
			   System.out.println("Page width: " + reportlist.getPageWidth());
	            System.out.println("Page height: " + reportlist.getPageHeight());

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> ImpressionRecuEleveSoldes(long ideleve) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ " a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, "
				+ " n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, UPPER(n.categorie) AS categorie, "
				+ " UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.datepaie, UPPER(n.frais) AS frais, "
				+ " m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune,UPPER(z.username) AS username,z.iduser, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos, x.id, "
				+ " (SELECT SUM(d2_sub.montant) " + " FROM tab_Frais d2_sub "
				+ " WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) AS montant_frais, "
				+ " (SELECT SUM(n_sub.montants) " + " FROM tab_Paiement n_sub "
				+ " WHERE n_sub.ideleve = b.ideleve) AS montant_paiement, " + " CASE " + " WHEN "
				+ " (SELECT SUM(d2_sub.montant) " + " FROM tab_Frais d2_sub "
				+ " WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) = "
				+ " (SELECT SUM(n_sub.montants) " + " FROM tab_Paiement n_sub " + " WHERE n_sub.ideleve = b.ideleve) "
				+ " THEN " + " (SELECT SUM(n_sub.montants) " + " FROM tab_Paiement n_sub "
				+ " WHERE n_sub.ideleve = b.ideleve) " + " ELSE 0 " + " END AS montants " + " FROM tab_Eleve b "
				+ " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idannee "
				+ " JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ " JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ " JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune " + " JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ " JOIN tab_User z ON z.iduser = n.iduser " + " LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ " WHERE b.ideleve = :ideleve "
				+ " GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse, h.idcommune, m.idprovince, "
				+ " c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ " k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais,z.iduser,x.id "
				+ " ORDER BY n.idpaiement DESC LIMIT 1 ";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> ImpressionRecuEleveSolde(long idpaiement) {
		try {
			List<PaiementDto> collections = ImpressionRecuEleveSoldes(idpaiement);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("NumberToWords", new NumberToWords());

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(
					ResourceUtils.getFile("classpath:etats/Recu.jrxml").getAbsolutePath()), parameters, ds);
			
			 System.out.println("Page width: " + reportlist.getPageWidth());
	            System.out.println("Page height: " + reportlist.getPageHeight());

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> ImpressionRecuModeEleveSoldes(long idpaiement) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ "	a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, "
				+ "	n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, UPPER(n.categorie) AS categorie, "
				+ "	UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.datepaie, UPPER(n.frais) AS frais, "
				+ "	m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune,UPPER(z.username) AS username,z.iduser, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos,x.id,  "
				+ "	(SELECT SUM(d2_sub.montant) " + "	FROM tab_Frais d2_sub "
				+ "	WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) AS montant_frais, "
				+ "	(SELECT SUM(n_sub.montants) " + "	FROM tab_Paiement n_sub "
				+ "	WHERE n_sub.ideleve = b.ideleve) AS montant_paiement, " + "	CASE " + "	WHEN "
				+ "	(SELECT SUM(d2_sub.montant) " + "	FROM tab_Frais d2_sub "
				+ "	WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) = "
				+ "	(SELECT SUM(n_sub.montants) " + "	FROM tab_Paiement n_sub "
				+ "	WHERE n_sub.ideleve = b.ideleve) " + "	THEN " + "	(SELECT SUM(n_sub.montants) "
				+ "	FROM tab_Paiement n_sub " + "	WHERE n_sub.ideleve = b.ideleve) " + "	ELSE 0 "
				+ "	END AS montants " + "	FROM tab_Eleve b "
				+ "	JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "	JOIN tab_Classe e ON c.idclasse = e.idclasse " + "	JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "	JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idannee "
				+ "	JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "	JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "	JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "	JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ "	JOIN tab_Paiement n ON n.ideleve = b.ideleve " + "	JOIN tab_User z ON z.iduser = n.iduser "
				+ " LEFT JOIN tab_Logos x ON x.idecole = a.idecole " + "	WHERE n.idpaiement = :idpaiement "
				+ "	GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse, h.idcommune, m.idprovince, "
				+ "	c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ " k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais,z.iduser,x.id "
				+ "	ORDER BY n.idpaiement DESC LIMIT 1 ";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idpaiement", idpaiement);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> ImpressionRecuModeEleveSolde(long idpaiement) {
		try {
			List<PaiementDto> collections = ImpressionRecuModeEleveSoldes(idpaiement);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("NumberToWords", new NumberToWords());

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(
					ResourceUtils.getFile("classpath:etats/Recu.jrxml").getAbsolutePath()), parameters, ds);
			
			 System.out.println("Page width: " + reportlist.getPageWidth());
	            System.out.println("Page height: " + reportlist.getPageHeight());

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> ImpressionRecuModeEleveAcomptes(long idpaiement) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ "	a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
				+ "	c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "	UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.montants as montant_paiement,n.datepaie, UPPER(n.frais) as frais, "
				+ "	m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune,UPPER(z.username) AS username,z.iduser,x.id, "
				+ "	CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos,x.id  "
				+ "	FROM tab_Eleve b " + "	JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ "	JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "	JOIN tab_Classe e ON c.idclasse = e.idclasse " + "	JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "	JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "	JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "	JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "	JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "	JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ "	JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "	JOIN tab_Commune h ON h.idcommune = a.idcommune " + "	JOIN tab_User z ON z.iduser = n.iduser "
				+ "	LEFT JOIN tab_Logos x ON x.idecole = a.idecole " + "	WHERE n.idpaiement = :idpaiement"
				+ "	GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse,x.id, h.idcommune, m.idprovince,z.iduser, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais "
				+ "	ORDER BY n.idpaiement LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idpaiement", idpaiement);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> ImpressionRecuModeEleveAcompte(long idpaiement) {
		try {
			List<PaiementDto> collections = ImpressionRecuModeEleveAcomptes(idpaiement);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("NumberToWords", new NumberToWords());

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(
					ResourceUtils.getFile("classpath:etats/Recu.jrxml").getAbsolutePath()), parameters, ds);
			
			 System.out.println("Page width: " + reportlist.getPageWidth());
	            System.out.println("Page height: " + reportlist.getPageHeight());

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> ImpressionRecuModeEleves(long idpaiement) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, "
				+ "    UPPER(e.classe) AS classe, n.idpaiement, ic.idintermedaireclasse, ia.idintermedaireannee, "
				+ "    f.idannee, UPPER(f.annee) AS annee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "    l.idtranche, UPPER(l.tranche) AS tranche, n.montants AS montant_paiement, n.datepaie, "
				+ "    UPPER(n.frais) AS frais, m.idprovince, UPPER(m.province) AS province, h.idcommune, "
				+ "    UPPER(h.commune) AS commune, UPPER(z.username) AS username, z.iduser, "
				+ "    x.id, CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
				+ "FROM tab_Paiement n " + "JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse ic ON n.idintermedaireclasse = ic.idclasse "
				+ "JOIN tab_Classe e ON ic.idclasse = e.idclasse " + "JOIN tab_Ecole a ON ic.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Annee f ON ia.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d2.idintermedaireclasse = ic.idintermedaireclasse AND d2.idintermedaireannee = ia.idintermedaireannee "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune " + "JOIN tab_User z ON z.iduser = n.iduser "
				+ "LEFT JOIN tab_Logos x ON x.idecole = a.idecole " + "WHERE n.idpaiement = :idpaiement "
				+ "ORDER BY n.idpaiement LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idpaiement", idpaiement);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> ImpressionRecuModeEleve(long idpaiement) {
		try {
			List<PaiementDto> collections = ImpressionRecuModeEleves(idpaiement);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("NumberToWords", new NumberToWords());

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(
					ResourceUtils.getFile("classpath:etats/Recu.jrxml").getAbsolutePath()), parameters, ds);

		    System.out.println("Page width: " + reportlist.getPageWidth());
            System.out.println("Page height: " + reportlist.getPageHeight());
			
			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
		
	}

	public List<PaiementDto> searchPaiements(String nom, Long idecole, boolean isAdmin) {

		StringBuilder query = new StringBuilder("SELECT UPPER(b.nom) AS nom,b.matricule, " + "       UPPER(b.postnom) AS postnom, "
				+ "       UPPER(b.prenom) AS prenom, " + "       UPPER(b.sexe) AS sexe, " + "       b.ideleve, "
				+ "       a.idecole, " + "       UPPER(a.ecole) AS ecole, " + "       e.idclasse, "
				+ "       UPPER(e.classe) AS classe, " + "      SUM(n.montants) AS montants, "
				+ "       MAX(n.datepaie) AS datepaie, " + "       f.idannee, " + "       x.id, "
				+ "       COALESCE(NULLIF(x.photo, ''), 'http://localhost:8080/uploads/icon.jpg') AS photo, "
				+ "       UPPER(f.annee) AS annee, " + "       (SELECT UPPER(n2.frais) "
				+ "        FROM tab_Paiement n2 " + "        WHERE n2.ideleve = b.ideleve "
				+ "        ORDER BY n2.datepaie DESC LIMIT 1) AS frais " + "FROM tab_Eleve b "
				+ "JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee " + "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "WHERE LOWER(b.nom) LIKE :nom " + "  AND f.idannee = ( " + "       SELECT MAX(f2.idannee) "
				+ "       FROM tab_Paiement n2 "
				+ "       JOIN tab_Intermedaireannee d2 ON n2.idintermedaireannee = d2.idannee "
				+ "       JOIN tab_Annee f2 ON d2.idannee = f2.idannee "
				+ "       WHERE n2.ideleve = b.ideleve AND n2.annule = false " + "   )  AND n.annule =  false ");

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("nom",
				"" + nom.toLowerCase().trim() + "%");

		if (!isAdmin) {
			query.append("AND a.idecole = :idecole ");
			parameters.addValue("idecole", idecole);
		}

		query.append("GROUP BY b.ideleve, b.nom, b.postnom, b.prenom, b.sexe,b.matricule, "
				+ "         a.idecole, e.idclasse, e.classe, " + "         c.idintermedaireclasse, d1.idannee, "
				+ "         f.idannee, f.annee, x.id, x.photo " + "ORDER BY montants;");

		try {
			return namedParameterJdbcTemplate.query(query.toString(), parameters,
					new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> searchPaiements(String userRole, String nom, Long idecole) {
		boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

		List<PaiementDto> collections = searchPaiements(nom, idecole, isAdmin);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé.");
		}
		return ResponseEntity.ok(collections);
	}

	public List<PaiementDto> PaiementDeleves(Long ideleve) {
		String query = " SELECT n.idpaiement,b.matricule, b.ideleve, b.nom, b.postnom, b.prenom, b.sexe, c.idclasse, "
				+ "    c.classe, n.montants AS montants, " + " n.datepaie, d.idannee, d.annee, t.idtranche, t.tranche, "
				+ "	ct.idcategorie,	ct.categorie, UPPER(n.frais) AS frais,"
				+ " COALESCE(NULLIF(x.photo, ''), 'http://localhost:8080/uploads/icon.jpg') AS photo, " + "    CASE  "
				+ "        WHEN n.idintermedaireclasse = b.idintermedaireclasse   "
				+ "             AND n.idintermedaireannee = b.idintermedaireannee  " + "        THEN 'CLASSE ACTUELLE' "
				+ "        ELSE 'CLASSE PRECEDENTE' " + "    END AS statut " + "FROM tab_Paiement n "
				+ "JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse ic ON n.idintermedaireclasse = ic.idclasse "
				+ "JOIN tab_Classe c ON ic.idclasse = c.idclasse "
				+ "JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Annee d ON ia.idannee = d.idannee " + "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "LEFT JOIN tab_Frais f ON f.idintermedaireclasse = ic.idintermedaireclasse AND f.idintermedaireannee = ia.idintermedaireannee "
				+ "LEFT JOIN tab_Tranche t ON t.idtranche = f.idtranche "
				+ "LEFT JOIN tab_Categoriefrais ct ON ct.idcategorie = f.idcategorie "
				+ " LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = c.idclasse AND h.idannee = d.idannee "
				+ "WHERE n.ideleve = :ideleve AND n.annule = false "
				+ "ORDER BY n.idintermedaireannee DESC, n.idpaiement DESC ";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> PaiementDeleve(Long ideleve) {
		List<PaiementDto> collections = PaiementDeleves(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> PaiementGroupeleves(Long ideleve) {
		String query = " SELECT b.ideleve,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    b.sexe, c.idclasse, UPPER(c.classe) AS classe, d.idannee, d.annee,"
				+ "SUM(n.montants) AS montants ," + "    COUNT(n.idpaiement) AS nombre_paiements, CASE  "
				+ "        WHEN n.idintermedaireclasse = b.idintermedaireclasse   "
				+ "             AND n.idintermedaireannee = b.idintermedaireannee  " + "        THEN 'CLASSE ACTUELLE' "
				+ "        ELSE 'CLASSE PRECEDENTE' " + "    END AS statut " + "FROM tab_Paiement n "
				+ "JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse ic ON n.idintermedaireclasse = ic.idclasse "
				+ "JOIN tab_Classe c ON ic.idclasse = c.idclasse "
				+ "JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Annee d ON ia.idannee = d.idannee "
				+ "LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = c.idclasse AND h.idannee = d.idannee "
				+ "WHERE n.ideleve = :ideleve AND n.annule = false "
				+ "GROUP BY b.ideleve,b.matricule, b.nom, b.postnom, b.prenom, b.sexe, "
				+ "c.idclasse, c.classe, d.idannee, d.annee, n.idintermedaireclasse, n.idintermedaireannee "
				+ "ORDER BY d.idannee DESC, c.idclasse, b.nom ";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> PaiementGroupeleve(Long ideleve) {
		List<PaiementDto> collections = PaiementGroupeleves(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> PaiementGroupDetails(Long ideleve, Long idintermedaireclasse, Long idintermedaireannee) {
		String query = "SELECT n.idpaiement,b.matricule, b.ideleve, b.nom, b.postnom, b.prenom, b.sexe, c.idclasse, "
				+ "c.classe, SUM(n.montants) AS montants, " + "n.datepaie, d.idannee, d.annee, t.idtranche, t.tranche, "
				+ "ct.idcategorie, ct.categorie, UPPER(n.frais) AS frais, "
				+ "COALESCE(NULLIF(x.photo, ''), 'http://localhost:8080/uploads/icon.jpg') AS photo, " + "CASE "
				+ "WHEN n.idintermedaireclasse = b.idintermedaireclasse AND n.idintermedaireannee = b.idintermedaireannee "
				+ "THEN 'CLASSE ACTUELLE' " + "ELSE 'CLASSE PRECEDENTE' " + "END AS statut " + "FROM tab_Paiement n "
				+ "JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse ic ON n.idintermedaireclasse = ic.idclasse "
				+ "JOIN tab_Classe c ON ic.idclasse = c.idclasse "
				+ "JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Annee d ON ia.idannee = d.idannee " + "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "LEFT JOIN tab_Frais f ON f.idintermedaireclasse = ic.idintermedaireclasse AND f.idintermedaireannee = ia.idintermedaireannee "
				+ "LEFT JOIN tab_Tranche t ON t.idtranche = f.idtranche "
				+ "LEFT JOIN tab_Categoriefrais ct ON ct.idcategorie = f.idcategorie "
				+ "LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = c.idclasse AND h.idannee = d.idannee "
				+ "WHERE n.ideleve = :ideleve AND n.idintermedaireclasse = :idintermedaireclasse "
				+ "AND n.idintermedaireannee = :idintermedaireannee AND n.annule = false "
				+ " GROUP BY  n.idpaiement,b.matricule,b.ideleve,b.nom,b.postnom,b.prenom,b.sexe,c.idclasse,c.classe, "
				+ "n.datepaie,d.idannee,d.annee,t.idtranche,t.tranche,ct.idcategorie,ct.categorie,n.frais,  "
				+ "  x.photo,n.annule " + "ORDER BY n.idintermedaireannee DESC, n.idpaiement DESC";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve)
				.addValue("idintermedaireclasse", idintermedaireclasse)
				.addValue("idintermedaireannee", idintermedaireannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> PaiementGroupDetail(Long ideleve, Long idintermedaireclasse, Long idintermedaireannee) {
		List<PaiementDto> collections = PaiementGroupDetails(ideleve, idintermedaireclasse, idintermedaireannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> CollectionPaiementModes(Long idpaiement) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, " + "CASE "
				+ "  WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M' " 
				+ "  WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' "
				+ "  ELSE 'NON DÉFINI' " + "END AS sexe, "
				+ "b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, "
				+ "n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "CASE WHEN n.annule = false THEN n.montants ELSE 0 END AS montants, "
				+ "n.datepaie, UPPER(n.frais) AS frais, "
				+ "CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos, x.id, "
				+ "m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ "UPPER(z.username) AS username, z.iduser, n.annule " + "FROM tab_Paiement n "
				+ "JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d2.idintermedaireannee = d1.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune " + "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "JOIN tab_User z ON z.iduser = n.iduser "
				+ "WHERE n.idpaiement = :idpaiement "
				+ "GROUP BY b.ideleve,b.matricule, a.idecole, x.id, e.idclasse, z.iduser, h.idcommune, m.idprovince, "
				+ "c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, "
				+ "l.idtranche, n.idpaiement, d2.idfrais " + "ORDER BY n.idpaiement LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idpaiement", idpaiement);
		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> CollectionPaiementMode(Long idpaiement) {
		List<PaiementDto> collections = CollectionPaiementModes(idpaiement);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> EcoleParClasses(Long idecole, Long idclasse, Long idannee) {

		String query = "WITH PaiementsDistincts AS ( "
				+ "   SELECT n.ideleve, SUM(CASE WHEN n.annule = false THEN n.montants ELSE 0 END) AS montants "
				+ "   FROM tab_Paiement n " + "   JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "   JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "   JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "   WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee "
				+ "   GROUP BY n.ideleve " + "), " + "MontantsFrais AS ( "
				+ "   SELECT d2.idintermedaireclasse, SUM(d2.montant) AS montant " + "   FROM tab_Frais d2 "
				+ "   JOIN tab_Intermedaireclasse c ON d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "   JOIN tab_Intermedaireannee ia ON d2.idintermedaireannee = ia.idintermedaireannee "
				+ "   WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee "
				+ "   GROUP BY d2.idintermedaireclasse " + ") " + "SELECT " + "   UPPER(b.nom) AS nom, "
				+ "   UPPER(b.postnom) AS postnom,b.matricule, " + "   UPPER(b.prenom) AS prenom, " + "   b.ideleve, "
				+ "   UPPER(b.sexe) AS sexe, " + "   UPPER(a.ecole) AS ecole, " + "   e.idclasse, "
				+ "   UPPER(e.classe) AS classe, " + "   an.annee AS annee, "
				+ "   COALESCE(pd.montants, 0) AS montants, " + "   COALESCE(mf.montant, 0) AS montant, "
				+ "   (COALESCE(pd.montants, 0) - COALESCE(mf.montant, 0)) AS reste, "
				+ " CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Intermedaireannee ia ON b.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Annee an ON ia.idannee = an.idannee "
				+ "LEFT JOIN PaiementsDistincts pd ON b.ideleve = pd.ideleve "
				+ "LEFT JOIN MontantsFrais mf ON c.idintermedaireclasse = mf.idintermedaireclasse "
				+ "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee "
				+ "ORDER BY b.nom, b.postnom, b.prenom;";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> EcoleParClasse(Long idecole, Long idclasse, Long idannee) {
		List<PaiementDto> collections = EcoleParClasses(idecole, idclasse, idannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}
	
	
	public List<PaiementDto> CollecteAnnulations(Long idecole, Long idclasse, Long idannee) {
	    String query = "SELECT DISTINCT ON (n.idpaiement) "
	        + "UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom,"
	        + " CASE WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M'  WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' "
	        + "	 ELSE 'NON DÉFINI' END AS sexe, "
	        + "UPPER(b.adresse) AS adresse, b.ideleve, "
	        + "a.idecole, UPPER(a.ecole) AS ecole, "
	        + "e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
	        + "k.idcategorie, UPPER(f.annee) AS annee, "
	        + "CASE WHEN n.annule = true THEN n.montants ELSE 0 END AS montant_paiement, "
	        + "z.username, z.iduser, "
	        + "n.datepaieannuler, UPPER(n.frais) AS frais "
	        + "FROM tab_Eleve b "
	        + "JOIN tab_Paiement n ON n.ideleve = b.ideleve "
	        + "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
	        + "JOIN tab_Ecole a ON c.idecole = a.idecole "
	        + "JOIN tab_Classe e ON c.idclasse = e.idclasse "
	        + "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
	        + "JOIN tab_Annee f ON d1.idannee = f.idannee "
	        + "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
	        + "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
	        + "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
	        + "JOIN tab_User z ON z.iduser = n.idusermodi "
	        + "WHERE a.idecole = :idecole AND e.idclasse = :idclasse AND f.idannee = :idannee "
	        + "AND n.annule = true "
	        + "ORDER BY n.idpaiement, b.nom ASC";

	    MapSqlParameterSource parameters = new MapSqlParameterSource()
	        .addValue("idecole", idecole)
	        .addValue("idclasse", idclasse)
	        .addValue("idannee", idannee);

	    try {
	        return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.emptyList();
	    }
	}

	public ResponseEntity<?> CollecteAnnulation(Long idecole, Long idclasse, Long idannee) {
		List<PaiementDto> collections = CollecteAnnulations(idecole, idclasse, idannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}
	
	public List<PaiementDto> FicheAnnulers(long idecole, long idclasse, long idannee) {
		String query = "SELECT DISTINCT ON (n.idpaiement) "
			    + "UPPER(b.nom) ||' '|| UPPER(b.postnom) ||' '|| UPPER(b.prenom) AS noms, "
			    + " CASE WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M'  WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' "
			    + "	ELSE 'NON DÉFINI' END AS sexe, b.matricule, "
			    + "UPPER(b.adresse) AS adresse, b.ideleve, "
			    + "a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
			    + "e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
			    + "c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
			    + "k.idcategorie, UPPER(k.categorie) AS categorie, "
			    + "UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
			    + "CASE WHEN n.annule = true THEN n.montants ELSE 0 END AS montant_paiement, "
			    + "n.datepaie, UPPER(n.frais) AS frais, z.username, z.iduser, "
			    + "UPPER(v.province) AS province, v.idprovince, UPPER(m.commune) AS commune, m.idcommune, "
			    + "CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
			    + "FROM tab_Eleve b "
			    + "JOIN tab_Paiement n ON n.ideleve = b.ideleve "
			    + "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
			    + "JOIN tab_Ecole a ON c.idecole = a.idecole "
			    + "JOIN tab_Classe e ON c.idclasse = e.idclasse "
			    + "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
			    + "JOIN tab_Annee f ON d1.idannee = f.idannee "
			    + "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
			    + "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
			    + "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
			    + "JOIN tab_Province v ON v.idprovince = b.idprovince "
			    + "JOIN tab_Commune m ON m.idcommune = a.idcommune "
			    + "JOIN tab_User z ON z.iduser = n.idusermodi "
			    + "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
			    + "WHERE a.idecole = :idecole AND e.idclasse = :idclasse AND f.idannee = :idannee AND n.annule = true "
			    + "ORDER BY n.idpaiement DESC";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	
	public ResponseEntity<?> FicheAnnuler(Long idecole, Long idclasse, Long idannee)
			throws FileNotFoundException, JRException {
		try {
			List<PaiementDto> collections = FicheAnnulers(idecole, idclasse, idannee);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager
					.compileReport(ResourceUtils.getFile("classpath:etats/FicheOperationAnnuler.jrxml").getAbsolutePath()), parameters);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}


	public List<PaiementDto> FichePaiementeleves(long ideleve, long idclasse, long idannee) {
		String query = " SELECT DISTINCT ON (n.idpaiement) "
				+ " UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom,b.matricule, UPPER(b.prenom) AS prenom, UPPER(b.sexe) AS sexe , UPPER(b.adresse) AS adresse, b.ideleve, "
				+ " a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
				+ " c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ " UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, CASE WHEN n.annule = false THEN n.montants ELSE 0 END AS montant_paiement, "
				+ " n.datepaie, UPPER(n.frais) AS frais, "
				+ " UPPER(v.province) AS province, v.idprovince, UPPER(m.commune) AS commune, m.idcommune,x.id, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
				+ " FROM tab_Eleve b " + " JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ " JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ " JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ " JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ " JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Province v ON v.idprovince = b.idprovince "
				+ " JOIN tab_Commune m ON m.idcommune = a.idcommune "
				+ "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ " WHERE b.ideleve = :ideleve AND e.idclasse = :idclasse AND f.idannee = :idannee And n.annule = false"
				+ " ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	@Override
	public ResponseEntity<?> FichePaiementeleve(Long ideleve, Long idclasse, Long idannee)
			throws FileNotFoundException, JRException {
		try {
			List<PaiementDto> collections = FichePaiementeleves(ideleve, idclasse, idannee);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager
					.compileReport(ResourceUtils.getFile("classpath:etats/paie.jrxml").getAbsolutePath()), parameters);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> FicheRecouvrementClasses(long idecole, long idclasse, long idannee) {
		String query = "WITH PaiementsDistincts AS ( "
				+ "    SELECT n.ideleve, SUM(CASE WHEN n.annule = false THEN n.montants ELSE 0 END) AS montants "
				+ "    FROM tab_Paiement n " + "    JOIN tab_Eleve b ON n.ideleve = b.ideleve "
				+ "    JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "    JOIN tab_Intermedaireannee ia ON n.idintermedaireannee = ia.idannee "
				+ "    WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee "
				+ "    GROUP BY n.ideleve " + "), " + "MontantsFrais AS ( "
				+ "    SELECT d2.idintermedaireclasse, SUM(d2.montant) AS montant " + "    FROM tab_Frais d2 "
				+ "    JOIN tab_Intermedaireclasse c ON d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "    JOIN tab_Intermedaireannee ia ON d2.idintermedaireannee = ia.idintermedaireannee "
				+ "    WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee "
				+ "    GROUP BY d2.idintermedaireclasse " + ") " + "SELECT "
				+ "    UPPER(b.nom || ' ' || b.postnom || ' ' || b.prenom) AS noms, " + "    UPPER(b.sexe) AS sexe, "
				+ "    UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue,b.matricule, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, UPPER(an.annee) AS annee_scolaire, "
				+ "    UPPER(h.commune) AS commune, UPPER(g.province) AS province, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos, "
				+ "    COALESCE(pd.montants, 0) AS montant_paiement, "
				+ "    COALESCE(mf.montant, 0) AS montant_attendu, "
				+ "    (COALESCE(pd.montants, 0) - COALESCE(mf.montant, 0)) AS montant_frais " + "FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Intermedaireannee ia ON b.idintermedaireannee = ia.idannee "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Annee an ON ia.idannee = an.idannee "
				+ "LEFT JOIN PaiementsDistincts pd ON b.ideleve = pd.ideleve "
				+ "LEFT JOIN MontantsFrais mf ON c.idintermedaireclasse = mf.idintermedaireclasse "
				+ "LEFT JOIN tab_Province g ON a.idprovince = g.idprovince "
				+ "LEFT JOIN tab_Commune h ON a.idcommune = h.idcommune "
				+ "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "WHERE c.idecole = :idecole AND c.idclasse = :idclasse AND ia.idannee = :idannee " + "ORDER BY noms";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@Override
	public ResponseEntity<?> FicheRecouvrementClasse(long idecole, long idclasse, long idannee)
			throws FileNotFoundException, JRException {
		try {
			List<PaiementDto> collections = FicheRecouvrementClasses(idecole, idclasse, idannee);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			JasperPrint reportlist = JasperFillManager.fillReport(
					JasperCompileManager.compileReport(
							ResourceUtils.getFile("classpath:etats/FicheRecouvrement.jrxml").getAbsolutePath()),
					parameters);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> CollectionPaiementse(long idecole) {
		String query = "SELECT UPPER(cl.classe) AS classe, UPPER(an.annee) AS annee,"
				+ "SUM(CASE WHEN p.annule = false THEN p.montants ELSE 0 END) AS montants " + " FROM tab_Paiement p "
				+ " JOIN tab_Eleve el ON p.ideleve = el.ideleve "
				+ " JOIN tab_Intermedaireclasse ic ON el.idintermedaireclasse = ic.idintermedaireclasse "
				+ " JOIN tab_Classe cl ON ic.idclasse = cl.idclasse "
				+ " JOIN tab_Intermedaireannee ia ON el.idintermedaireannee = ia.idintermedaireannee "
				+ " JOIN tab_Annee an ON ia.idannee = an.idannee " + " JOIN tab_Ecole e ON e.idecole = ia.idecole "
				+ "	WHERE e.idecole = :idecole  " + " GROUP BY cl.classe, an.annee " + " ORDER BY an.annee DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollectionPaiementses(long idecole) {
		List<PaiementDto> collections = CollectionPaiementse(idecole);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> CollectionPaiementdashbords(long idecole, long idclasse, long idannee) {
		String query = "SELECT UPPER(cl.classe) AS classe, UPPER(an.annee) AS annee," + "SUM(p.montants) AS montants "
				+ "	FROM tab_Paiement p " + "	JOIN tab_Eleve el ON p.ideleve = el.ideleve "
				+ "	JOIN tab_Intermedaireclasse ic ON el.idintermedaireclasse = ic.idintermedaireclasse "
				+ "	JOIN tab_Ecole a ON ic.idecole = a.idecole "
				+ "	JOIN tab_Classe cl ON ic.idclasse = cl.idclasse "
				+ "	JOIN tab_Intermedaireannee ia ON el.idintermedaireannee = ia.idintermedaireannee "
				+ "	JOIN tab_Annee an ON ia.idannee = an.idannee "
				+ "	WHERE a.idecole = :idecole AND ic.idclasse = :idclasse  "
				+ "AND an.idannee = :idannee AND p.annule = false" + "	GROUP BY cl.classe, an.annee ";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollectionPaiementdashbord(long idecole, long idclasse, long idannee) {
		List<PaiementDto> collections = CollectionPaiementdashbords(idecole, idclasse, idannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> PaiementActuelledashbords() {
		String query = "SELECT  SUM(CASE WHEN b.annule = false THEN b.montants ELSE 0 END) AS montant_paiement "
				+ "FROM tab_Paiement b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse "
				+ "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee " 
				+ "LEFT JOIN Tab_Historique_classe_eleve hi "
			    + " ON hi.ideleve = b.ideleve "
	            + "  AND hi.idclasse = e.idclasse " 
	            + "  AND hi.idannee = f.idannee "
	            + " AND b.annule = true"
	            + " WHERE f.cloturee = FALSE " 
	            + " GROUP BY f.annee "
	            + " ORDER BY f.annee DESC";

		return namedParameterJdbcTemplate.query(query, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> PaiementActuelledashbord() {
		List<PaiementDto> collections = PaiementActuelledashbords();

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<PaiementDto> SectionPaiementActuelledashbords() {
    String query = """
        SELECT  
            CASE 
                WHEN UPPER(e.classe) LIKE '%MATERNELLE%' THEN 'MATERNELLE'
                WHEN UPPER(e.classe) LIKE '%PRIMAIRE%' THEN 'PRIMAIRE'
                WHEN UPPER(e.classe) LIKE '%SECONDAIRE%' THEN 'SECONDAIRE'
                WHEN UPPER(e.classe) LIKE '%ELECTRICITE%' THEN 'ELECTRICITE'
                WHEN UPPER(e.classe) LIKE '%MECANIQUE%' THEN 'MECANIQUE'
                WHEN UPPER(e.classe) LIKE '%COUPE-COUTURE%' THEN 'COUPE-COUTURE'
                WHEN UPPER(e.classe) LIKE '%SCIENTIFIQUE%' THEN 'SCIENTIFIQUE'
                ELSE 'AUTRES'
            END AS type_classe,
            
            SUM(b.montants) AS montant_paiement,
            
             ROUND(
                (SUM(b.montants) * 100.0 / SUM(SUM(b.montants)) OVER ())::numeric,
                2
            ) AS pourcentage

        FROM tab_Paiement b
        JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse
        JOIN tab_Classe e ON c.idclasse = e.idclasse
        JOIN tab_Ecole a ON c.idecole = a.idecole
        JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee
        JOIN tab_Annee f ON d.idannee = f.idannee
        LEFT JOIN Tab_Historique_classe_eleve hi 
            ON hi.ideleve = b.ideleve 
            AND hi.idclasse = e.idclasse 
            AND hi.idannee = f.idannee
        AND b.annule = true 
		 WHERE f.cloturee = FALSE  
	AND b.annule = FALSE
        GROUP BY type_classe
        ORDER BY type_classe
        """;

    return namedParameterJdbcTemplate.query(query, new BeanPropertyRowMapper<>(PaiementDto.class));
}


	public ResponseEntity<?> SectionPaiementActuelledashbord() {
		List<PaiementDto> collections = SectionPaiementActuelledashbords();

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	private List<PaiementDto> getPaiementsByEleves(Long ideleve) {
		String query = "WITH Paiements AS ( "
				+ "    SELECT n.ideleve,b.matricule, n.idintermedaireclasse, SUM(CASE WHEN n.annule = false THEN n.montants ELSE 0 END) AS montant_paiement "
				+ "    FROM tab_Paiement n " + "    GROUP BY n.ideleve, n.idintermedaireclasse " + "), " + "Frais AS ( "
				+ "    SELECT d2.idintermedaireclasse, SUM(d2.montant) AS montant_frais " + "    FROM tab_Frais d2 "
				+ "    GROUP BY d2.idintermedaireclasse " + ") " + "SELECT " + "    b.ideleve, "
				+ "    UPPER(b.nom) AS nom, " + "    UPPER(b.postnom) AS postnom, " + "    UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe, " + "    UPPER(a.ecole) AS ecole, " + "    e.idclasse, "
				+ "    UPPER(e.classe) AS classe, " + "    COALESCE(pd.montant_paiement,0) AS montant_paiement, "
				+ "    COALESCE(mf.montant_frais,0) AS montant_frais, " + "    'CLASSE ACTUELLE' AS statut "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "LEFT JOIN Paiements pd ON b.ideleve = pd.ideleve AND c.idclasse = pd.idintermedaireclasse "
				+ "LEFT JOIN Frais mf ON c.idintermedaireclasse = mf.idintermedaireclasse "
				+ "WHERE b.ideleve = :ideleve " + "UNION ALL " + "SELECT " + "    b.ideleve, "
				+ "    UPPER(b.nom) AS nom, " + "    UPPER(b.postnom) AS postnom, " + "    UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe, " + "    UPPER(a.ecole) AS ecole, " + "    cl.idclasse, "
				+ "    UPPER(cl.classe) AS classe, " + "    COALESCE(pd.montant_paiement,0) AS montant_paiement, "
				+ "    COALESCE(mf.montant_frais,0) AS montant_frais, " + "    'CLASSE PRECEDENTE' AS statut "
				+ "FROM tab_Eleve b " + "JOIN Tab_Historique_classe_eleve hi ON hi.ideleve = b.ideleve "
				+ "JOIN tab_Classe cl ON hi.idclasse = cl.idclasse "
				+ "JOIN tab_Intermedaireclasse ic ON ic.idclasse = cl.idclasse "
				+ "JOIN tab_Ecole a ON ic.idecole = a.idecole "
				+ "LEFT JOIN Paiements pd ON b.ideleve = pd.ideleve AND ic.idclasse = pd.idintermedaireclasse "
				+ "LEFT JOIN Frais mf ON ic.idintermedaireclasse = mf.idintermedaireclasse "
				+ "WHERE b.ideleve = :ideleve " + "ORDER BY nom, postnom, prenom";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> getPaiementsByEleve(Long ideleve) {
		List<PaiementDto> collections = getPaiementsByEleves(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé.");
		}

		List<PaiementDto> precedentes = collections.stream().filter(p -> "CLASSE PRECEDENTE".equals(p.getStatut()))
				.filter(p -> p.getDette() > 0) // uniquement les dettes
				.toList();

		PaiementDto actuelle = collections.stream().filter(p -> "CLASSE ACTUELLE".equals(p.getStatut())).findFirst()
				.orElse(null);

		Map<String, Object> response = new HashMap<>();

		if (!precedentes.isEmpty()) {
			response.put("dettesPrecedentes", precedentes);
			response.put("message", "L'élève doit d'abord payer les dettes des classes précédentes.");
		}

		if (actuelle != null) {
			response.put("classeActuelle", actuelle);
		}

		return ResponseEntity.ok(response);
	}

	public List<PaiementDto> CollectionPaiementAcomptes(Long ideleve) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ " a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, "
				+ " c.idintermedaireclasse, d1.idintermedaireannee, f.idannee,  UPPER(n.categorie) AS categorie, "
				+ " UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.datepaie, UPPER(n.frais) as frais, "
				+ " m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ "CASE WHEN n.annule = false THEN n.montants ELSE 0 END AS montants, "
				+ " (SELECT SUM(d2_sub.montant) " + " FROM tab_Frais d2_sub "
				+ " WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse "
				+ " AND d2_sub.idintermedaireannee = d1.idintermedaireannee) AS montant_frais, "
				+ " (SELECT SUM(n_sub.montants) " + " FROM tab_Paiement n_sub "
				+ " WHERE n_sub.ideleve = b.ideleve) AS montant_paiement" + " FROM tab_Eleve b "
				+ " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idintermedaireannee "
				+ " JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ " JOIN tab_Frais d2 ON b.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ " JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ " JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune " + " JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ " WHERE b.ideleve = :ideleve "
				+ " GROUP BY b.ideleve,b.matricule, a.idecole, e.idclasse, h.idcommune, m.idprovince, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais "
				+ " ORDER BY n.idpaiement DESC LIMIT 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollectionPaiementAcompte(Long ideleve) {
		List<PaiementDto> collections = CollectionPaiementAcomptes(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> CollectionPaiementSoldes(Long ideleve) {
		String query = "SELECT UPPER(b.nom) AS nom,b.matricule, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, b.ideleve, "
				+ "    a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, e.idclasse, UPPER(e.classe) AS classe, "
				+ "    n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, UPPER(n.categorie) AS categorie, "
				+ "    UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, n.datepaie, UPPER(n.frais) AS frais, "
				+ "    m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ "    (SELECT SUM(d2_sub.montant) " + "     FROM tab_Frais d2_sub "
				+ "     WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) AS montant_frais, "
				+ "    (SELECT SUM(CASE WHEN n_sub.annule = false THEN n_sub.montants ELSE 0 END) "
				+ "     FROM tab_Paiement n_sub " + "     WHERE n_sub.ideleve = b.ideleve) AS montant_paiement, "
				+ "    CASE " + "        WHEN " + "            (SELECT SUM(d2_sub.montant) "
				+ "             FROM tab_Frais d2_sub "
				+ "             WHERE d2_sub.idintermedaireclasse = c.idintermedaireclasse AND d2_sub.idintermedaireannee = d1.idintermedaireannee) = "
				+ "            (SELECT SUM(CASE WHEN n_sub.annule = false THEN n_sub.montants ELSE 0 END) "
				+ "             FROM tab_Paiement n_sub " + "             WHERE n_sub.ideleve = b.ideleve) "
				+ "        THEN "
				+ "            (SELECT SUM(CASE WHEN n_sub.annule = false THEN n_sub.montants ELSE 0 END)  "
				+ "             FROM tab_Paiement n_sub " + "             WHERE n_sub.ideleve = b.ideleve) "
				+ "        ELSE 0 " + "    END AS montants " +

				"FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON b.idintermedaireannee = d1.idintermedaireannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON b.idintermedaireannee = d2.idintermedaireannee AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ "WHERE b.ideleve = :ideleve "
				+ "GROUP BY b.ideleve, a.idecole, e.idclasse, h.idcommune, m.idprovince, "
				+ "    c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, l.idtranche, n.idpaiement, d2.idfrais " + "ORDER BY n.idpaiement DESC LIMIT 1";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> CollectionPaiementSolde(Long ideleve) {
		List<PaiementDto> collections = CollectionPaiementSoldes(ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public void logUserPayment(String username, String noms, String classe, String ecole, String annee, double montant,
			String frais) {
		logger.info(
				"Paiement effectué | Élève: {} | Classe: {} | École: {} | Année: {} | Montant: {} | Utilisateur: {}",
				noms, classe, ecole, annee, montant, username, frais);
	}

	public void enregistrerPaiement(String username, String noms, String classe, String ecole, String annee,
			double montant, String frais) {
		logUserPayment(username, noms, classe, ecole, annee, montant, frais);
		System.out.println("✅ Paiement enregistré dans les logs !");
	}

	public List<PaiementDto> RapportjournalierCaisses(Long iduser) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe,b.matricule, UPPER(b.adresse) AS adresse, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, UPPER(k.categorie) AS categorie, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "    n.montants AS montant_paiement," + " n.datepaie, UPPER(n.frais) AS frais, u.username "
				+ "FROM tab_Eleve b " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " + "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " + "WHERE u.iduser = :iduser AND n.annule = false"
				+ "  AND DATE(n.datepaie) = CURRENT_DATE " + "  AND f.idannee = ( " + "        SELECT idannee "
				+ "        FROM tab_Annee " + "        ORDER BY idannee DESC " + "        LIMIT 1 ) "
				+ "ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("iduser", iduser);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> RapportjournalierCaisse(Long iduser) {
		List<PaiementDto> collections = RapportjournalierCaisses(iduser);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> ImpressionRapportjournalierCaisses(Long iduser) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ " UPPER(b.nom || ' ' || b.postnom || ' ' || b.prenom) AS noms, CASE "
				+ "        WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M' "
				+ "        WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' " + "        ELSE UPPER(b.sexe) "
				+ "    END AS sexe, UPPER(b.adresse) AS adresse,b.matricule, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, UPPER(k.categorie) AS categorie, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "    n.montants AS montant_paiement, n.datepaie, UPPER(n.frais) AS frais, u.username, "
				+ "    m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
				+ "FROM tab_Eleve b " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " + "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " + "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "WHERE u.iduser = :iduser" + "  AND DATE(n.datepaie) = CURRENT_DATE " + "  AND f.idannee = ( "
				+ "        SELECT idannee " + "        FROM tab_Annee " + "        ORDER BY idannee DESC "
				+ "        LIMIT 1 ) " + "ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("iduser", iduser);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	@Override
	public ResponseEntity<?> ImpressionRapportjournalierCaisse(Long iduser) throws FileNotFoundException, JRException {
		try {
			List<PaiementDto> collections = ImpressionRapportjournalierCaisses(iduser);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			JasperPrint reportlist = JasperFillManager.fillReport(
					JasperCompileManager.compileReport(
							ResourceUtils.getFile("classpath:etats/FicheRapportjournalier.jrxml").getAbsolutePath()),
					parameters);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> ImpressionRapportjournalierCaissesId(Long iduser) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ " UPPER(b.nom || ' ' || b.postnom || ' ' || b.prenom) AS noms, CASE "
				+ "        WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M' "
				+ "        WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' " + "        ELSE UPPER(b.sexe) "
				+ "    END AS sexe,b.matricule, UPPER(b.adresse) AS adresse, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, UPPER(k.categorie) AS categorie, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "    n.montants AS montant_paiement, n.datepaie, UPPER(n.frais) AS frais, u.username, "
				+ "    m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
				+ "FROM tab_Eleve b " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " + "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " + "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "WHERE u.iduser = :iduser" + "  AND DATE(n.datepaie) = CURRENT_DATE " + "  AND f.idannee = ( "
				+ "        SELECT idannee " + "        FROM tab_Annee " + "        ORDER BY idannee DESC "
				+ "        LIMIT 1 ) " + "ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("iduser", iduser);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	@Override
	public ResponseEntity<?> ImpressionRapportjournalierDesCaisseId(Long iduser)
			throws FileNotFoundException, JRException {
		try {
			List<PaiementDto> collections = ImpressionRapportjournalierCaissesId(iduser);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			JasperPrint reportlist = JasperFillManager.fillReport(
					JasperCompileManager.compileReport(
							ResourceUtils.getFile("classpath:etats/FicheRapportjournalier.jrxml").getAbsolutePath()),
					parameters);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
			return ResponseEntity.ok(new reportBase64(encodedString));
		} catch (FileNotFoundException e) {
			return ResponseEntity.ok().body(e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.ok().body(e.getMessage());
		}
	}

	public List<PaiementDto> RapportjournalierDesCaisses(String role) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe, UPPER(b.adresse) AS adresse, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, UPPER(k.categorie) AS categorie, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "    n.montants  AS montant_paiement,b.matricule, " + " n.datepaie, UPPER(n.frais) AS frais, u.username "
				+ "FROM tab_Eleve b " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " + "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " + "WHERE r.role = :role AND n.annule = false "
				+ "  AND DATE(n.datepaie) = CURRENT_DATE " + "  AND f.idannee = ( " + "        SELECT idannee "
				+ "        FROM tab_Annee " + "        ORDER BY idannee DESC " + "        LIMIT 1 ) "
				+ "ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("role", role);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> RapportjournalierDesCaisse(String role) {
		List<PaiementDto> collections = RapportjournalierDesCaisses(role);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public List<PaiementDto> RapportjournalierCaissesId(Long iduser) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe, UPPER(b.adresse) AS adresse, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
				+ "    k.idcategorie, UPPER(k.categorie) AS categorie,b.matricule, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "    n.montants  AS montant_paiement, " + " n.datepaie, UPPER(n.frais) AS frais, u.username,u.iduser "
				+ "FROM tab_Eleve b " + "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " + "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " + "WHERE u.iduser = :iduser And n.annule = false "
				+ "  AND DATE(n.datepaie) = CURRENT_DATE " + "  AND f.idannee = ( " + "        SELECT idannee "
				+ "        FROM tab_Annee " + "        ORDER BY idannee DESC " + "        LIMIT 1 ) "
				+ "ORDER BY n.idpaiement DESC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("iduser", iduser);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> RapportjournalierCaisseId(Long iduser) {
		List<PaiementDto> collections = RapportjournalierCaissesId(iduser);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}

	public ResponseEntity<?> annulerPaiement(Long idpaiement) {
		Paiement paiement = paiementRepository.findById(idpaiement)
				.orElseThrow(() -> new RuntimeException("Paiement introuvable"));

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		Users user = usersRepository.findByUsername(username)
				.orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));

		if (paiement.getAnnule().FALSE) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Ce paiement a déjà été annulé.");
		}

		paiement.setAnnule(true);
		paiement.setMotifAnnulation(" Annulation du reçu N° " + paiement.getIdpaiement() + " Par utilisateur "
				+ user.getNom() + " " + user.getPostnom() + " " + user.getUsername());
		paiement.setDatepaieannuler(LocalDateTime.now());
		paiement.setIdusermodi(user.getIduser());

		paiementRepository.save(paiement);

		return ResponseEntity.ok("Paiement annulé avec succès.");
	}

	public List<PaiementDto> InventaireCaissesIds(Long iduser, Date dateDebut, Date dateFin) {
		String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom,b.matricule, "
				+ "UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "UPPER(b.sexe) AS sexe, UPPER(b.adresse) AS adresse, b.ideleve, "
				+ "a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
				+ "e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, "
				+ "d1.idintermedaireannee, f.idannee, k.idcategorie, UPPER(k.categorie) AS categorie, "
				+ "UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
				+ "n.montants AS montant_paiement, n.datepaie, UPPER(n.frais) AS frais, "
				+ "u.username, u.iduser "
				+ "FROM tab_Eleve b " 
				+ "JOIN tab_Paiement n ON n.ideleve = b.ideleve "
				+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " 
				+ "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
				+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
				+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
				+ "AND d2.idintermedaireclasse = c.idintermedaireclasse "
				+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
				+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " 
				+ "JOIN tab_User u ON n.iduser = u.iduser "
				+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser "
				+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole " 
				+ "WHERE u.iduser = :iduser AND n.annule = false "
				+ "AND DATE(n.datepaie) BETWEEN :dateDebut AND :dateFin "
				+ "ORDER BY n.idpaiement DESC";

		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("iduser", iduser)
				.addValue("dateDebut", dateDebut)
				.addValue("dateFin", dateFin);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}

	public ResponseEntity<?> InventaireCaissesId(Long iduser, Date dateDebut, Date dateFin) {
		List<PaiementDto> collections = InventaireCaissesIds(iduser, dateDebut, dateFin);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun paiement trouvé pour cet élève.");
		} else {
			return ResponseEntity.ok(collections);
		}

	}



public List<PaiementDto> ImpressionInventaireCaissesIds(Long iduser, Date dateDebut, Date dateFin) {
	String query = "SELECT DISTINCT ON (n.idpaiement) UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
			+ " UPPER(b.nom || ' ' || b.postnom || ' ' || b.prenom) AS noms, CASE "
			+ "        WHEN UPPER(b.sexe) = 'MASCULIN' THEN 'M' "
			+ "        WHEN UPPER(b.sexe) = 'FEMININ' THEN 'F' " 
			+ "        ELSE UPPER(b.sexe) "
			+ "    END AS sexe,b.matricule, UPPER(b.adresse) AS adresse, b.ideleve, a.idecole, UPPER(a.ecole) AS ecole, UPPER(a.avenue) AS avenue, "
			+ "    e.idclasse, UPPER(e.classe) AS classe, n.idpaiement, c.idintermedaireclasse, d1.idintermedaireannee, f.idannee, "
			+ "    k.idcategorie, UPPER(k.categorie) AS categorie, UPPER(f.annee) AS annee, l.idtranche, UPPER(l.tranche) AS tranche, "
			+ "    n.montants AS montant_paiement, n.datepaie, UPPER(n.frais) AS frais, u.username, "
			+ "    m.idprovince, UPPER(m.province) AS province, h.idcommune, UPPER(h.commune) AS commune, "
			+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
			+ "FROM tab_Eleve b " 
			+ "JOIN tab_Paiement n ON n.ideleve = b.ideleve  "
			+ "JOIN tab_Intermedaireclasse c ON n.idintermedaireclasse = c.idclasse "
			+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " 
			+ "JOIN tab_Ecole a ON c.idecole = a.idecole "
			+ "JOIN tab_Intermedaireannee d1 ON n.idintermedaireannee = d1.idannee "
			+ "JOIN tab_Annee f ON d1.idannee = f.idannee "
			+ "JOIN tab_Frais d2 ON d1.idintermedaireannee = d2.idintermedaireannee "
			+ "                  AND d2.idintermedaireclasse = c.idintermedaireclasse "
			+ "JOIN tab_Categoriefrais k ON k.idcategorie = d2.idcategorie "
			+ "JOIN tab_Province m ON m.idprovince = b.idprovince "
			+ "JOIN tab_Commune h ON h.idcommune = a.idcommune "
			+ "JOIN tab_Tranche l ON l.idtranche = d2.idtranche " 
			+ "JOIN tab_User u ON n.iduser = u.iduser "
			+ "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser  "
			+ "JOIN tab_Role r ON r.idrole = ur.roles_idrole  " 
			+ "LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
			+ "WHERE u.iduser = :iduser AND n.annule = false "
			+ "AND DATE(n.datepaie) BETWEEN :dateDebut AND :dateFin "
			+ "ORDER BY n.idpaiement DESC";
	MapSqlParameterSource parameters = new MapSqlParameterSource()
			.addValue("iduser", iduser)
			.addValue("dateDebut", dateDebut)
			.addValue("dateFin", dateFin);
	return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
}

@Override
public ResponseEntity<?> ImpressionInventaireCaissesId(Long iduser, Date dateDebut, Date dateFin)
		throws FileNotFoundException, JRException {
	try {
		List<PaiementDto> collections = ImpressionInventaireCaissesIds(iduser, dateDebut, dateFin);
		JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("REPORT_DATA_SOURCE", ds);
		parameters.put("dateDebut", dateDebut);
        parameters.put("dateFin", dateFin);

		JasperPrint reportlist = JasperFillManager.fillReport(
				JasperCompileManager.compileReport(
						ResourceUtils.getFile("classpath:etats/FicheInventaire.jrxml").getAbsolutePath()),
				parameters);

		String encodedString = Base64.getEncoder()
				.encodeToString(JasperExportManager.exportReportToPdf(reportlist));
		return ResponseEntity.ok(new reportBase64(encodedString));
	} catch (FileNotFoundException e) {
		return ResponseEntity.ok().body(e.getMessage());
	} catch (JRException e) {
		return ResponseEntity.ok().body(e.getMessage());
	}
}

}
