package com.sysgepecole.demo.ServiceImpl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mapstruct.ap.internal.conversion.GetDateTimeFormatterField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sysgepecole.demo.Models.Annee;
import com.sysgepecole.demo.Models.Classe;
import com.sysgepecole.demo.Models.Eleve;
import com.sysgepecole.demo.Models.HistoriqueClasseEleve;
import com.sysgepecole.demo.Models.Users;
import com.sysgepecole.demo.Repository.AnneeRepository;
import com.sysgepecole.demo.Repository.ClasseRepository;
import com.sysgepecole.demo.Repository.EleveRepository;
import com.sysgepecole.demo.Repository.HistoriqueClasseEleveRepository;
import com.sysgepecole.demo.Repository.PaiementRepository;
import com.sysgepecole.demo.Repository.UsersRepository;
import com.sysgepecole.demo.Service.HistoriqueClasseEleveService;

@Service
public class HistoriqueClasseEleveServiceImpl implements HistoriqueClasseEleveService{
	
	    @Autowired
	    private EleveRepository eleveRepository;

	    @Autowired
	    private ClasseRepository classeRepository;
	    
	    @Autowired
	    private AnneeRepository anneeRepository;
	    
	    @Autowired
	    private UsersRepository usersRepository;

	    @Autowired
	    private HistoriqueClasseEleveRepository historiqueClasseEleveRepository;

	    @Autowired
	    private PaiementRepository paiementRepository;
	    	    
	    @Override
		public Optional<HistoriqueClasseEleve> findHistoriqueClasseEleve(Long ideleve,Long idclasse, Long idannee) {
			return historiqueClasseEleveRepository.findByIdeleveAndIdclasseAndIdannee(ideleve,idclasse, idannee);
		}
	    
	    @Override
	    public Classe getClasseById(Long idClasse) {
	        return classeRepository.findById(idClasse)
	                .orElseThrow(() -> new RuntimeException("Classe non trouvée avec l'ID : " + idClasse));
	    }
	    
		
	    @Override
		public Eleve passerClasse(Eleve eleve) {		    
		    Long idClasse = eleve.getIdintermedaireclasse();
		    Classe classeActuelle = classeRepository.findById(idClasse).orElse(null);
	
		    Long idAnnee = eleve.getIdintermedaireannee();
		    Annee anneeActuelle = anneeRepository.findById(idAnnee).orElse(null);
		    
		    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		    String username = authentication.getName();
		    Users user = usersRepository.findByUsername(username)
		        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));


		    // 1. Sauvegarder l'historique
		    HistoriqueClasseEleve historique = new HistoriqueClasseEleve();
		    historique.setIdeleve(eleve.getIdeleve());
		    historique.setIdclasse(eleve.getIdintermedaireclasse());
		    historique.setIdannee(eleve.getIdintermedaireannee());
		    historique.setIduser(user.getIduser()); // ← utilisateur connecté
		    historique.setDatePassation(eleve.getDateins());
		    
		    historiqueClasseEleveRepository.save(historique);

		    // 2. Trouver la classe suivante
		    Classe classeSuivante = classeRepository.findByOrder(classeActuelle.getOrdre());
		    if (classeSuivante == null) {
		        throw new IllegalStateException("Aucune classe suivante trouvée pour : " + classeActuelle.getClasse());
		    }

		    // 3. Trouver l’année suivante
		    Annee anneeSuivante = anneeRepository.findByOrder(anneeActuelle.getDebut());
		    if (anneeSuivante == null) {
		        throw new IllegalStateException("Aucune année suivante trouvée pour : " + anneeActuelle.getAnnee());
		    }

		    // 4. Mettre à jour l’élève
		    eleve.setIdintermedaireclasse(classeSuivante.getIdclasse());
		    eleve.setIdintermedaireannee(anneeSuivante.getIdannee());
		    eleve.setDateins(new Date());


		    return eleveRepository.save(eleve);
		}



		
		@Override
		public void HistoriqueClasseParClasse(Long idintermedaireclasse, Long idclasse, Long idannee) {
		    // Récupérer la nouvelle classe
		    Classe nouvelleClasse = classeRepository.findById(idclasse)
		            .orElseThrow(() -> new RuntimeException("Classe non trouvée avec l'ID : " + idclasse));

		    // Récupérer les élèves de la classe intermédiaire
		    List<Eleve> eleves = eleveRepository.findByIdintermedaireclasse(idintermedaireclasse);
		    if (eleves.isEmpty()) {
		        throw new IllegalStateException("Aucun élève trouvé dans la classe intermédiaire : " + idintermedaireclasse);
		    }

		    // Faire passer chaque élève
		    for (Eleve eleve : eleves) {
		        passerClasse(eleve, nouvelleClasse, idannee);
		    }
		}



		@Override
		public Eleve passerClasse(Eleve eleve, Classe nouvelleClasse, Long idannee) {
		    if (eleve.getIdintermedaireclasse() == null || eleve.getIdintermedaireannee() == null) {
		        throw new IllegalStateException("L'élève n'a pas de classe ou d'année actuelle définie.");
		    }
		    
		    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		    String username = authentication.getName();
		    Users user = usersRepository.findByUsername(username)
		        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));

		    HistoriqueClasseEleve historique = new HistoriqueClasseEleve();
		    historique.setIdeleve(eleve.getIdeleve());
		    historique.setIdclasse(eleve.getIdintermedaireclasse());
		    historique.setIdannee(eleve.getIdintermedaireannee());
		    historique.setIduser(user.getIduser()); // ← utilisateur connecté
		    historique.setDatePassation(eleve.getDateins());
		    historiqueClasseEleveRepository.save(historique);

		    Long idAnnee = eleve.getIdintermedaireannee();
		    Annee anneeActuelle = anneeRepository.findById(idAnnee).orElse(null);
		    
		    Annee anneeSuivante = anneeRepository.findByOrder(anneeActuelle.getDebut());
		    if (anneeSuivante == null) {
		        throw new IllegalStateException("Aucune année suivante trouvée pour : " + anneeActuelle.getAnnee());
		    }

		    if (nouvelleClasse == null || nouvelleClasse.getIdclasse() == null) {
		        throw new IllegalStateException("Classe cible invalide.");
		    }

		    eleve.setIdintermedaireclasse(nouvelleClasse.getIdclasse());
		    eleve.setIdintermedaireannee(anneeSuivante.getIdannee());
		    eleve.setDateins(new Date());

		    return eleveRepository.save(eleve);
		}
		
		
}
