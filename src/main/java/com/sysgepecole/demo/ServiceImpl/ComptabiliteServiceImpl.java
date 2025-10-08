package com.sysgepecole.demo.ServiceImpl;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sysgepecole.demo.Dto.ComptabiliteDto;
import com.sysgepecole.demo.Models.Comptabilite;
import com.sysgepecole.demo.Models.Users;
import com.sysgepecole.demo.Repository.ComptabiliteRepository;
import com.sysgepecole.demo.Repository.UsersRepository;
import com.sysgepecole.demo.Service.ComptabiliteService;



@Service
public class ComptabiliteServiceImpl implements ComptabiliteService{
	
	
	
	@Autowired
	private ComptabiliteRepository comptabilitepository;
	
	@Autowired
    private UsersRepository usersRepository;
	
	@Autowired 
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	
	
	
	@Override
	public Comptabilite createComptabilite(Comptabilite comptabilite) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    String username = authentication.getName();
	    Users user = usersRepository.findByUsername(username)
	        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));

	    LocalDate now = LocalDate.now();
	    int count = comptabilitepository.countByUserAndMonth(user.getIduser(), now.getMonthValue(), now.getYear());

	    if (count >= 5) {
	        throw new IllegalStateException("Limite mensuelle atteinte : maximum 5 opérations par mois.");
	    }
	    
	    Optional<Comptabilite>existing = comptabilitepository.findByIdannee(comptabilite.getIdannee());
	    
		if (existing.isPresent()) {
			comptabilite = existing.get();
			double monatantActuel = comptabilite.getMontant();
			
			    comptabilite.setIduser(user.getIduser());  
			    comptabilite.setEtape("0");    
			    comptabilite.setIdannee(comptabilite.getIdannee());  
			    comptabilite.setMontant(monatantActuel + comptabilite.getMontant());
			    comptabilite.setLibelle(comptabilite.getLibelle());
			    comptabilite.setTypeoperation("DECAISSEMENT"); 
			    comptabilite.setDateOperation(now);
			    comptabilite.setAnnule(false);
			
		} else {
			comptabilite = new Comptabilite();
			comptabilite.setLibelle(comptabilite.getLibelle());
		    comptabilite.setTypeoperation("DECAISSEMENT"); 
		    comptabilite.setDateOperation(now);
		    comptabilite.setAnnule(false);
			comptabilite.setMontant(comptabilite.getMontant());
			comptabilite.setIduser(user.getIduser());  
		    comptabilite.setEtape("0");    
		    comptabilite.setIdannee(comptabilite.getIdannee()); 
			
		}
		
	    return comptabilitepository.save(comptabilite);
	}
	
	
	@Override
	public ResponseEntity<Comptabilite> updateComptabilite(Long id, Comptabilite comptabilite) {
	    Optional<Comptabilite> comptabiliteData = comptabilitepository.findById(id);
	    if (!comptabiliteData.isPresent()) {
	        throw new IllegalStateException("Opération comptable introuvable.");
	    }

	    Comptabilite existing = comptabiliteData.get();

	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    String username = authentication.getName();
	    Users user = usersRepository.findByUsername(username)
	        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));

	    LocalDate now = LocalDate.now();
	    int count = comptabilitepository.countByUserAndMonth(user.getIduser(), now.getMonthValue(), now.getYear());

	    if (count >= 5) {
	        throw new IllegalStateException("Limite mensuelle atteinte : maximum 5 opérations par mois.");
	    }

	    // Mise à jour des champs
	    existing.setIdusermodi(user.getIduser());
	    existing.setEtape("0");
	    existing.setIdannee(comptabilite.getIdannee());
	    existing.setMontant(comptabilite.getMontant());
	    existing.setLibelle(comptabilite.getLibelle());
	    existing.setTypeoperation("DECAISSEMENT");
	    existing.setDateOperationmodi(now);
	    comptabilite.setAnnule(false);
	   	    

	    return new ResponseEntity<>(comptabilitepository.save(existing), HttpStatus.OK);
	}


	

	    @Override
	    public ResponseEntity<?> annulerOperation(Long id) {
	        Comptabilite comptabilite = comptabilitepository.findById(id)
	                        .orElseThrow(() -> new RuntimeException("Operation non trouvé"));
	        
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		    String username = authentication.getName();
		    Users user = usersRepository.findByUsername(username)
		        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));
		    
		    
		    comptabilite.setIduser(user.getIduser());
	        comptabilite.setAnnule(true);
	        comptabilite.setDateOperationmodi(LocalDate.now());
	        comptabilitepository.save(comptabilite);
			return ResponseEntity.ok("Operation annulé avec succès.");
	    }
	    
	    
	    public List<ComptabiliteDto> ComptableIds(Long iduser, Date dateDebut, Date dateFin) {
	    	String query = "SELECT  (b.id), UPPER(u.nom) AS nom, UPPER(u.postnom) AS postnom, "
	    	        + "UPPER(u.prenom) AS prenom, f.idannee, UPPER(f.annee) AS annee, b.annule, b.typeoperation, b.etape,  "
	    	        + "b.montant AS montant, b.etape, b.date_operation, UPPER(b.libelle) AS libelle, "
	    	        + "u.username, u.iduser "
	    	        + "FROM tab_Comptables b "
	    	        + "JOIN tab_Annee f ON b.idannee = f.idannee "
	    	        + "JOIN tab_User u ON b.iduser = u.iduser "
	    	        + "JOIN tab_User_roles ur ON ur.users_iduser = u.iduser "
	    	        + "JOIN tab_Role r ON r.idrole = ur.roles_idrole "
	    	        + "WHERE u.iduser = :iduser AND b.annule = false "
	    	        + "AND DATE(b.date_operation) BETWEEN :dateDebut AND :dateFin "
	    	        + "ORDER BY b.id DESC";


			MapSqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("iduser", iduser)
					.addValue("dateDebut", dateDebut)
					.addValue("dateFin", dateFin);
			return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(ComptabiliteDto.class));
		}

		public ResponseEntity<?> ComptableId(Long iduser, Date dateDebut, Date dateFin) {
			List<ComptabiliteDto> collections = ComptableIds(iduser, dateDebut, dateFin);

			if (collections.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune operation comptable trouvé .");
			} else {
				return ResponseEntity.ok(collections);
			}

		}
		
		@Override
		public String updateEtape(Long id, String etape) {
		    // Récupération de la comptabilité ou exception si non trouvée
		    Comptabilite comptabilite = comptabilitepository.findById(id)
		            .orElseThrow(() -> new RuntimeException("Id introuvable."));

		    // Vérification si l'étape est déjà validée
		    if ("1".equals(comptabilite.getEtape())) {
		        throw new IllegalStateException("Cette opération est déjà validée et ne peut plus être modifiée.");
		    }

		    
		    comptabilite.setEtape(etape); 
		    comptabilitepository.save(comptabilite); 

		    return "Étape changée avec succès.";
		}



}
