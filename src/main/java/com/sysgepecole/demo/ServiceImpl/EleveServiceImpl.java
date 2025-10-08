package com.sysgepecole.demo.ServiceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
/*import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;*/
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.sysgepecole.demo.Dto.EleveModelDto;

import com.sysgepecole.demo.Models.Classe;
import com.sysgepecole.demo.Models.Ecole;
import com.sysgepecole.demo.Models.Eleve;
import com.sysgepecole.demo.Models.reportBase64;
import com.sysgepecole.demo.Repository.ClasseRepository;
import com.sysgepecole.demo.Repository.EcoleRepository;
import com.sysgepecole.demo.Repository.EleveRepository;

import com.sysgepecole.demo.Service.EleveService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class EleveServiceImpl implements EleveService {

	private static final Logger logger = LoggerFactory.getLogger(EleveServiceImpl.class);

	@Autowired
	private EleveRepository eleverepository;
	
	@Autowired
	private EcoleRepository ecolerepository;
	
	@Autowired
	private ClasseRepository classerepository;

	/*
	 * @Autowired private JavaMailSender mailSender;
	 */

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public Optional<Eleve> findEleveByNomPostnomPrenom(String nom, String postnom, String prenom,
			Long idintermedaireclasse, Long idintermedaireannee) {
		return eleverepository.findByNomAndPostnomAndPrenomAndIdintermedaireclasseAndIdintermedaireannee(nom, postnom,
				prenom, idintermedaireclasse, idintermedaireannee);
	}
	

	@Override
public Eleve createEleve(Eleve eleve) {
    Optional<Eleve> eleves = findEleveByNomPostnomPrenom(
            eleve.getNom(), 
            eleve.getPostnom(), 
            eleve.getPrenom(),
            eleve.getIdintermedaireclasse(), 
            eleve.getIdintermedaireannee()
    );
    
    

    if (eleves.isPresent()) {
        throw new IllegalStateException("L'élève existe déjà dans cette classe et cette année.");
    }

    
    
    Classe classe = classerepository.findByIdclasse(eleve.getIdintermedaireclasse())
            .orElseThrow(() -> new IllegalStateException(
                "Classe non trouvée pour l'élève ID : " + eleve.getIdeleve()
            ));

    
    Ecole ecole = ecolerepository.findByIdecole(eleve.getIdecole())
            .orElseThrow(() -> new IllegalStateException(
                "Ecole non trouvée pour l'élève ID : " + eleve.getIdeleve()
            ));
  
    
    String annee = String.valueOf(LocalDate.now().getYear()).substring(2);
    String ecoleCode = genererCodeEcole(ecole.getEcole());
    String classeCode = genererCodeClasse(classe.getClasse());
    Long dernierId = eleverepository.findMaxIdeleve().orElse(0L);
    String numero = String.format("%04d", dernierId + 1);

    String matricule = String.format("%s%s%s%s", annee, ecoleCode, classeCode, numero);
    eleve.setMatricule(matricule);
    Eleve savedEleve = eleverepository.save(eleve);


    try {
		/* envoyerEmailConfirmation(savedEleve); */
    } catch (Exception e) {
        // Logguer l'erreur plutôt que de l'afficher
        System.err.println("Erreur lors de l'envoi du mail à : " + savedEleve.getEmail());
        e.printStackTrace(); 
    }

    return savedEleve;
}
	
	private String genererCodeEcole(String nomEcole) {
	    if (nomEcole == null || nomEcole.isBlank()) return "UNK";
	    String[] mots = nomEcole.trim().toUpperCase().split("\\s+");

	    StringBuilder code = new StringBuilder();

	    // Si le nom contient au moins deux mots
	    if (mots.length >= 3) {
	        // Garde le premier mot (ex: "CS") complet
	        code.append(mots[0]);
	        // Puis ajoute la première lettre des deux mots suivants (ex: MAMA, NSONGO)
	        code.append(mots[1].charAt(0));
	        code.append(mots[2].charAt(0));
	    } else {
	        // Si l'école a peu de mots, on prend juste les initiales
	        for (String mot : mots) {
	            code.append(mot.charAt(0));
	        }
	    }

	    return code.toString();
	}


	private String genererCodeClasse(String nomClasse) {
    if (nomClasse == null || nomClasse.isBlank()) return "UNK";

    nomClasse = nomClasse.toUpperCase();

    // Mots inutiles à ignorer dans la génération
    String[] motsInutiles = {
        "EME", "ERE", "DE", "DU", "LA", "LE"
    };

    for (String mot : motsInutiles) {
        nomClasse = nomClasse.replace(mot, "");
    }

    // Nettoyage
    nomClasse = nomClasse.replaceAll("\\s+", " ").trim();
    String[] mots = nomClasse.split(" ");
    StringBuilder code = new StringBuilder();

    // Exemple : "1 ERE MATERNELLE A" -> mots = ["1", "MATERNELLE", "A"]
    for (int i = 0; i < mots.length; i++) {
        String mot = mots[i];
        if (i == 0) {
            // Si c’est le premier mot (souvent un chiffre)
            code.append(mot);
        } else if (mot.length() >= 2 && i == 1) {
            // Si c’est le mot de la matière/niveau -> 2 premières lettres
            code.append(mot.substring(0, 2));
        } else {
            // Dernier mot (souvent une lettre comme A/B/C)
            code.append(mot.charAt(0));
        }
    }

    return code.toString();
}



/*
 * private void envoyerEmailConfirmation(Eleve eleve) { SimpleMailMessage email
 * = new SimpleMailMessage(); email.setTo(eleve.getEmail());
 * email.setSubject("Confirmation d'inscription à CS MAMA NSONGO");
 * 
 * String message = "Bonjour cher parent " + eleve.getNomtuteur() + ",\n\n" +
 * "Nous vous remercions chaleureusement de nous avoir fait confiance pour l'inscription de votre enfant à MAMA NSONGO.\n"
 * + "Votre formulaire a bien été enregistré.\n\n" + "Cordialement,\n" +
 * "L'équipe CS MAMA NSONGO";
 * 
 * email.setText(message); email.setFrom("ekabatantshi@gmail.com");
 * mailSender.send(email);
 * 
 * System.out.println("Email envoyé à : " + eleve.getEmail()); }
 */

	@Override
	public ResponseEntity<Eleve> updateEleve(Long ideleve, Eleve eleve) {
		Optional<Eleve> EleveData = eleverepository.findById(ideleve);
		if (EleveData.isPresent()) {
			Eleve eleves = EleveData.get();
			eleves.setNom(eleve.getNom());
			eleves.setPostnom(eleve.getPostnom());
			eleves.setPrenom(eleve.getPrenom());
			eleves.setSexe(eleve.getSexe());
			eleves.setIdprovince(eleve.getIdprovince());
			eleves.setDatenaiss(eleve.getDatenaiss());
			eleves.setIdecole(eleve.getIdecole());
			eleves.setTelephone(eleve.getTelephone());
			eleves.setIdeleve(eleve.getIdeleve());
			eleves.setEmail(eleve.getEmail());
			eleves.setAdresse(eleve.getAdresse());
			eleves.setIdintermedaireclasse(eleve.getIdintermedaireclasse());
			eleves.setIdintermedaireannee(eleve.getIdintermedaireannee());
			eleves.setIduser(eleve.getIduser());
		    eleves.setMatricule(eleve.getMatricule());

			return new ResponseEntity<>(eleverepository.save(eleve), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	public List<EleveModelDto> CollecteEleves() {
		String query = "SELECT b.ideleve,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom,"
				+ "UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email,"
				+ "b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe,UPPER(b.adresse) AS adresse,"
				+ "c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province,"
				+ "h.idcommune, UPPER(h.commune) AS commune, f.idannee, UPPER(f.annee) AS annee,UPPER(a.avenue) AS avenue,"
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos "
				+ " FROM tab_Eleve b" + " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse"
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse" + " JOIN tab_Ecole a ON c.idecole = a.idecole"
				+ " JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee"
				+ " JOIN tab_Annee f ON d.idannee = f.idannee" + " JOIN tab_Province g ON b.idprovince = g.idprovince"
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune"
				+ " LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ " LEFT JOIN Tab_Historique_classe_eleve hi ON hi.ideleve = b.ideleve AND hi.idclasse = e.idclasse AND hi.idannee = f.idannee "
				+ " ORDER BY b.ideleve DESC limit 1";
		return namedParameterJdbcTemplate.query(query, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> CollecteEleve() {
		List<EleveModelDto> collections = CollecteEleves();

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> CollecteElevese(long idecole) {
		String query = "SELECT UPPER(e.classe) AS classe, UPPER(f.annee) AS annee, COUNT(b.ideleve) AS ideleve "
				+ "FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse "
				+ "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee "
				+ "JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ "JOIN tab_Ecole k ON k.idecole = d.idecole "
				+ "LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = e.idclasse AND h.idannee = f.idannee "
				+ "WHERE k.idecole = :idecole  "
				+ "GROUP BY e.classe,f.annee " + "ORDER BY annee ASC";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> CollecteEleveses(long idecole) {
		List<EleveModelDto> collections = CollecteElevese(idecole);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}
	/*
	 * public List<EleveModelDto> FicheEleves(Long ideleve) { String query = """
	 * SELECT b.ideleve, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom,
	 * UPPER(b.prenom) AS prenom, UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS
	 * nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, b.telephone,
	 * a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS
	 * classe,UPPER(b.adresse) AS adresse, c.idintermedaireclasse,
	 * d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province,
	 * x.id,y.id, h.idcommune, UPPER(h.commune) AS commune, f.idannee,
	 * UPPER(f.annee) AS annee,UPPER(a.avenue) AS avenue,UPPER(z.username) AS
	 * username, COALESCE(NULLIF(x.logos, ''),
	 * 'http://localhost:8080/logos/logo.jpg') AS logos, COALESCE(NULLIF(y.photo,
	 * ''), 'http://localhost:8080/uploads/icon.jpg') AS photo FROM tab_Eleve b JOIN
	 * tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idintermedaireclasse
	 * JOIN tab_Classe e ON c.idclasse = e.idclasse JOIN tab_Ecole a ON c.idecole =
	 * a.idecole JOIN tab_Intermedaireannee d ON b.idintermedaireannee =
	 * d.idintermedaireannee JOIN tab_Annee f ON d.idannee = f.idannee JOIN tab_User
	 * z ON z.iduser = b.iduser JOIN tab_Province g ON b.idprovince = g.idprovince
	 * JOIN tab_Commune h ON h.idcommune = a.idcommune LEFT JOIN tab_Logos x ON
	 * x.idecole = a.idecole LEFT JOIN tab_Photo y ON y.ideleve = b.ideleve where
	 * b.ideleve = :ideleve ORDER BY b.ideleve limit 1 """; MapSqlParameterSource
	 * parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);
	 * 
	 * try { return namedParameterJdbcTemplate.query(query, parameters, new
	 * BeanPropertyRowMapper<>(EleveModelDto.class)); } catch (Exception e) { return
	 * Collections.emptyList(); } }
	 * 
	 * 
	 * public ResponseEntity<?> FicheEleve(Long ideleve) throws JRException { try {
	 * List<EleveModelDto> collections = FicheEleves(ideleve);
	 * 
	 * if (collections.isEmpty()) { return
	 * ResponseEntity.status(HttpStatus.NOT_FOUND)
	 * .body("Aucune fiche élève trouvée pour l'ID : " + ideleve); }
	 * 
	 * for (EleveModelDto eleve : collections) { try { // === Traitement du logo ===
	 * BufferedImage logoImage; if (eleve.getLogos() != null &&
	 * eleve.getLogos().startsWith("data:image")) { byte[] decodedLogo =
	 * Base64.getDecoder().decode(eleve.getLogos().split(",")[1]); logoImage =
	 * ImageIO.read(new ByteArrayInputStream(decodedLogo)); } else { InputStream
	 * defaultLogo = new ClassPathResource("static/logo.jpg").getInputStream();
	 * logoImage = ImageIO.read(defaultLogo); } eleve.setLogosImage(logoImage);
	 * 
	 * // === Traitement de la photo === BufferedImage photoImage; if
	 * (eleve.getPhoto() != null && eleve.getPhoto().startsWith("data:image")) {
	 * byte[] decodedPhoto =
	 * Base64.getDecoder().decode(eleve.getPhoto().split(",")[1]); photoImage =
	 * ImageIO.read(new ByteArrayInputStream(decodedPhoto)); } else { InputStream
	 * defaultPhoto = new ClassPathResource("static/icon.jpg").getInputStream();
	 * photoImage = ImageIO.read(defaultPhoto); } eleve.setPhotoImage(photoImage);
	 * 
	 * } catch (IOException e) {
	 * System.err.println("Erreur de chargement d’image : " + e.getMessage()); } }
	 * 
	 * // === Génération du rapport Jasper === JRBeanCollectionDataSource ds = new
	 * JRBeanCollectionDataSource(collections); InputStream jrxmlStream = new
	 * ClassPathResource("etats/Eleves.jrxml").getInputStream(); JasperReport
	 * jasperReport = JasperCompileManager.compileReport(jrxmlStream); JasperPrint
	 * reportPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(),
	 * ds);
	 * 
	 * String encodedPdf = Base64.getEncoder().encodeToString(
	 * JasperExportManager.exportReportToPdf(reportPrint));
	 * 
	 * return ResponseEntity.ok(new reportBase64(encodedPdf));
	 * 
	 * } catch (IOException e) { return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Fichier JRXML introuvable ou inaccessible : " + e.getMessage()); }
	 * catch (JRException e) { return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur JasperReports : " + e.getMessage()); } catch (Exception e) {
	 * return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur inattendue : " + e.getMessage()); } }
	 * 
	 * 
	 * 
	 * /* public ResponseEntity<?> FicheEleve(Long ideleve) throws JRException { try
	 * { List<EleveModelDto> collections = FicheEleves(ideleve);
	 * 
	 * if (collections.isEmpty()) { return
	 * ResponseEntity.status(HttpStatus.NOT_FOUND)
	 * .body("Aucune fiche élève trouvée pour l'ID : " + ideleve); }
	 * 
	 * for (EleveModelDto eleve : collections) { try { if (eleve.getLogos() != null
	 * && eleve.getLogos().startsWith("data:image")) { String base64 =
	 * eleve.getLogos().split(",")[1]; byte[] decoded =
	 * Base64.getDecoder().decode(base64); eleve.setLogosImage(ImageIO.read(new
	 * ByteArrayInputStream(decoded))); }
	 * 
	 * if (eleve.getPhoto() != null && eleve.getPhoto().startsWith("data:image")) {
	 * String base64 = eleve.getPhoto().split(",")[1]; byte[] decoded =
	 * Base64.getDecoder().decode(base64); eleve.setPhotoImage(ImageIO.read(new
	 * ByteArrayInputStream(decoded))); }
	 * 
	 * } catch (IOException ex) { System.out.println("Erreur de lecture image : " +
	 * ex.getMessage()); } }
	 * 
	 * JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);
	 * 
	 * InputStream jrxmlStream = new
	 * ClassPathResource("etats/Eleves.jrxml").getInputStream(); JasperReport
	 * jasperReport = JasperCompileManager.compileReport(jrxmlStream); JasperPrint
	 * reportlist = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
	 * 
	 * String encodedString = Base64.getEncoder()
	 * .encodeToString(JasperExportManager.exportReportToPdf(reportlist));
	 * 
	 * return ResponseEntity.ok(new reportBase64(encodedString));
	 * 
	 * } catch (IOException e) { return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Fichier JRXML introuvable ou inaccessible : " + e.getMessage()); }
	 * catch (JRException e) { return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur JasperReports : " + e.getMessage()); } catch (Exception e) {
	 * return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur inattendue : " + e.getMessage()); } }
	 * 
	 * 
	 * public ResponseEntity<?> FicheEleve(Long ideleve) throws JRException { try {
	 * List<EleveModelDto> collections = FicheEleves(ideleve);
	 * 
	 * if (collections.isEmpty()) { for (EleveModelDto eleve : collections) { try {
	 * byte[] imageBytesLogos = Base64.getDecoder().decode(eleve.getLogos());
	 * BufferedImage logosImage = ImageIO.read(new
	 * ByteArrayInputStream(imageBytesLogos));
	 * 
	 * byte[] imageBytesPhoto = Base64.getDecoder().decode(eleve.getPhoto());
	 * BufferedImage photoImage = ImageIO.read(new
	 * ByteArrayInputStream(imageBytesPhoto));
	 * 
	 * if (logosImage == null || photoImage == null) { throw new
	 * IOException("L'une des images n'est pas dans un format reconnu !"); }
	 * 
	 * // Convertir BufferedImage en Base64 ByteArrayOutputStream baosLogos = new
	 * ByteArrayOutputStream(); ImageIO.write(logosImage, "png", baosLogos); String
	 * base64Logos = Base64.getEncoder().encodeToString(baosLogos.toByteArray());
	 * 
	 * ByteArrayOutputStream baosPhoto = new ByteArrayOutputStream();
	 * ImageIO.write(photoImage, "png", baosPhoto); String base64Photo =
	 * Base64.getEncoder().encodeToString(baosPhoto.toByteArray());
	 * 
	 * eleve.setLogos(base64Logos); eleve.setPhoto(base64Photo);
	 * 
	 * } catch (IOException e) { System.out.println("Erreur conversion image : " +
	 * e.getMessage()); } }
	 * 
	 * }
	 * 
	 * JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);
	 * 
	 * InputStream jrxmlStream = new
	 * ClassPathResource("etats/Eleves.jrxml").getInputStream(); JasperReport
	 * jasperReport = JasperCompileManager.compileReport(jrxmlStream); JasperPrint
	 * reportlist = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
	 * 
	 * String encodedString = Base64.getEncoder()
	 * .encodeToString(JasperExportManager.exportReportToPdf(reportlist));
	 * 
	 * return ResponseEntity.ok(new reportBase64(encodedString));
	 * 
	 * } catch (IOException e) { e.printStackTrace(); return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Fichier JRXML introuvable ou inaccessible : " + e.getMessage()); }
	 * catch (JRException e) { e.printStackTrace(); return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur JasperReports : " + e.getMessage()); } catch (Exception e) {
	 * e.printStackTrace(); return
	 * ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	 * .body("Erreur inattendue : " + e.getMessage()); } }
	 */

	public List<EleveModelDto> FicheEleves(Long ideleve) {
		String query = "SELECT b.ideleve, b.matricule , UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom,"
				+ "UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email,"
				+ "b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe,UPPER(b.adresse) AS adresse,"
				+ "c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province, x.id,y.id, "
				+ "h.idcommune, UPPER(h.commune) AS commune, f.idannee, UPPER(f.annee) AS annee,UPPER(a.avenue) AS avenue,UPPER(z.username) AS username, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos, "
				+ " CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(y.photo, ''), 'icon.jpg')) AS photo "
				+ " FROM tab_Eleve b" + "	JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse"
				+ "	JOIN tab_Classe e ON c.idclasse = e.idclasse" + " JOIN tab_Ecole a ON c.idecole = a.idecole"
				+ "	JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee"
				+ "	JOIN tab_Annee f ON d.idannee = f.idannee" + "	JOIN tab_User z ON z.iduser = b.iduser"
				+ " JOIN tab_Province g ON b.idprovince = g.idprovince"
				+ " JOIN tab_Commune h ON h.idcommune = a.idcommune"
				+ " LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ " LEFT JOIN tab_Photo y ON y.ideleve = b.ideleve "
				+ " LEFT JOIN Tab_Historique_classe_eleve hi ON hi.ideleve = b.ideleve AND hi.idclasse = e.idclasse AND hi.idannee = f.idannee"
				+ " where b.ideleve = :ideleve "
				+ " ORDER BY b.ideleve limit 1";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> FicheEleve(Long ideleve) throws FileNotFoundException, JRException {
		try {
			List<EleveModelDto> collections = FicheEleves(ideleve);
			if (collections.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Aucune fiche élève trouvée pour l'ID : " + ideleve);
			}

			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);
			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager
					.compileReport(ResourceUtils.getFile("classpath:etats/Eleves.jrxml").getAbsolutePath()), null, ds);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));

			return ResponseEntity.ok(new reportBase64(encodedString));

		} catch (FileNotFoundException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Fichier JRXML introuvable : " + e.getMessage());
		} catch (JRException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Erreur JasperReports : " + e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Erreur inattendue : " + e.getMessage());
		}
	}

	public List<EleveModelDto> FicheSelonEleves(Long ideleve) {
		String query = "SELECT * FROM (" + "  SELECT b.ideleve, b.matricule, "
	             + "         UPPER(b.nom) AS nom,"
				+ "         UPPER(b.postnom) AS postnom,"
	            + "         UPPER(b.prenom) AS prenom,"
				+ "         UPPER(b.sexe) AS sexe," + "         UPPER(b.nomtuteur) AS nomtuteur,"
				+ "         b.dateins," + "         b.datenaiss," + "         UPPER(b.email) AS email,"
				+ "         b.telephone," + "         a.idecole," + "         UPPER(a.ecole) AS ecole,"
				+ "         e.idclasse," + "         UPPER(e.classe) AS classe,"
				+ "         UPPER(b.adresse) AS adresse," + "         c.idintermedaireclasse,"
				+ "         d.idintermedaireannee," + "         g.idprovince,"
				+ "         UPPER(g.province) AS province," + "         x.id AS idlogo," + "         y.id AS idphoto,"
				+ "         h.idcommune," + "         UPPER(h.commune) AS commune," + "         f.idannee,"
				+ "         UPPER(f.annee) AS annee," + "         UPPER(a.avenue) AS avenue,"
				+ "         UPPER(z.username) AS username,"
				+ "         CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos,"
				+ "         CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(y.photo, ''), 'icon.jpg')) AS photo,"
				+ "         'CLASSE ACTUELLE' AS statut_classe" + "  FROM tab_Eleve b"
				+ "  JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse"
				+ "  JOIN tab_Classe e ON c.idclasse = e.idclasse" + "  JOIN tab_Ecole a ON c.idecole = a.idecole"
				+ "  JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee"
				+ "  JOIN tab_Annee f ON d.idannee = f.idannee" + "  JOIN tab_User z ON z.iduser = b.iduser"
				+ "  JOIN tab_Province g ON b.idprovince = g.idprovince"
				+ "  JOIN tab_Commune h ON h.idcommune = a.idcommune"
				+ "  LEFT JOIN tab_Logos x ON x.idecole = a.idecole "
				+ "  LEFT JOIN tab_Photo y ON y.ideleve = b.ideleve  " + "  WHERE b.ideleve = :ideleve" + "  UNION ALL"
				+ "  SELECT b.ideleve, b.matricule , " + "         UPPER(b.nom) AS nom, " + "         UPPER(b.postnom) AS postnom, "
				+ "         UPPER(b.prenom) AS prenom," + "         UPPER(b.sexe) AS sexe,"
				+ "         UPPER(b.nomtuteur) AS nomtuteur," + "         b.dateins," + "         b.datenaiss,"
				+ "         UPPER(b.email) AS email," + "         b.telephone," + "         a.idecole,"
				+ "         UPPER(a.ecole) AS ecole," + "         h.idclasse, " + "         UPPER(cl.classe) AS classe,"
				+ "         UPPER(b.adresse) AS adresse," + "         c.idintermedaireclasse,"
				+ "         d.idintermedaireannee," + "         g.idprovince,"
				+ "         UPPER(g.province) AS province," + "         x.id AS idlogo," + "         y.id AS idphoto,"
				+ "         h_comm.idcommune," + "         UPPER(h_comm.commune) AS commune," + "         h.idannee,"
				+ "         UPPER(an.annee) AS annee," + "         UPPER(a.avenue) AS avenue,"
				+ "         UPPER(z.username) AS username,"
				+ "         CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(x.logos, ''), 'logos.png')) AS logos,"
				+ "         CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(y.photo, ''), 'icon.jpg')) AS photo,"
				+ "         'CLASSE PRECEDENTE ' AS statut_classe" + "  FROM Tab_Historique_classe_eleve h"
				+ "  JOIN Tab_Eleve b ON b.ideleve = h.ideleve" + "  JOIN tab_Classe cl ON h.idclasse = cl.idclasse"
				+ "  JOIN tab_Annee an ON h.idannee = an.idannee"
				+ "  JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse"
				+ "  JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee"
				+ "  JOIN tab_Ecole a ON c.idecole = a.idecole" + "  JOIN tab_Province g ON b.idprovince = g.idprovince"
				+ "  JOIN tab_Commune h_comm ON h_comm.idcommune = a.idcommune"
				+ "  LEFT JOIN tab_Logos x ON x.idecole = a.idecole"
				+ "  LEFT JOIN tab_Photo y ON y.ideleve = b.ideleve" + "  JOIN tab_User z ON z.iduser = b.iduser"
				+ "  WHERE h.ideleve = :ideleve" + ") AS result " + "ORDER BY idannee DESC";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> FicheSelonEleve(Long ideleve) {
		try {
			List<EleveModelDto> collections = FicheSelonEleves(ideleve);

			if (collections.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Aucune fiche élève trouvée pour l'ID : " + ideleve);
			}

			// Filtrer la fiche actuelle
			List<EleveModelDto> ficheActuelle = collections.stream()
					.filter(e -> "CLASSE ACTUELLE".equalsIgnoreCase(e.getStatut_classe())).collect(Collectors.toList());

			// Si pas de fiche actuelle, prendre la dernière fiche précédente
			List<EleveModelDto> ficheFinale;
			if (!ficheActuelle.isEmpty()) {
				ficheFinale = ficheActuelle;
			} else {
				ficheFinale = collections.stream()
						.filter(e -> "CLASSE PRECEDENTE".equalsIgnoreCase(e.getStatut_classe()))
						.sorted(Comparator.comparing(EleveModelDto::getIdannee).reversed()).limit(1)
						.collect(Collectors.toList());
			}

			return ResponseEntity.ok(ficheFinale);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Erreur lors de la récupération des fiches : " + e.getMessage());
		}
	}

	public List<EleveModelDto> searchEleves(String nom, Long idecole, boolean isAdmin) {
		String query = "SELECT DISTINCT b.ideleve ,b.matricule , UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, "
				+ "b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, "
				+ "c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province, f.idannee, "
				+ "UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue, x.id, "
				+ " CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee "
				+ "LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = e.idclasse AND h.idannee = f.idannee "
				+ "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "JOIN tab_Province g ON b.idprovince = g.idprovince " + "WHERE TRIM(LOWER(b.nom)) LIKE :nom ";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("nom",
				"" + nom.toLowerCase().trim() + "%");

		if (!isAdmin) {
			query += " AND a.idecole = :idecole";
			parameters.addValue("idecole", idecole);
		}

		query += " ORDER BY b.ideleve";

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> searchEleves(String userRole, String nom, Long idecole) {
		boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

		List<EleveModelDto> collections = searchEleves(nom, idecole, isAdmin);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé.");
		}
		return ResponseEntity.ok(collections);
	}

	public List<EleveModelDto> CollecteAnneeEleves(long idintermedaireclasse, long idintermedaireannee) {

		String query = "SELECT DISTINCT b.ideleve ,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ " UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, "
				+ " b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, "
				+ " c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province, f.idannee, "
				+ " UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue, x.id, "
				+ " CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo "
				+ " FROM tab_Eleve b " + " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ " JOIN tab_Annee f ON d.idannee = f.idannee " + " LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ " JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ " WHERE c.idclasse = :idintermedaireclasse AND d.idannee = :idintermedaireannee "
				+ " ORDER BY b.ideleve;";

		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("idintermedaireclasse", idintermedaireclasse)
				.addValue("idintermedaireannee", idintermedaireannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> CollecteAnneeEleve(long idintermedaireclasse, long idintermedaireannee) {
		List<EleveModelDto> collections = CollecteAnneeEleves(idintermedaireclasse, idintermedaireannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ce nom.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> CollecteClasseAnneeEleves(long idintermedaireclasse, long idintermedaireannee) {
		String query = " SELECT DISTINCT b.ideleve ,b.matricule , UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, c.idintermedaireclasse, "
				+ "    d.idintermedaireannee, f.idannee, UPPER(f.annee) AS annee, 'CLASSE ACTUELLE' AS statut "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee "
				+ "WHERE c.idclasse = :idintermedaireclasse AND d.idannee = :idintermedaireannee " + "UNION "
				+ "SELECT DISTINCT b.ideleve,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, c.idintermedaireclasse, "
				+ "    d.idintermedaireannee, f.idannee, UPPER(f.annee) AS annee, 'CLASSE PRECEDENTE' AS statut "
				+ "FROM tab_Eleve b " + "JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve "
				+ "JOIN tab_Classe e ON h.idclasse = e.idclasse " + "JOIN tab_Annee f ON f.idannee = h.idannee "
				+ "JOIN tab_Intermedaireclasse c ON c.idclasse = h.idclasse "
				+ "JOIN tab_Intermedaireannee d ON d.idannee = h.idannee "
				+ "WHERE h.idclasse = :idintermedaireclasse AND h.idannee = :idintermedaireannee "
				+ "ORDER BY ideleve ";
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("idintermedaireclasse", idintermedaireclasse)
				.addValue("idintermedaireannee", idintermedaireannee);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> CollecteClasseAnneeEleve(long idintermedaireclasse, long idintermedaireannee) {
		List<EleveModelDto> collections = CollecteClasseAnneeEleves(idintermedaireclasse, idintermedaireannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ce nom.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> EleveParClasses(long idecole, long idclasse, long idannee) {

		String query = "SELECT DISTINCT b.ideleve ,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ " UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, "
				+ " b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, "
				+ " c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province, f.idannee, "
				+ " UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue, x.id, "
				+ " CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo "
				+ " FROM tab_Eleve b " + " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ " JOIN tab_Annee f ON d.idannee = f.idannee " + " LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ " LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = e.idclasse AND h.idannee = f.idannee "
				+ " JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ " WHERE a.idecole = :idecole AND e.idclasse = :idclasse AND f.idannee = :idannee "
				+ " ORDER BY b.ideleve;";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> EleveParClasse(long idecole, long idclasse, long idannee) {
		List<EleveModelDto> collections = EleveParClasses(idecole, idclasse, idannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> ElevePars(long idecole, long idclasse, long idannee, long ideleve) {
		String query = " SELECT  b.ideleve, b.matricule , UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ " UPPER(b.sexe) AS sexe, UPPER(b.adresse) AS adresse, b.telephone,"
				+ " a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, "
				+ " c.idintermedaireclasse, d.idintermedaireannee, f.idannee, UPPER(f.annee) AS annee, x.id, "
				+ " COALESCE(NULLIF(x.photo, ''), 'http://localhost:8080/uploads/icon.jpg') AS photo "
				+ " FROM tab_Eleve b " + " JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ " JOIN tab_Classe e ON c.idclasse = e.idclasse " + " JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ " JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ " JOIN tab_Annee f ON d.idannee = f.idannee " + " LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ " LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = e.idclasse AND h.idannee = f.idannee "
				+ " WHERE a.idecole = :idecole AND e.idclasse = :idclasse AND f.idannee = :idannee AND b.ideleve = :ideleve "
				+ " GROUP BY b.ideleve ,b.matricule, a.idecole, e.idclasse,x.id, c.idintermedaireclasse, d.idintermedaireannee, f.idannee "
				+ " ORDER BY b.ideleve";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee).addValue("ideleve", ideleve);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> ElevePar(long idecole, long idclasse, long idannee, long ideleve) {
		List<EleveModelDto> collections = ElevePars(idecole, idclasse, idannee, ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> FicheClasses(long idecole, long idclasse, long idannee) {
		String query = "SELECT b.ideleve, b.matricule, UPPER(b.nom) || ' ' || UPPER(b.postnom) || ' ' || UPPER(b.prenom) AS noms, "
				+ "UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, "
				+ "b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, "
				+ "c.idintermedaireclasse, d.idintermedaireannee, g.idprovince, UPPER(g.province) AS province,y.id, "
				+ "h.idcommune, UPPER(h.commune) AS commune, f.idannee, UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue, UPPER(z.username) AS username, "
				+ " CONCAT('http://localhost:8080/logos/', COALESCE(NULLIF(y.logos, ''), 'logos.png')) AS logos "
				+ "FROM tab_Eleve b " + "LEFT JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "LEFT JOIN tab_Classe e ON c.idclasse = e.idclasse "
				+ "LEFT JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "LEFT JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "LEFT JOIN tab_Annee f ON d.idannee = f.idannee " + "LEFT JOIN tab_User z ON z.iduser = b.iduser "
				+ "LEFT JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ "LEFT JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ "LEFT JOIN tab_Logos y ON y.idecole = a.idecole "
				+ " LEFT JOIN Tab_Historique_classe_eleve hi ON hi.ideleve = b.ideleve AND hi.idclasse = e.idclasse AND hi.idannee = f.idannee "
				+ "WHERE a.idecole = :idecole " + "AND e.idclasse = :idclasse " + "AND f.idannee = :idannee "
				+ "ORDER BY ideleve, noms ASC";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);

		try {
			return namedParameterJdbcTemplate.query(query, parameters,
					new BeanPropertyRowMapper<>(EleveModelDto.class));
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ResponseEntity<?> FicheClasse(long idecole, long idclasse, long idannee)
			throws FileNotFoundException, JRException {
		try {

			List<EleveModelDto> collections = FicheClasses(idecole, idclasse, idannee);
			JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(collections);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("REPORT_DATA_SOURCE", ds);

			String reportPath = ResourceUtils.getFile("classpath:etats/Ficheleves.jrxml").getAbsolutePath();
			File reportFile = new File(reportPath);

			if (!reportFile.exists()) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body("Report file not found: " + reportPath);
			}

			JasperPrint reportlist = JasperFillManager.fillReport(JasperCompileManager.compileReport(reportPath),
					parameters, ds);

			String encodedString = Base64.getEncoder()
					.encodeToString(JasperExportManager.exportReportToPdf(reportlist));

			return ResponseEntity.ok(new reportBase64(encodedString));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Report file not found: " + e.getMessage());
		} catch (JRException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Jasper report error: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown error: " + e.getMessage());
		}
	}

	public List<EleveModelDto> CollecteElevedashbords(long idecole, long idclasse, long idannee) {
		String query = "SELECT UPPER(e.classe) AS classe, UPPER(f.annee) AS annee, COUNT(b.ideleve) AS ideleve "
				+ "FROM tab_Eleve b " + "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee " + "JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ "JOIN tab_Commune h ON h.idcommune = a.idcommune "
				+ " LEFT JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve AND h.idclasse = e.idclasse AND h.idannee = f.idannee "
				+ "WHERE a.idecole = :idecole AND c.idclasse = :idclasse AND d.idannee = :idannee "
				+ "GROUP BY e.classe,f.annee ";
		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee);
		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> CollecteElevedashbord(long idecole, long idclasse, long idannee) {
		List<EleveModelDto> collections = CollecteElevedashbords(idecole, idclasse, idannee);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public List<EleveModelDto> NombreEleveActuelledashbords() {
	    String query = """
	        SELECT  
	            COUNT(DISTINCT b.ideleve) AS ideleve,
	            f.annee
	        FROM tab_Eleve b
	        JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse
	        JOIN tab_Classe e ON c.idclasse = e.idclasse
	        JOIN tab_Ecole a ON c.idecole = a.idecole
	        JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee
	        JOIN tab_Annee f ON d.idannee = f.idannee
	        LEFT JOIN Tab_Historique_classe_eleve hi 
	               ON hi.ideleve = b.ideleve 
	              AND hi.idclasse = e.idclasse 
	              AND hi.idannee = f.idannee
	        WHERE f.cloturee = FALSE 
	        GROUP BY f.annee
	        ORDER BY f.annee DESC
	    """;

	    return namedParameterJdbcTemplate.query(query, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> NombreEleveActuelledashbord() {
	    List<EleveModelDto> collections = NombreEleveActuelledashbords();

	    if (collections.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body("Aucun élève trouvé pour l'année en cours.");
	    } else {
	        return ResponseEntity.ok(collections);
	    }
	}

	public List<EleveModelDto> SectionEleveActuelledashbords() {
		String query = "SELECT CASE "
				+ "    WHEN UPPER(e.classe) LIKE '%MATERNELLE%' THEN 'MATERNELLE' "
				+ "    WHEN UPPER(e.classe) LIKE '%PRIMAIRE%' THEN 'PRIMAIRE' "
				+ "    WHEN UPPER(e.classe) LIKE '%SECONDAIRE%' THEN 'SECONDAIRE' "
				+ "    WHEN UPPER(e.classe) LIKE '%MECANIQUE%' THEN 'MECANIQUE' "
				+ "    WHEN UPPER(e.classe) LIKE '%ELECTRICITE%' THEN 'ELECTRICITE' "
				+ "    WHEN UPPER(e.classe) LIKE '%BIO-CHIMIE%' THEN 'BIO-CHIMIE' "
				+ "    WHEN UPPER(e.classe) LIKE '%COUPE-COUTURE%' THEN 'COUPE-COUTURE' "
				+ "    ELSE 'AUTRES'  END AS type_classe, "
				+ "  COUNT(DISTINCT b.ideleve) AS ideleve,"
				+ "  ROUND( COUNT(DISTINCT b.ideleve) * 100.0 / "
				+ "    SUM(COUNT(DISTINCT b.ideleve)) OVER (), 2 "
				+ "  ) AS pourcentage "
				+ " FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse "
				+ "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee "
				+ "LEFT JOIN Tab_Historique_classe_eleve hi "
				+ "  ON hi.ideleve = b.ideleve "
				+ "  AND hi.idclasse = e.idclasse "
				+ "  AND hi.idannee = f.idannee "
				+ " WHERE f.cloturee = FALSE  "
				+ "GROUP BY type_classe "
				+ "ORDER BY type_classe";

		return namedParameterJdbcTemplate.query(query, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> SectionEleveActuelledashbord() {
		List<EleveModelDto> collections = SectionEleveActuelledashbords();

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ces paramètres.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

	public void logUserIscription(String username, String noms, String classe, String ecole, String annee) {
		logger.info("Iscription effectué | Élève: {} | Classe: {} | École: {} | Année: {}  | Utilisateur: {}", noms,
				classe, ecole, annee, username);
	}

	public void enregistrerIscription(String username, String noms, String classe, String ecole, String annee) {
		logUserIscription(username, noms, classe, ecole, annee);
		System.out.println("✅ Inscription enregistré dans les logs !");
	}

	@Override
	public List<EleveModelDto> ParcourEleves(long idecole, long idclasse, long idannee, long ideleve) {
		String query = " SELECT b.ideleve ,b.matricule , UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, "
				+ "    UPPER(b.sexe) AS sexe, UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, "
				+ "    b.telephone, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse, UPPER(e.classe) AS classe, UPPER(b.adresse) AS adresse, "
				+ "    g.idprovince, NULL AS date_passation, UPPER(g.province) AS province, f.idannee, UPPER(f.annee) AS annee, UPPER(a.avenue) AS avenue, "
				+ "    CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo, "
				+ "    'CLASSE ACTUELLE' AS statut_classe " + "FROM tab_Eleve b "
				+ "JOIN tab_Intermedaireclasse c ON b.idintermedaireclasse = c.idclasse "
				+ "JOIN tab_Classe e ON c.idclasse = e.idclasse " + "JOIN tab_Ecole a ON c.idecole = a.idecole "
				+ "JOIN tab_Intermedaireannee d ON b.idintermedaireannee = d.idannee "
				+ "JOIN tab_Annee f ON d.idannee = f.idannee " + "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "JOIN tab_Province g ON b.idprovince = g.idprovince "
				+ "WHERE a.idecole = :idecole AND e.idclasse = :idclasse AND f.idannee = :idannee AND b.ideleve = :ideleve "
				+ "UNION ALL "
				+ "SELECT b.ideleve ,b.matricule, UPPER(b.nom) AS nom, UPPER(b.postnom) AS postnom, UPPER(b.prenom) AS prenom, UPPER(b.sexe) AS sexe, "
				+ "    UPPER(b.nomtuteur) AS nomtuteur, b.dateins, b.datenaiss, UPPER(b.email) AS email, b.telephone, a.idecole, "
				+ "    UPPER(a.ecole) AS ecole, cl.idclasse, UPPER(cl.classe) AS classe, UPPER(b.adresse) AS adresse, g.idprovince, h.date_passation, "
				+ "    UPPER(g.province) AS province, an.idannee, UPPER(an.annee) AS annee, UPPER(a.avenue) AS avenue, "
				+ "    CONCAT('http://localhost:8080/uploads/', COALESCE(NULLIF(x.photo, ''), 'icon.jpg')) AS photo, "
				+ "    'CLASSE PRECEDENTE' AS statut_classe " + "FROM tab_Eleve b "
				+ "JOIN Tab_Historique_classe_eleve h ON h.ideleve = b.ideleve "
				+ "JOIN tab_Classe cl ON h.idclasse = cl.idclasse " + "JOIN tab_Annee an ON h.idannee = an.idannee "
				+ "JOIN tab_Intermedaireannee d ON d.idannee = an.idannee "
				+ "JOIN tab_Ecole a ON d.idecole = a.idecole " + "LEFT JOIN tab_Photo x ON x.ideleve = b.ideleve "
				+ "JOIN tab_Province g ON b.idprovince = g.idprovince " + "WHERE b.ideleve = :ideleve";

		MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole)
				.addValue("idclasse", idclasse).addValue("idannee", idannee).addValue("ideleve", ideleve);

		return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(EleveModelDto.class));
	}

	public ResponseEntity<?> ParcourEleve(long idecole, long idclasse, long idannee, long ideleve) {
		List<EleveModelDto> collections = ParcourEleves(idecole, idclasse, idannee, ideleve);

		if (collections.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucun élève trouvé pour ce nom.");
		} else {
			return ResponseEntity.ok(collections);
		}
	}

}
