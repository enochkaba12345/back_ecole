package com.sysgepecole.demo.ServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;


import com.sysgepecole.demo.Dto.ClasseModelDto;
import com.sysgepecole.demo.Dto.ClasseTransitionDto;
import com.sysgepecole.demo.Dto.PaiementDto;
import com.sysgepecole.demo.Models.Classe;
import com.sysgepecole.demo.Repository.ClasseRepository;
import com.sysgepecole.demo.Service.ClasseService;
import com.sysgepecole.demo.Service.ClasseUtils;

@Service
public class ClasseServiceImpl implements ClasseService{
	
	
	
	@Autowired 
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	

	@Autowired
	private ClasseRepository classerepository;
	
	
	@Override
	public Optional<Classe> findClasseByClasse(String classe) {
		return classerepository.findByClasse(classe); 
	}

	@Override
	public Classe createClasses(Classe classe) {
		Optional<Classe> existingEntity = findClasseByClasse(classe.getClasse());
		if (existingEntity.isPresent()) {
		
			} else {
			
				classe.setOrdre(ClasseUtils.extraireOrdre(classe));
		        classe.setCycle(ClasseUtils.extraireCycle(classe));
				
				return classerepository.save(classe); 
				}
		return classe;
	}
	
	
	public List<ClasseModelDto> CollecteClasses(Long idniveau, Long idecole) {
	    StringBuilder query = new StringBuilder(
	        "SELECT a.idecole, UPPER(a.ecole) AS ecole, cl.idclasse, UPPER(cl.classe) AS classe, " +
	        "ic.idintermedaireclasse " +
	        "FROM tab_Ecole a " +
	        "JOIN tab_Intermedaireclasse ic ON ic.idecole = a.idecole " +
	        "JOIN tab_Classe cl ON cl.idclasse = ic.idclasse " +
	        "JOIN tab_niveau nv ON nv.idniveau = cl.idniveau " +
	        "WHERE nv.idniveau = :idniveau "
	    );

	    MapSqlParameterSource parameters = new MapSqlParameterSource()
	            .addValue("idniveau", idniveau);

	    if (idecole != null) {
	        query.append("AND a.idecole = :idecole ");
	        parameters.addValue("idecole", idecole);
	    }

	    query.append("ORDER BY cl.idclasse DESC");

	    return namedParameterJdbcTemplate.query(query.toString(), parameters, new BeanPropertyRowMapper<>(ClasseModelDto.class));
	}

	
	public ResponseEntity<?> CollecteClasse(long idniveau,Long idecole) {
		  List<ClasseModelDto> collections = CollecteClasses(idniveau,idecole);

		    if (collections.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune niveau trouvée.");
		    } else {
		        return ResponseEntity.ok(collections);
		    }
	}
	
	
	
	public List<ClasseModelDto> CollecteEcoles(Long idecole) {
	    String query = "SELECT a.idecole, UPPER(a.ecole) AS ecole, e.idclasse,"
	    		+ "  UPPER(e.classe) AS classe, c.idintermedaireclasse "
	    		+ "  FROM tab_Ecole a "
	    		+ " JOIN tab_Intermedaireclasse c ON c.idecole = a.idecole "
	    		+ " JOIN tab_Classe e ON e.idclasse = c.idclasse "
	    		+ " WHERE a.idecole = :idecole "
	    		+ "ORDER BY e.idclasse DESC";
	 	     MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("idecole", idecole);
		    return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(ClasseModelDto.class));
	}
	
	public ResponseEntity<?> CollecteEcole(long idecole) {
		  List<ClasseModelDto> collections = CollecteEcoles(idecole);

		    if (collections.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune niveau trouvée.");
		    } else {
		        return ResponseEntity.ok(collections);
		    }
	}
	
	public List<ClasseModelDto> CollecteEcoleAnnees(long idecole,long idannee) {
	    String query = "SELECT a.idecole, UPPER(a.ecole) AS ecole, e.idclasse,"
	    		+ "  UPPER(e.classe) AS classe, c.idintermedaireclasse "
	    		+ " FROM tab_Ecole a "
	    		+ " JOIN tab_Intermedaireannee h ON h.idecole = a.idecole "
	    		+ " JOIN tab_Annee f ON f.idannee = h.idannee "
	    		+ " JOIN tab_Intermedaireclasse c ON c.idecole = a.idecole "
	    		+ " JOIN tab_Classe e ON e.idclasse = c.idclasse "
	    		+ " WHERE a.idecole = :idecole AND f.idannee = :idannee "
	    		+ " ORDER BY e.idclasse DESC ";
	 	     MapSqlParameterSource parameters = new MapSqlParameterSource()
	 	    		 .addValue("idecole", idecole)
	 	    		.addValue("idannee", idannee);
		    return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(ClasseModelDto.class));
	}
	
	public ResponseEntity<?> CollecteEcoleAnnee(long idecole,long idannee) {
		  List<ClasseModelDto> collections = CollecteEcoleAnnees(idecole,idannee);

		    if (collections.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune niveau trouvée.");
		    } else {
		        return ResponseEntity.ok(collections);
		    }
	}
	
	@Override
	public ResponseEntity<Classe> updateClasses(Long idclasse, Classe classe) {
		Optional<Classe> ClasseData = classerepository.findById(idclasse);

		if (ClasseData.isPresent()) {
			Classe classes = ClasseData.get();
			classes.setIdclasse(classe.getIdclasse());
			classes.setClasse(classe.getClasse());
			classes.setIdniveau(classe.getIdniveau());
			classe.setOrdre(ClasseUtils.extraireOrdre(classe));
	        classe.setCycle(ClasseUtils.extraireCycle(classe));
			classes.setIduser(classe.getIduser());
			return new ResponseEntity<>(classerepository.save(classe), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<Map<String, Boolean>> delete(Long idclasse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Classe> getAllClasse() {
		return classerepository.findAll();
	}

	public List<PaiementDto> CollecteCategorieClasses(long idtranche,long idcategorie,long idecole) {
		String query = "SELECT b.idtranche, UPPER(b.tranche) AS tranche, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse,"
				+ " UPPER(e.classe) AS classe, c.idintermedaireclasse, f.idfrais, f.montant, "
				+ " g.idcategorie, UPPER(g.categorie) AS categorie, d.idintermedaireannee, h.idannee, UPPER(h.annee) AS annee "
				+ " FROM tab_Ecole a "
				+ " JOIN tab_Intermedaireclasse c ON c.idecole = a.idecole "
				+ " JOIN tab_Classe e ON e.idclasse = c.idclasse "
				+ " JOIN tab_Frais f ON f.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Intermedaireannee d ON d.idintermedaireannee = f.idintermedaireannee "
				+ " JOIN tab_Tranche b ON b.idtranche = f.idtranche "
				+ " JOIN tab_Annee h ON h.idannee = d.idannee "
				+ " JOIN tab_Categoriefrais g ON g.idcategorie = f.idcategorie"
				+ " WHERE   b.idtranche= :idtranche and g.idcategorie= :idcategorie and a.idecole= :idecole"
				+ " GROUP BY e.idclasse, b.idtranche, c.idintermedaireclasse, a.idecole, g.idcategorie, f.idfrais, h.idannee,d.idintermedaireannee "
				+ " ORDER BY g.idcategorie ASC";
	    MapSqlParameterSource parameters = new MapSqlParameterSource()
	    		.addValue("idtranche", idtranche)
	    		.addValue("idcategorie", idcategorie)
	    		.addValue("idecole", idecole);
	    return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}
	
	public ResponseEntity<?> CollecteCategorieClasse(long idtranche,long idcategorie,long idecole) {
		  List<PaiementDto> collections = CollecteCategorieClasses(idtranche,idcategorie,idecole);

		    if (collections.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune Categorie trouvée.");
		    } else {
		        return ResponseEntity.ok(collections);
		    }
	}
	
	
	public List<PaiementDto> CollecteCategorieClasseAdmins(long idtranche,long idcategorie) {
		String query = "SELECT b.idtranche, UPPER(b.tranche) AS tranche, a.idecole, UPPER(a.ecole) AS ecole, e.idclasse,"
				+ " UPPER(e.classe) AS classe, c.idintermedaireclasse, f.idfrais, f.montant, "
				+ " g.idcategorie, UPPER(g.categorie) AS categorie, d.idintermedaireannee, h.idannee, UPPER(h.annee) AS annee "
				+ " FROM tab_Ecole a "
				+ " JOIN tab_Intermedaireclasse c ON c.idecole = a.idecole "
				+ " JOIN tab_Classe e ON e.idclasse = c.idclasse "
				+ " JOIN tab_Frais f ON f.idintermedaireclasse = c.idintermedaireclasse "
				+ " JOIN tab_Intermedaireannee d ON d.idintermedaireannee = f.idintermedaireannee "
				+ " JOIN tab_Tranche b ON b.idtranche = f.idtranche "
				+ " JOIN tab_Annee h ON h.idannee = d.idannee "
				+ " JOIN tab_Categoriefrais g ON g.idcategorie = f.idcategorie"
				+ " WHERE   b.idtranche= :idtranche and g.idcategorie= :idcategorie "
				+ " GROUP BY e.idclasse, b.idtranche, c.idintermedaireclasse, a.idecole, g.idcategorie, f.idfrais, h.idannee,d.idintermedaireannee "
				+ " ORDER BY g.idcategorie ASC";
	    MapSqlParameterSource parameters = new MapSqlParameterSource()
	    		.addValue("idtranche", idtranche)
	    		.addValue("idcategorie", idcategorie);
	    return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(PaiementDto.class));
	}
	
	public ResponseEntity<?> CollecteCategorieClasseAdmin(long idtranche,long idcategorie) {
		  List<PaiementDto> collections = CollecteCategorieClasseAdmins(idtranche,idcategorie);

		    if (collections.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune Categorie trouvée.");
		    } else {
		        return ResponseEntity.ok(collections);
		    }
	}
	
	@Override
    public List<ClasseModelDto> FindClasseById(long idclasse) {
        String query = "SELECT a.idecole, UPPER(a.ecole) AS ecole, e.idclasse,"
                + " UPPER(e.classe) AS classe, c.idintermedaireclasse,UPPER(e.cycle) AS cycle, e.ordre"
                + " FROM tab_Ecole a "
                + " JOIN tab_Intermedaireclasse c ON c.idecole = a.idecole "
                + " JOIN tab_Classe e ON e.idclasse = c.idclasse "
                + " WHERE e.idclasse = :idclasse "
                + " ORDER BY e.idclasse DESC ";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("idclasse", idclasse);

        return namedParameterJdbcTemplate.query(query, parameters, new BeanPropertyRowMapper<>(ClasseModelDto.class));
    }


	@Override
    public ResponseEntity<?> getClasseActuelleEtMontante(long idclasse) {
        List<ClasseModelDto> collections = FindClasseById(idclasse);

        if (collections.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classe non trouvée.");
        }

        ClasseModelDto classeActuelleDto = collections.get(0);

        // Mapper vers entité pour calcul de la montante
        Classe classeActuelle = new Classe();
        classeActuelle.setIdclasse(classeActuelleDto.getIdclasse());
        classeActuelle.setClasse(classeActuelleDto.getClasse());
        classeActuelle.setCycle(classeActuelleDto.getCycle());
        classeActuelle.setOrdre(classeActuelleDto.getOrdre());

        // Récupérer la classe montante
        Classe classeMontanteEntity = getClasseMontante(classeActuelle);

        ClasseModelDto classeMontanteDto = null;
        if (classeMontanteEntity != null) {
            classeMontanteDto = new ClasseModelDto();
            classeMontanteDto.setIdclasse(classeMontanteEntity.getIdclasse());
            classeMontanteDto.setClasse(classeMontanteEntity.getClasse());
            classeMontanteDto.setCycle(classeMontanteEntity.getCycle());
            classeMontanteDto.setOrdre(classeMontanteEntity.getOrdre());
            // ajoute les autres champs nécessaires
        }

        // Affectation dans ton DTO final
        ClasseTransitionDto dto = new ClasseTransitionDto();
        dto.setClasseActuelle(classeActuelleDto);
        dto.setClasseMontante(classeMontanteDto);

        return ResponseEntity.ok(dto);

    }
	
	public Classe getClasseMontante(Classe classeActuelle) {
	    long ordreActuel = classeActuelle.getOrdre();
	    String cycleActuel = classeActuelle.getCycle();
	    int dernierOrdre = getDernierOrdreDuCycle(cycleActuel);

	    Classe classeMontante = null;

	    if (ordreActuel < dernierOrdre) {
	        // Même cycle
	        List<Classe> classes = classerepository.findClasseMontanteList(ordreActuel + 1, cycleActuel);

	        if (!classes.isEmpty()) {
	            // Extraire suffixe (ex: A, B)
	            String[] parts = classeActuelle.getClasse().split(" ");
	            String suffixeActuel = parts[parts.length - 1];

	            for (Classe c : classes) {
	                String[] partsC = c.getClasse().split(" ");
	                String suffixeC = partsC[partsC.length - 1];

	                if (suffixeC.equalsIgnoreCase(suffixeActuel)) {
	                    classeMontante = c;
	                    break;
	                }
	            }

	            // fallback si rien trouvé
	            if (classeMontante == null) {
	                classeMontante = classes.get(0);
	            }

	            System.out.println("Classe montante retenue = " 
	                + classeMontante.getClasse() 
	                + " | id=" + classeMontante.getIdclasse());
	        }
	    } else {
	        // Cycle suivant
	        String prochainCycle = getCycleSuivant(cycleActuel);
	        if (prochainCycle != null) {
	            List<Classe> classes = classerepository.findClasseMontanteList(1, prochainCycle);

	            if (!classes.isEmpty()) {
	                classeMontante = classes.get(0);
	                System.out.println("Classe montante cycle suivant = " 
	                    + classeMontante.getClasse() 
	                    + " | id=" + classeMontante.getIdclasse());
	            }
	        }
	    }

	    return classeMontante;
	}



	private static final Map<String, String> cycleSuivantMap = Map.of(
		    "MATERNELLE", "PRIMAIRE",
		    "PRIMAIRE", "SECONDAIRE",
		    "SECONDAIRE", "ELECTRICITE",
		    "ELECTRICITE", "BIO-CHIMIE",
		    "BIO-CHIMIE", "MECANIQUE-GENERALE",
		    "MECANIQUE-GENERALE", "COMMERCE-ADMIN",
		    "COMMERCE-ADMIN", "COMMERCE-INFORMATIQUE",
		    "COMMERCE-INFORMATIQUE", "COUPE-COUTURE",
		    "COUPE-COUTURE", "CONSTRUCTION",
		    "CONSTRUCTION", "PECHE"
		);

		private String getCycleSuivant(String cycleActuel) {
		    return cycleSuivantMap.getOrDefault(cycleActuel, null);
		}


	 private int getDernierOrdreDuCycle(String cycle) {
	        switch (cycle) {
	            case "MATERNELLE": return 3;
	            case "PRIMAIRE": return 6;
	            case "SECONDAIRE": return 8;
	            case "ELECTRICITE":
	            case "BIO-CHIMIE":
	            case "MECANIQUE-GENERALE":
	            case "COMMERCE-ADMIN":
	            case "COMMERCE-INFORMATIQUE":
	            case "COUPE-COUTURE":
	            case "CONSTRUCTION":
	            case "PECHE":
	                return 4;
	            default: return Integer.MAX_VALUE;
	        }
	    }






	

}
